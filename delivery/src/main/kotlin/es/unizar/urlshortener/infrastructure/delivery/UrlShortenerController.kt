@file:Suppress("WildcardImport")
package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import ua_parser.Parser
import ua_parser.Client
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
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Returns the information of a short url identified by its [id].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun returnInfo(id: String): List<Info>


}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

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
    val returnInfoUseCase: ReturnInfoUseCase
) : UrlShortenerController {


    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown User-Agent"
        // Lógica para extraer el sistema operativo y el navegador del User-Agent
        val uaParser = Parser()
        val client = uaParser.parse(userAgent)

        // Obtener información sobre el sistema operativo y el navegador
        val operatingSystem = client.os.family
        val browser = client.userAgent.family

        // Muestra el sistema operativo y el navegador por la consola
        println("Operating System: $operatingSystem")
        println("Browser: $browser")

        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr, os = operatingSystem,
                browser = browser))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            return ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }
    }

//    private fun getApproximateLocation(ip: String): String {
//        // Utiliza un servicio web de geolocalización o base de datos para obtener la ubicación
//        // Aquí, se usa un ejemplo con el servicio gratuito de ipstack
//        val apiKey = "tu_api_key"
//        val apiUrl = "http://api.ipstack.com/$ip?access_key=$apiKey"
//
//        val url = URL(apiUrl)
//        val connection = url.openConnection()
//        val content = connection.getInputStream().bufferedReader().use { it.readText() }
//
//        // Analiza la respuesta JSON y extrae la información de ubicación necesaria
//        // Aquí, se asume que la respuesta contiene el país y la ciudad
//        // Puedes ajustar esto según la estructura real de la respuesta
//        val country = "Country" // Reemplazar con el campo real en tu respuesta JSON
//        val city = "City" // Reemplazar con el campo real en tu respuesta JSON
//
//        return "$city, $country"
//    }


    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/api/link/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun returnInfo(@PathVariable id: String): List<Info> = returnInfoUseCase.returnInfo(id)
//    @GetMapping("/api/link/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
//    override fun returnInfo(@PathVariable id: String): List<Info> =  checkIdOrThrow(id) { id ->
//        returnInfoUseCase.returnInfo(id)
//    }

//    private fun <T> checkIdOrThrow(id: String, block: (String)->T): T {
//        // chequeo que esta bien o no, y si eta mal lanzo la excepción correspondiente
//        block(id)
//    }
}
