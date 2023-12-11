
@file:Suppress("WildcardImport", "MaxLineLength", "ReturnCount", "LongParameterList", "UnusedPrivateProperty", "UnusedParameter", "FunctionOnlyReturningConstant")
package es.unizar.urlshortener.infrastructure.delivery

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.bind.annotation.*
import ua_parser.Parser
import java.io.File
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI

//This site or product includes IP2Location LITE data available from
// <a href="https://lite.ip2location.com">https://lite.ip2location.com</a>.


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * @param data The data for creating a short URL.
     * @param request The HttpServletRequest.
     * @param limit The limit parameter (optional, with a default value of "default_limit").
     * @return ResponseEntity containing ShortUrlDataOut.
     */
    fun shortener(
        data: ShortUrlDataIn,
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") limit: String,
        @RequestParam(required = false, defaultValue = "false") hayQr: String,
    ): ResponseEntity<ShortUrlDataReapose>

    /**
     * Returns the information of a short url identified by its [id].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun returnInfo(id: String): List<Info>

    fun returnSystemInfo(@PathVariable id: String): SystemInfo
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null
)

sealed interface ShortUrlDataReapose

/**
Data returned after the creation of a short url.*/
data class ShortUrlDataOut(
    val url: URI? = null,
    val qr: String? = null,
    val properties: Map<String, Any> = emptyMap()
) :  ShortUrlDataReapose

/**
 * Error returned after the intention of creation of a short url.
 */
data class Error(
    val statusCode: Int,
    val message: String
) : ShortUrlDataReapose

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
@EnableScheduling
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val returnInfoUseCase: ReturnInfoUseCase,
    val returnSystemInfoUseCase: ReturnSystemInfoUseCase,
    //val metricsEndpoint: MetricsEndpoint
    val redirectLimitUseCase: RedirectLimitUseCase,
    var isUrlReachableUseCase: IsUrlReachableUseCase,
    val qrUseCase: QrUseCase,                        //añadimos el nuevo UseCase del Qr
    val msgUseCase: MsgUseCase

) : UrlShortenerController {

    @Value("classpath:GeoLite2-City.mmdb")
    // A File object pointing to your GeoIP2 or GeoLite2 database
    private lateinit var database: Resource

    private val reader: DatabaseReader by lazy {
        DatabaseReader.Builder(database.file).build()
    }

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown User-Agent"
        val propiedades = obtenerInformacionUsuario(userAgent, request)

        if (!redirectLimitUseCase.newRedirect(id)) return ResponseEntity(HttpStatus.TOO_MANY_REQUESTS)

        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, propiedades)
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            return ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }
    }

    // curl -v -d "url=http://www.unizar.es/&limit=3" http://localhost:8080/api/link para especificar el límite

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun shortener(
        data: ShortUrlDataIn,
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") limit: String,
        @RequestParam(required = false, defaultValue = "false") hayQr: String,

        ): ResponseEntity<ShortUrlDataReapose> {

        var limiteLong: Long = limiteALong(limit)

        if (limiteLong < 0) {
            return ResponseEntity(Error(HttpStatus.BAD_REQUEST.value(), "El límite debe ser mayor o igual que 0"), HttpStatus.BAD_REQUEST)
        }

        val result = createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                limit = limiteLong
            )
        )

        //msgUseCase.sendMsg("cola_1", "Se ha creado una nueva URL corta con hash: ${result.hash}")

        redirectLimitUseCase.addNewRedirect(result.hash, limiteLong)

        val h = HttpHeaders()
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(result.hash, request) }.toUri()
        h.location = url

        val qrUri = linkTo<UrlShortenerControllerImpl> { redirectTo(result.hash, request) }
            .slash("qr")
            .toUri()

        val response = ShortUrlDataOut(
            url = url,
            // Si hayQr == "on" se genera el código QR, si es false, qr = null
            qr = if (hayQr == "on") qrUri.toString() else null,
            properties = mapOf(
                "safe" to result.properties.safe
            )
        )

        // A partir de aqui es el QR.
        //Comprobamos el valor de hayQr en la base de datos.
        println("Valor del hayQr antes: ${qrUseCase.getCodeStatus(result.hash)}")

        if (hayQr == "on") {
            //qrUseCase.generateQRCode(url.toString(), result.hash)

            //Enviamos mensaje por la cola 1 para que se genere el QR enviando como string el hash un espacio y la url
            msgUseCase.sendMsg("cola_1", "${result.hash} ${url.toString()}")
        }

        //Comprobamos el valor de hayQr en la base de datos.
        println("Valor del hayQr despues: ${qrUseCase.getCodeStatus(result.hash)}")

        // ALCANZABILIDAD ------------------------------------------------
        // indicamos en la db que todavia lo estamos calculando
        isUrlReachableUseCase.setCodeStatus(result.hash, 2)
        println("calculando alcanzabilidad...")
        println("Valor del alcanzable: ${isUrlReachableUseCase.getCodeStatus(result.hash)}")
        if (!isUrlReachableUseCase.isUrlReachable(data.url)) {
            println("La URL no es alcanzable")
            // indicamos en la db que no es alcanzable
            isUrlReachableUseCase.setCodeStatus(result.hash, 0)
            // construimos el Error return
            val response1 = Error(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "URI de destino no alcanzable"
            )
            return ResponseEntity(response1,HttpStatus.BAD_REQUEST)
        }
        else {
            println("La URL es alcanzable")
            // indicamos en la db que es alcanzable
            isUrlReachableUseCase.setCodeStatus(result.hash, 1)
        }
        println("Valor del alcanzable: ${isUrlReachableUseCase.getCodeStatus(result.hash)}")
        // ---------------------------------------------------------------

        return ResponseEntity(response, h, HttpStatus.CREATED)
    }

    // Función para convertir el límite a entero
    private fun limiteALong(limit: String): Long {
        try {
            return limit.toLong()
        } catch (e: NumberFormatException) {
            return -1L
        }
    }

    @GetMapping("/api/link/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun returnInfo(@PathVariable id: String): List<Info> = returnInfoUseCase.returnInfo(id)

    @GetMapping("/api/stats/metrics/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun returnSystemInfo(@PathVariable id: String):
            SystemInfo = returnSystemInfoUseCase.returnSystemInfo(id)

    private fun obtenerInformacionUsuario(
        userAgent: String,
        request: HttpServletRequest
    ): ClickProperties {
        // Lógica para extraer el sistema operativo y el navegador del User-Agent
        val uaParser = Parser()
        val client = uaParser.parse(userAgent)

        // Obtener información sobre el sistema operativo y el navegador
        val operatingSystem = client.os.family
        val browser = client.userAgent.family
        val ip = request.remoteAddr
        var cityName: String? = null
        var countryIsoCode: String? = null

        if (ip != "0:0:0:0:0:0:0:1" && ip != "127.0.0.1") {
            // Obtener información de la ciudad basada en la dirección IP
            val ipAddress = InetAddress.getByName(ip)
            val cityResponse: CityResponse = reader.city(ipAddress)

            // Extraer información específica de la ciudad (puedes ajustar según tus necesidades)
            cityName = cityResponse.city.name
            countryIsoCode = cityResponse.country.isoCode
        }

        // Mostrar toda la informacion del usuario que solicita la redireccion
        println(
            "Usuario solicita redireccion: SistemaOperativo[$operatingSystem], Navegador[$browser], " +
                    "IP[$ip], Ciudad[$cityName], Pais[$countryIsoCode]"
        )

        val propiedades = ClickProperties(
            ip = request.remoteAddr, os = operatingSystem,
            browser = browser, country = countryIsoCode, city = cityName
        )
        return propiedades
    }

    //para los casos de error.
    //data class ErrorResponse(val error: String, val message: String)

    @GetMapping("/{id:(?!api|index).*}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    fun returnQr(@PathVariable id: String, @RequestHeader(value = "User-Agent", required = false) userAgent: String?): ResponseEntity<out Serializable> {
        // Obtener información sobre la URL corta utilizando la nueva función
        val qrInfo = qrUseCase.getInfoForQr(id)

        return when {
            // PARA QUE FUNCIONE DE MOMENTO ALCANZABLE A 0 PORQUE NADIE LO MODIFICA !!!!!!!!!!!!!!!!!!!!!!!!!
            qrInfo.hayQr == 1 && qrInfo.alcanzable == 1 -> {
                // La URL corta existe, es redireccionable y tiene un código QR, devolver QR
                return qrInfo.imageBytes?.let {
                    ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(it)
                } ?: ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener el código QR")
            }
            qrInfo.hayQr == 2 -> {
                // La URL corta existe, pero el código QR está en proceso de creación
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Retry-After", "10")
                    .body("Código QR en proceso de creación")
            }
            else -> {
                // Otros casos como no redireccionable, no operativa, spam, etc.
                // Puedes agregar lógica adicional según tus necesidades
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No se puede redirigir a esta URL corta en este momento")
            }
        }

        /*

            COSAS A ACABAR Y VER COMO SACAR:
                * Si la URI recortada existe --> Esto implica que si el hayQr es 1 o 2 es que si que existe
                * Si se ha confirmado que se puede realizar la redirección --> alcanzable¿?
                * Como hacer: "¿todavía no se ha confirmado si se puede o no realizar la redirección?"
                * Como hacer: "se han enviado demasiadas peticiones durante una cantidad de tiempo determinada"
                * No puede utilizarse para redirecciones porque no está operativa
                * No puede ser utilizada para redirecciones por spam
         */
    }


}
