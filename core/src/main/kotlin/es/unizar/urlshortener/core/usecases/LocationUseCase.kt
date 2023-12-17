/**
 * Package containing use cases related to the core functionality of URL shortener.
 */
package es.unizar.urlshortener.core.usecases

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.InformationNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import java.net.InetAddress
import ua_parser.Parser
import es.unizar.urlshortener.core.usecases.LogClickUseCase

/**
 * Use case interface for returning information about users who have clicked a shortened link.
 */
interface LocationUseCase {

    /**
     * Given a key, returns a list of [Info] objects containing information about users who clicked the link.
     *
     * @param key The key associated with the shortened link.
     * @return A list of [Info] objects representing user click information.
     * @throws InformationNotFound if no information is found for the given key.
     */
    fun obtenerInformacionUsuario(userAgent: String, ip: String, id: String): ClickProperties
}

class LocationUseCaseImpl (
    val logClickUseCase: LogClickUseCase
): LocationUseCase {

    @Value("classpath:GeoLite2-City.mmdb")
    // A File object pointing to your GeoIP2 or GeoLite2 database
    private lateinit var database: Resource

    private val reader: DatabaseReader by lazy {
        DatabaseReader.Builder(database.file).build()
    }

    /**
     * Given a key, returns a list of [Info] objects containing information about users who clicked the link.
     *
     * @param key The key associated with the shortened link.
     * @return A list of [Info] objects representing user click information.
     * @throws InformationNotFound if no information is found for the given key.
     */
    override fun obtenerInformacionUsuario(
        userAgent: String,
        ip: String,
        id: String
    ): ClickProperties {
        // Lógica para extraer el sistema operativo y el navegador del User-Agent
        val uaParser = Parser()
        val client = uaParser.parse(userAgent)

        // Obtener información sobre el sistema operativo y el navegador
        val operatingSystem = client.os.family
        val browser = client.userAgent.family
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
            ip = ip, os = operatingSystem,
            browser = browser, country = countryIsoCode, city = cityName
        )

        logClickUseCase.logClick(id, propiedades)
        return propiedades
    }
}
