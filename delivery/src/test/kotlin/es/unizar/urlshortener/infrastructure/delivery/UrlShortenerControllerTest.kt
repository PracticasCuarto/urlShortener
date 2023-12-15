@file:Suppress("WildcardImport", "ForbiddenComment", "UnusedPrivateProperty")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class
    ]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var returnInfoUseCase: ReturnInfoUseCase

    @MockBean
    private lateinit var isUrlReachableUseCase: IsUrlReachableUseCase

    @MockBean
    private lateinit var qrUseCase: QrUseCase

    @MockBean
    private lateinit var redirectLimitUseCase: RedirectLimitUseCase

    @MockBean
    private lateinit var returnSystemInfoUseCase: ReturnSystemInfoUseCase

    @MockBean
    private lateinit var msgUseCase: MsgUseCase

    @MockBean
    private lateinit var msgUseCaseReachable: MsgUseCaseReachable

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))
        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }
        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))
        // Asumimos que el límite de redirecciones no se ha alcanzado y que la URL es válida
        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)
        given(isUrlReachableUseCase.isUrlReachable("http://example.com/","f684a3c4")).willReturn(true)

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns bad request if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }
        // Asumimos que el límite de redirecciones no se ha alcanzado y que la URL es válida
        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)
        given(isUrlReachableUseCase.isUrlReachable("http://example.com/","f684a3c4")).willReturn(true)

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

        @Test
        fun `returnInfo returns a list of info when the key exists`() {
            // Hacer una petición con argumentos os: Windows, browser: Chrome
            given(returnInfoUseCase.returnInfo("key"))
                .willReturn(InfoHash(0,0,
                    listOf(Info("0:0:0:0:0:0:0:1", "Macintosh", "Chrome"))))

            mockMvc.perform(get("/api/link/{id}", "key"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("lista[0].ip").value("0:0:0:0:0:0:0:1"))
                .andExpect(jsonPath("lista[0].os").value("Macintosh"))
                .andExpect(jsonPath("lista[0].browser").value("Chrome"))
        }

    @Test
    fun `returnInfo returns a not found when the key does not exist`() {
        given(returnInfoUseCase.returnInfo("malo"))
            .willAnswer { throw InformationNotFound("malo") }

        mockMvc.perform(get("/api/link/{id}", "malo"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash given a redirection limit`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", limit = 1)
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))
        // Asumimos que el límite de redirecciones no se ha alcanzado y que la URL es válida
        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)
        given(isUrlReachableUseCase.isUrlReachable("http://example.com/","f684a3c4")).willReturn(true)

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns a bad request if its given an invalid redirection limit`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", limit = -1)
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("limit", "-1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))

    }

    @Test
    fun `newRedirect returns a too many conections if it passes the limit`() {
        given(redirectLimitUseCase.newRedirect("key")).willReturn(false)

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isTooManyRequests)
    }

    // Test que comprueba el funcionamiento del isUrlReachableUseCase si SI es alcanzable
    @Test
    fun `isUrlReachable returns a ok if the url is reachable`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", limit = 0)
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        verify(msgUseCaseReachable).sendMsg("cola_2", "f684a3c4 https://example.com/")

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andDo(print())
            .andExpect(status().isCreated)

    }

    // Test que comprueba el funcionamiento del isUrlReachableUseCase si NO es alcanzable
    @Test
    fun `isUrlReachable returns a bad request if the url is not reachable`() {
        given(isUrlReachableUseCase.isUrlReachable("http://notexample.com/","f684a3c4")).willReturn(false)

        given(returnInfoUseCase.returnInfo("malo"))
            .willAnswer { throw InformationNotFound("malo") }

        mockMvc.perform(get("/api/link/{id}", "malo"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))
    }
    @Test
    fun `returnSystemInfo returns the four metrics correctly`() {
        given(returnSystemInfoUseCase.returnSystemInfo("key"))
            .willReturn(SystemInfo(1.0, 2.0, 3, 4))

        mockMvc.perform(get("/api/stats/metrics/{id}", "key"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.memoryUsed").value(1.0))
            .andExpect(jsonPath("$.upTime").value(2.0))
            .andExpect(jsonPath("$.totalRedirecciones").value(3))
            .andExpect(jsonPath("$.totalRedireccionesHash").value(4))
    }



    // Test para comprobar que devuelve el código QR cuando se crea correctamente
    @Test
    fun `getQrImageBytes returns a QR code when it is created correctly`() {
        // Configuración del escenario de prueba
        val validUrl = "https://example.com/"
        val validHash = "f684a3c4"


        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", limit = 0)
            )
        ).willReturn(ShortUrl(validHash, Redirection(validUrl)))

        // given? - lo que tiene que ver con salvar en la base de datos la nueva url recortada
        verify(msgUseCase).sendMsg("cola_1", "f684a3c4 https://example.com/")

        // verify - que se ha guardado en la base de datos (save llamado)
        // verify - que se ha llamado la función que crea qr
        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andDo(print())
            .andExpect(status().isCreated)

    }

}
