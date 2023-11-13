@file:Suppress("MatchingDeclarationName", "WildcardImport")

package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
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
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URI

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setup() {
        val httpClient = HttpClientBuilder.create()
            .disableRedirectHandling()
            .build()
        (restTemplate.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory).httpClient = httpClient

        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click", "redirect_summary")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click", "redirect_summary")
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
        // Especifica la IP deseada, por ejemplo, "188.99.61.3" Esta en la ciudad Igualada,Barcelona
        val specifiedIp = "188.77.145.43"
        val target = shortUrl("http://example.com/").headers.location
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"
        headers["X-Forwarded-For"] = specifiedIp
        val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)

        // Imprime el contenido de la tabla "click" de la base de datos
        val clickTableContent = jdbcTemplate.queryForList("SELECT * FROM click")
        println("Contenido de la tabla 'click': $clickTableContent")

        // Verifica que la IP especificada esté en la base de datos
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "click",
            "ip = '$specifiedIp' AND Country = 'ES' AND City = 'Igualada'")).isEqualTo(1)
    }

    @Test
    fun `redirectTo adds to the database the ip and the location of the user for non-existent short URL`() {
        // Especifica la IP deseada, por ejemplo, "188.99.61.3" Esta en la ciudad Igualada,Barcelona
        val specifiedIp = "188.77.145.43"
        val target = shortUrl("http://www.mcdonaldsnoessano.com/").headers.location
        val headers = HttpHeaders()
        headers["User-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36"
        headers["X-Forwarded-For"] = specifiedIp
        val response = restTemplate.exchange(target, HttpMethod.GET, HttpEntity<Unit>(headers), String::class.java)

        // Imprime el contenido de la tabla "click" de la base de datos
        val clickTableContent = jdbcTemplate.queryForList("SELECT * FROM click")
        println("Contenido de la tabla 'click': $clickTableContent")

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
}
