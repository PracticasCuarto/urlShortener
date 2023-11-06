package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.RedirectSummary
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
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
    val createShortUrlUseCase: CreateShortUrlUseCase
) : UrlShortenerController {


    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {

        val userAgent = request.getHeader("User-Agent") ?: "Unknown User-Agent"
        val ip = request.remoteAddr
        // Lógica para extraer el sistema operativo y el navegador del User-Agent
        val (operatingSystem, browser) = parseUserAgentDetails(userAgent)

        // Muestra el sistema operativo y el navegador por la consola
        println("Operating System: $operatingSystem")
        println("Browser: $browser")

        // Crear el objeto RedirectSummary con los datos del sistema operativo, navegador y url
         val info = RedirectSummary(
            os = operatingSystem,
            browser = browser,
            url = request.requestURL.toString()
        )

        redirectUseCase.redirectTo(id, info).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            return ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }
    }

    // Función para extraer el sistema operativo y el navegador del User-Agent
    private fun parseUserAgentDetails(userAgent: String): Pair<String, String> {
        // Lógica para extraer el sistema operativo y el navegador del User-Agent
        // Utiliza expresiones regulares u otros métodos de análisis de cadenas para identificarlos
        // Este ejemplo es básico y puede no cubrir todos los casos

        val operatingSystem = when {
            userAgent.contains("Windows") -> "Windows"
            userAgent.contains("Mac") -> "Macintosh"
            userAgent.contains("Android") -> "Android"
            userAgent.contains("iOS") -> "iOS"
            else -> "null"
        }

        val browser = when {
            userAgent.contains("Firefox") -> "Firefox"
            userAgent.contains("Chrome") -> "Chrome"
            userAgent.contains("Safari") -> "Safari"
            userAgent.contains("Edge") -> "Edge"
            else -> "null"
        }

        return Pair(operatingSystem, browser)
    }

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


}
