
@file:Suppress("WildcardImport", "ReturnCount", "LongParameterList", "UnusedPrivateProperty",
    "UnusedParameter", "FunctionOnlyReturningConstant")
package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.*
import ua_parser.Parser
import java.io.File
import java.io.Serializable
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
    fun returnInfo(id: String): InfoHash

    fun returnSystemInfo(@PathVariable id: String): SystemInfo

     fun updateSystemInfoURL(@PathVariable id: String)

    fun updateSystemInfoAutomatic()
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
    val locationUseCase: LocationUseCase,
    val msgUseCaseUpdateMetrics: MsgUseCaseUpdateMetrics,
    val msgUseCaseLocation: MsgUseCaseLocation,
    val rabbitSender: RabbitMQSenderService

) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown User-Agent"
         val propiedades = locationUseCase.obtenerInformacionUsuario(userAgent, request.remoteAddr)
        msgUseCaseLocation.sendMsg("cola_3", userAgent + "||||||" + request.remoteAddr)

        // Casos de error alcanzabilidad
        isUrlReachableUseCase.getInfoForReachable(id)

        if (!redirectLimitUseCase.newRedirect(id)) return ResponseEntity(HttpStatus.TOO_MANY_REQUESTS)

        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, propiedades)
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            return ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }
    }

    // curl -v -d "url=http://www.unizar.es/&limit=3" http://localhost:8080/api/link para especificar el límite

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE], produces =
    [MediaType.APPLICATION_JSON_VALUE])
    override fun shortener(
        data: ShortUrlDataIn,
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") limit: String,
        @RequestParam(required = false, defaultValue = "false") hayQr: String,

        ): ResponseEntity<ShortUrlDataReapose> {

        var limiteLong: Long = limiteALong(limit)

        if (limiteLong < 0) {
            return ResponseEntity(Error(HttpStatus.BAD_REQUEST.value(),
                "El límite debe ser mayor o igual que 0"), HttpStatus.BAD_REQUEST)
        }

        val result = createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                limit = limiteLong
            )
        )

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
            println("He entrado para hacer el QR")

            //Enviamos mensaje por la cola 1 para que se genere el QR enviando como string el hash un espacio y la url
            rabbitSender.qrChannelMessage("${result.hash} ${url.toString()}")
        }

        //Comprobamos el valor de hayQr en la base de datos.
        println("Valor del hayQr despues: ${qrUseCase.getCodeStatus(result.hash)}")

        // ALCANZABILIDAD ------------------------------------------------
        // indicamos en la db que todavia lo estamos calculando
        isUrlReachableUseCase.setCodeStatus(result.hash, 2)
        println("calculando alcanzabilidad...")
        println("Valor del alcanzable: ${isUrlReachableUseCase.getCodeStatus(result.hash)}")
        rabbitSender.reachableChannelMessage("${result.hash} ${data.url}")
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
    override fun returnInfo(@PathVariable id: String): InfoHash = returnInfoUseCase.returnInfo(id)

    @GetMapping("/api/stats/metrics/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun returnSystemInfo(@PathVariable id: String):
            SystemInfo = returnSystemInfoUseCase.returnSystemInfo(id)

    @PostMapping("/api/update/metrics")
    override fun updateSystemInfoURL(@PathVariable id: String) {
        returnSystemInfoUseCase.updateSystemInfo()
    }

    @GetMapping("/{id:(?!api|index).*}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    fun returnQr(@PathVariable id: String, @RequestHeader(value = "User-Agent", required = false)
    userAgent: String?): ResponseEntity<*> {
        // Obtener información sobre la URL corta utilizando la nueva función
        val qrInfo = qrUseCase.getInfoForQr(id)

        // La URL corta existe, es redireccionable y tiene un código QR, devolver QR
        return qrInfo.imageBytes?.let {
            ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(it)
        } ?: ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error al obtener el código QR")

    }

    @Scheduled(fixedRate = 6000)
    override fun updateSystemInfoAutomatic() {
        // Escribir en cola de rabbit que se actualice
        msgUseCaseUpdateMetrics.sendMsg("cola_3", "update")

        println("Actualizando sistema automaticamente...")
    }

}
