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
import java.net.URI
import ua_parser.Parser
import ua_parser.Client
import java.io.File
import com.maxmind.db.Reader
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import org.springframework.web.bind.annotation.*
import java.io.FileNotFoundException
import java.net.InetAddress

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
        @RequestParam(required = false, defaultValue = "0") limit: String
    ): ResponseEntity<ShortUrlDataOut>

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

    // A File object pointing to your GeoIP2 or GeoLite2 database
    private val database = File("../GeoLite2-City.mmdb")

    private val reader: DatabaseReader = DatabaseReader.Builder(database).build()

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown User-Agent"
        // Lógica para extraer el sistema operativo y el navegador del User-Agent
        val uaParser = Parser()
        val client = uaParser.parse(userAgent)

        // Obtener información sobre el sistema operativo y el navegador
        val operatingSystem = client.os.family
        val browser = client.userAgent.family
        val ip = request.remoteAddr

        if (ip != "0:0:0:0:0:0:0:1" && ip != "127.0.0.1") {
            // Obtener información de la ciudad basada en la dirección IP
            val ipAddress = InetAddress.getByName(ip)
            val cityResponse: CityResponse = reader.city(ipAddress)

            // Extraer información específica de la ciudad (puedes ajustar según tus necesidades)
            val cityName = cityResponse.city.name
            val countryIsoCode = cityResponse.country.isoCode

            // Muestra información de la ciudad por la consola
            println("City: $cityName")
            println("Country ISO Code: $countryIsoCode")
        }

        // Muestra el sistema operativo y el navegador por la consola
        println("Operating System: $operatingSystem")
        println("Browser: $browser")
        println("Client IP: $ip")

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

    // curl -v -d "url=http://www.unizar.es/&limit=3" http://localhost:8080/api/link para especificar el límite

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(
        data: ShortUrlDataIn,
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") limite: String
    ): ResponseEntity<ShortUrlDataOut> {
        val result = createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                limit = limite
            )
        )

        println("El límite es: $limite")

        val h = HttpHeaders()
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(result.hash, request) }.toUri()
        h.location = url

        val response = ShortUrlDataOut(
            url = url,
            properties = mapOf(
                "safe" to result.properties.safe
            )
        )

        return ResponseEntity(response, h, HttpStatus.CREATED)
    }

    @GetMapping("/api/link/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun returnInfo(@PathVariable id: String): List<Info> = returnInfoUseCase.returnInfo(id)
}
//    @GetMapping("/api/link/{id:(?!api|index).*}", produces = [MediaType.APPLICATION_JSON_VALUE])
//    override fun returnInfo(@PathVariable id: String): List<Info> =  checkIdOrThrow(id) { id ->
//        returnInfoUseCase.returnInfo(id)
//    }

//    private fun <T> checkIdOrThrow(id: String, block: (String)->T): T {
//        // chequeo que esta bien o no, y si eta mal lanzo la excepción correspondiente
//        block(id)
//    }
