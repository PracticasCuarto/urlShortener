@file:Suppress("WildcardImport", "MaxLineLength", "ReturnCount", "LongParameterList")
package es.unizar.urlshortener.infrastructure.delivery

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ua_parser.Parser
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime

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
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val returnInfoUseCase: ReturnInfoUseCase,
    val returnSystemInfoUseCase: ReturnSystemInfoUseCase,
    //val metricsEndpoint: MetricsEndpoint
    val redirectLimitUseCase: RedirectLimitUseCase,
    val isUrlReachableUseCase: IsUrlReachableUseCase

) : UrlShortenerController {

    // A File object pointing to your GeoIP2 or GeoLite2 database
    private val database = File("../GeoLite2-City.mmdb")

    private val reader: DatabaseReader = DatabaseReader.Builder(database).build()

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown User-Agent"
        val propiedades = obtenerInformacionUsuario(userAgent, request)

        if (limiteRedirecciones(id)) return ResponseEntity(HttpStatus.TOO_MANY_REQUESTS)

        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, propiedades)
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            return ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }
    }

    // Registra una nueva redirección y devuelve true si el número de redirecciones supera el límite
    private fun limiteRedirecciones(id: String): Boolean {
        // Obtener el límite de redirecciones permitido y el número actual de redirecciones
        val limite = redirectLimitUseCase.obtainLimit(id)
        val numRedirecciones = redirectLimitUseCase.obtainNumRedirects(id)
        val horaAnterior = redirectLimitUseCase.obtenerHora(id)

        // Verificar si hay un límite establecido
        if (limite != 0) {
            if (horaAnterior != null) {
                // Obtener la hora actual
                val horaActual = LocalDateTime.now()
                val duracionTranscurrida = Duration.between(horaAnterior, horaActual)

                println("La hora anterior es: $horaAnterior y la hora actual es: $horaActual")

                // Verificar si ha pasado más de una hora desde la última redirección
                if (duracionTranscurrida >= Duration.ofHours(1)) {
                    // Se ha superado la duración máxima permitida, reiniciar contador y actualizar la hora
                    redirectLimitUseCase.reiniciarNumRedirecciones(id)
                    redirectLimitUseCase.actualizarHora(id, horaActual)
                }
            }

            // Verificar que no se superen el número máximo de redirecciones permitidas
            if (numRedirecciones >= limite) {
                // Se ha alcanzado el limite de redirecciones
                return true
            }
        }

        // No hay límite de redirecciones o no se ha alcanzado el máximo permitido, registrar nueva redirección
        redirectLimitUseCase.newRedirect(id)

        // Imprimir información de depuración
        println("El límite es: $limite y el número de redirecciones es: $numRedirecciones")

        // Se ha alcanzado o superado el número máximo de redirecciones permitidas
        return false
    }


    // curl -v -d "url=http://www.unizar.es/&limit=3" http://localhost:8080/api/link para especificar el límite

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun shortener(
        data: ShortUrlDataIn,
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") limit: String,
    ): ResponseEntity<ShortUrlDataReapose> {
        val limiteInt: Int
        try {
            limiteInt = limit.toInt()
        } catch (e: NumberFormatException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        val datos = ShortUrlProperties(
            ip = request.remoteAddr,
            sponsor = data.sponsor,
            limit = limiteInt,
            numRedirecciones = 0,
            horaRedireccion = null // Cambiar por LocalDateTime.now() para activar el límite de redirecciones
        )

        val result = createShortUrlUseCase.create(
            url = data.url,
            data = datos
        )

        val h = HttpHeaders()
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(result.hash, request) }.toUri()
        h.location = url

        val response = ShortUrlDataOut(
            url = url,
            properties = mapOf(
                "safe" to result.properties.safe
            )
        )

        // comprobamos si la URL es alcanzable
        if (isUrlReachableUseCase.isUrlReachable(data.url) != HttpURLConnection.HTTP_OK) {
            println("La URL no es alcanzable")
            val response = Error(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "URI de destino no alcanzable"
            )
            return ResponseEntity(response,HttpStatus.BAD_REQUEST)
        }
        else {
            println("La URL es alcanzable")
        }

        return ResponseEntity(response, h, HttpStatus.CREATED)
    }

    @GetMapping("/api/link/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun returnInfo(@PathVariable id: String): List<Info> = returnInfoUseCase.returnInfo(id)

    @GetMapping("/api/metrics/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
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
}
