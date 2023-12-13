@file:Suppress("MatchingDeclarationName", "WildcardImport")

package es.unizar.urlshortener

import com.fasterxml.jackson.databind.ObjectMapper
import es.unizar.urlshortener.core.usecases.isUrlReachableUseCaseBadMock
import es.unizar.urlshortener.core.usecases.isUrlReachableUseCaseGoodMock
import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import es.unizar.urlshortener.infrastructure.delivery.UrlShortenerControllerImpl
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.net.URI


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var urlShortenerController: UrlShortenerControllerImpl

    @BeforeEach
    fun setup() {
        val httpClient = HttpClientBuilder.create()
            .disableRedirectHandling()
            .build()
        (restTemplate.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory).httpClient = httpClient

        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @Test
    fun `main page works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("A front-end example page for the project")
    }

    @Test
    fun `shortener returns a redirect when the key exists`() {
        val target = shortUrl("http://example.com/").headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(1)
    }

    @Test
    fun `shortener limits the amount of redirections in the database`() {
        val target = shortUrl("http://example.com/", "3").headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        // Imprime el contenido de la tabla "click" de la base de datos
        val shorturlTableContent = jdbcTemplate.queryForList("SELECT * FROM shorturl")
        println("Contenido de la tabla 'shorturl': $shorturlTableContent")

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "shorturl","limit = '3'" )).isEqualTo(1)
    }

    @Test
    fun `redirectTo processes user agent`() {
        val target = shortUrl("http://example.com/").headers.location
        require(target != null)
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"
        val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "click",
            "browser = 'Chrome'")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "click",
            "os = 'Mac OS X'")).isEqualTo(1)
    }

    @Test
    fun `redirectTo doesnt process invalid user agent`() {
        val target = shortUrl("http://example.com/").headers.location
        require(target != null)
        val headers = HttpHeaders()
        headers["User-agent"] = "asdnklajsd"
        val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "click",
            "browser = 'Other'")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "click",
            "os = 'Other'")).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val response = restTemplate.getForEntity("http://localhost:$port/f684a3c4", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `redirectTo adds to the database the ip and the location of the user for existing short URL`() {
        // forzamos a que la url sea alcanzable
        // URL reachable mock
        val reachableMock = isUrlReachableUseCaseGoodMock

        // Configure the controller to use the reachableMock
        urlShortenerController.isUrlReachableUseCase = reachableMock

        // Especifica la IP deseada, por ejemplo, "188.99.61.3" Esta en la ciudad Igualada,Barcelona
        val specifiedIp = "188.77.145.43"
        val target = shortUrl("http://example.com/").headers.location
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"
        headers["X-Forwarded-For"] = specifiedIp
        restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)

        // Imprime el contenido de la tabla "click" de la base de datos
        val clickTableContent = jdbcTemplate.queryForList("SELECT * FROM click")
        println("Contenido de la tabla 'click': $clickTableContent")

        // Verifica que la IP especificada esté en la base de datos
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "click",
            "ip = '$specifiedIp' AND Country = 'ES' AND City = 'Igualada'")).isEqualTo(1)
    }

    @Test
    fun `redirectTo adds to the database the ip and the location of the user for non-existent short URL`() {
        // forzamos a que la url sea alcanzable
        // URL reachable mock
        val reachableMock = isUrlReachableUseCaseGoodMock

        // Configure the controller to use the reachableMock
        urlShortenerController.isUrlReachableUseCase = reachableMock

        // Especifica la IP deseada, por ejemplo, "188.99.61.3" Esta en la ciudad Igualada, Barcelona
        val specifiedIp = "188.77.145.43"
        val target = shortUrl("http://www.mcdonaldsnoessano.com/").headers.location
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"
        headers["X-Forwarded-For"] = specifiedIp
        restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)

        // Verifica que la IP especificada esté en la base de datos
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "click",
            "ip = '$specifiedIp' AND Country = 'ES' AND City = 'Igualada'")).isEqualTo(1)
    }


    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val response = shortUrl("http://example.com/")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/f684a3c4"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/f684a3c4"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "ftp://example.com/"

        val response = restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers),
            ShortUrlDataOut::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(0)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    private fun shortUrl(url: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers),
            ShortUrlDataOut::class.java
        )
    }

    private fun shortUrl(url: String, limit: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url
        data["limit"] = limit  // Agrega el parámetro limit=3 al cuerpo de la solicitud

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers),
            ShortUrlDataOut::class.java
        )
    }

    @Test
    fun `shortener returns a shortened URL when the URL is reachable`() {
        // URL reachable mock
        val reachableMock = isUrlReachableUseCaseGoodMock

        // Configure the controller to use the reachableMock
        urlShortenerController.isUrlReachableUseCase = reachableMock

        // Perform the shortening operation
        val response = shortUrl("http://example.com/")

        // Assertions
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isNotNull
        assertThat(response.body?.url).isEqualTo(response.headers.location)
    }

    @Test
    fun `shortener returns an error when the URL is not reachable`() {
        // URL not reachable mock
        val notReachableMock = isUrlReachableUseCaseBadMock

        // Configure the controller to use the notReachableMock
        urlShortenerController.isUrlReachableUseCase = notReachableMock

        // Perform the shortening operation
        val response = shortUrl("http://example.com/")

        // Assertions
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.headers.location).isNull()
        assertThat(response.body?.url).isEqualTo(null)
    }

    // Test que comprueba que cuando se hace una redireccion a una URL, se sume 1 a la metrica de totalRedirecciones
    @Test
    fun `redirectTo increments totalRedirecciones and totalRedireccionesHash metrics when a redirection is made`() {
        // Forzamos a que la URL sea alcanzable
        // URL reachable mock
        val reachableMock = isUrlReachableUseCaseGoodMock
        // Configuramos el controlador para usar el reachableMock
        urlShortenerController.isUrlReachableUseCase = reachableMock
        // Realizamos la operación de acortamiento
        val response = shortUrl("http://example.com/")

        // Hacer la redirección
        restTemplate.getForEntity(response.headers.location, String::class.java)
        // Esperar 6000
        Thread.sleep(6000)

        // Obtener el valor actualizado de la métrica totalRedirecciones después de la redirección
        val updatedRedirectionCount = getTotalRedireccionesMetric()
        // Obtener el valor actualizado de la métrica totalRedireccionesHash después de la redirección
        val updatedRedirectionHashCount = getTotalRedireccionesHashMetric()

        // Verificar que la métrica totalRedirecciones se incrementó en 1 después de la redirección
        assertThat(updatedRedirectionCount).isEqualTo(1)
        // Verificar que la métrica totalRedireccionesHash se incrementó en 1 después de la redirección
        assertThat(updatedRedirectionHashCount).isEqualTo(1)
    }

    // Test que compruebe que cuando se hacen 5 redirecciones a una URL, se sume 5 a la métrica de totalRedirecciones
    // pero no se sume a la metrica de totalRedireccionesHash porque no se solicita a la misma URL
    @Test
    fun `redirectTo increments totalRedirecciones and totalRedireccionesHash metrics when 3 redirections are made`() {
        // Forzamos a que la URL sea alcanzable
        // URL reachable mock
        val reachableMock = isUrlReachableUseCaseGoodMock
        // Configuramos el controlador para usar el reachableMock
        urlShortenerController.isUrlReachableUseCase = reachableMock
        // Realizamos la operación de acortamiento
        var response = shortUrl("http://example.com/")

        // Hacer 3 redirecciones
        repeat(3) {
            restTemplate.getForEntity(response.headers.location, String::class.java)
            // Esperar 6000
            Thread.sleep(6000)
        }
        response = shortUrl("https://youtube.com")
        restTemplate.getForEntity(response.headers.location, String::class.java)
        // Esperar 7000
        Thread.sleep(7000)

        // Obtener el valor actualizado de la métrica totalRedirecciones después de la redirección
        val updatedRedirectionCount = getTotalRedireccionesMetric()
        // Obtener el valor actualizado de la métrica totalRedireccionesHash después de la redirección
        val updatedRedirectionHashCount = getTotalRedireccionesHashMetric()

        println("updatedRedirectionCount: $updatedRedirectionCount")
        println("updatedRedirectionHashCount: $updatedRedirectionHashCount")
        // Verificar que la métrica totalRedirecciones se incrementó en 5 después de la redirección
        assertThat(updatedRedirectionCount).isEqualTo(4)
        // Verificar que la métrica totalRedireccionesHash se incrementó en 5 después de la redirección
        assertThat(updatedRedirectionHashCount).isEqualTo(3)
    }

    @Test
    fun `redirectTo redirects when limit is 0`() {
        val target = shortUrl("http://example.com/", "0").headers.location
        require(target != null)
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"

        // Probar por ejemplo 3 veces a solicitar
        repeat(3) {
            val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)
            assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
            assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))
        }
    }

    @Test
    fun `redirectTo redirects 3 times when limit is 3`() {
        val target = shortUrl("http://example.com/", "3").headers.location
        require(target != null)
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"

        // Probar por ejemplo 3 veces a solicitar
        repeat(3) {
            val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)
            assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
            assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))
        }
    }

    @Test
    fun `redirectTo redirects limits the number of redirects to 3`() {
        val target = shortUrl("http://example.com/", "3").headers.location
        require(target != null)
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"

        // Probar por ejemplo 3 veces a solicitar
        repeat(3) {
            val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)
            assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
            assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))
        }

        // Comprobar que ahora no se redirige y se devuelve estado 429
        val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test
    fun `create doesnt allow a limit lesser than 0`() {
        val target = shortUrl("http://example.com/", "-1").headers.location
        assertThat(target).isNull()
    }

    @Test
    fun `returnInfo returns the limit correctly`() {
        val target = shortUrl("http://example.com/", "3").headers.location
        require(target != null)

        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        val infoResponse = restTemplate.getForEntity("http://localhost:$port/api/link/f684a3c4", String::class.java)
        println("infoResponse: ${infoResponse.body}")
        assertThat(infoResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(infoResponse.body).contains("\"limit\":3")
    }

    @Test
    fun `returnInfo returns the amount of pending redirections correctly`() {
        val target = shortUrl("http://example.com/", "3").headers.location
        require(target != null)

        // Repetir 3 veces y comprobar que el numRedirecciones va aumentando
        repeat(3) {
            val response = restTemplate.getForEntity(target, String::class.java)
            assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
            assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

            val infoResponse = restTemplate.getForEntity("http://localhost:$port/api/link/f684a3c4", String::class.java)
            println("infoResponse: ${infoResponse.body}")
            assertThat(infoResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(infoResponse.body).contains("\"numRedirecciones\":${it+1}")
        }
    }


    private fun getTotalRedireccionesMetric(): Int {
        // Obtener el valor de la métrica totalRedirecciones desde el endpoint JSON
        val statsEndpoint = "http://localhost:$port/api/stats/metrics/f684a3c4"
        val metricResponse = restTemplate.getForEntity(statsEndpoint, String::class.java)
        // Parsear el JSON para obtener el valor de la métrica totalRedirecciones
        return ObjectMapper().readTree(metricResponse.body).get("totalRedirecciones").asInt()
    }

    private fun getTotalRedireccionesHashMetric(): Int {
        // Obtener el valor de la métrica totalRedireccionesHash desde el endpoint JSON
        val statsEndpoint = "http://localhost:$port/api/stats/metrics/f684a3c4"
        val metricResponse = restTemplate.getForEntity(statsEndpoint, String::class.java)
        // Parsear el JSON para obtener el valor de la métrica totalRedireccionesHash
        return ObjectMapper().readTree(metricResponse.body).get("totalRedireccionesHash").asInt()
    }


}
