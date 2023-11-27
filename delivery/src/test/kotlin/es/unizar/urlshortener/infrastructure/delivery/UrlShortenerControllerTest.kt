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
    private lateinit var redirectLimitUseCase: RedirectLimitUseCase

    @MockBean
    private lateinit var returnSystemInfoUseCase: ReturnSystemInfoUseCase

    @MockBean
    private lateinit var qrUseCase: QrUseCase

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

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
                data = ShortUrlProperties(ip = "127.0.0.1", horaRedireccion = null)
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

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
                .willReturn(listOf(Info("0:0:0:0:0:0:0:1", "Macintosh", "Chrome")))

            mockMvc.perform(get("/api/link/{id}", "key"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].ip").value("0:0:0:0:0:0:0:1"))
                .andExpect(jsonPath("$[0].os").value("Macintosh"))
                .andExpect(jsonPath("$[0].browser").value("Chrome"))

            //verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))

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

    // Test para comprobar que se devuelve un 404 cuando se intenta acceder a un código QR que no existe

    // Test para comprobar que devuelve el código QR cuando se crea correctamente
    @Test
    fun `getQrImageBytes returns a QR code when it is created correctly`() {
        // Configuración del escenario de prueba
        val validUrl = "https://example.com"
        val validHash = "8ae9a8dc"
        val qrCodeBytes = byteArrayOf(1, 2, 3)

        given(qrUseCase.generateQRCode(validUrl, validHash))
            .willReturn(qrCodeBytes)

        // Llamada al endpoint que debería generar el código QR
        mockMvc.perform(get("/{id}/qr", validHash))
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes(qrCodeBytes))
    }

}
