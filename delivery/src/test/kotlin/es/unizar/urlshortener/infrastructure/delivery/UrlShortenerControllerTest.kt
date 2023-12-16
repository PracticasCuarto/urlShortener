@file:Suppress("WildcardImport", "ForbiddenComment", "UnusedPrivateProperty")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.Captor
import org.mockito.kotlin.eq
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
    private lateinit var rabbitMQSender: RabbitMQSenderService

    @MockBean
    private lateinit var locationUseCase: LocationUseCase

    @MockBean
    private lateinit var msgUseCaseUpdateMetrics: MsgUseCaseUpdateMetrics

    @MockBean
    private lateinit var msgUseCaseLocation: MsgUseCaseLocation


    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }
        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)
        given(isUrlReachableUseCase.getCodeStatus("key")).willReturn(1)


        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    // Test que comprueba el funcionamiento del isUrlReachableUseCase si SI es alcanzable
    @Test
    fun `isUrlReachable returns a created if the url is reachable`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", limit = 0)
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andDo(print())
            .andExpect(status().isCreated)

        //verify(msgUseCaseReachable).sendMsg("cola_2", "f684a3c4 http://example.com/")
        verify(rabbitMQSender).sendSecondChannelMessage("f684a3c4 http://example.com/")

    }

    // Test que comprueba el funcionamiento del isUrlReachableUseCase si NO es alcanzable
    @Test
    fun `isUrlReachable returns a forbidden if the url is not reachable`() {
        given(isUrlReachableUseCase.getInfoForReachable("f684a3c4"))
            .willAnswer { throw InvalidExist("No se puede redirigir a esta URL") }

        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "f684a3c4"))
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.statusCode").value(403))
    }

    @Test
    fun `isUrlReachable returns a bad request if reachability is not calculated`() {
        given(isUrlReachableUseCase.getInfoForReachable("f684a3c4"))
            .willAnswer { throw CalculandoException("Alcanzabilidad en proceso de creacion") }

        given(redirectLimitUseCase.newRedirect("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "f684a3c4"))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    fun `isUrlReachable returns a ok if the url is reachable`() {
        given(redirectUseCase.redirectTo("f684a3c4")).willReturn(Redirection("http://example.com/"))
        given(redirectLimitUseCase.newRedirect("f684a3c4")).willReturn(true)

        given(isUrlReachableUseCase.getCodeStatus("f684a3c4")).willReturn(1)

        mockMvc.perform(get("/{id}", "f684a3c4"))
            .andDo(print())
            .andExpect(status().isTemporaryRedirect)
    }

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


    // Test para comprobar que se llama a la funcion de enviar mensaje de crear QR.
    @Test
    fun `getQrImageBytes returns a QR code when it is created correctly`() {
        // Configuración del escenario de prueba
        val validUrl = "https://example.com/"
        val validHash = "f684a3c4"
        val shortUrl = "http://localhost/f684a3c4"

        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", limit = 0)
            )
        ).willReturn(ShortUrl(validHash, Redirection(validUrl)))

        // when - realizas la solicitud
        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("hayQr", "on") // Simula que el botón del QR se ha pulsado
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andExpect(status().isCreated)

        // then - verificas que se llamó a la función sendMsg con los argumentos correctos
        verify(msgUseCase).sendMsg("cola_1","$validHash $shortUrl")

    }

    // Test el cual comprueba que dado un id no existente, devuleve la excepcion de "ID introducido no existe"
    @Test
    fun `QR returns a isNotFound request if the id does not exist`() {
        given(qrUseCase.getInfoForQr("NoExiste"))
            .willAnswer { throw InformationNotFound("El id introducido no existe") }

        mockMvc.perform(get("/{id}/qr", "NoExiste"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))
    }

    @Test
    fun `Qr returns a badRequest if the qr is not already ready`() {
        given(qrUseCase.getInfoForQr("f684a3c4"))
            .willAnswer { throw CalculandoException("Qr o URL en proceso de creacion") }

        mockMvc.perform(get("/{id}/qr", "f684a3c4"))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    fun `Qr returns a isForbidden if the qr exist but is not valid`() {
        given(qrUseCase.getInfoForQr("f684a3c4"))
            .willAnswer { throw InvalidExist( "No se puede redirigir a esta URL corta") }

        mockMvc.perform(get("/{id}/qr", "f684a3c4"))
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.statusCode").value(403))
    }

    @Test
    fun `QR returns a ok if the QR exist and URL is reachable`() {
        given(qrUseCase.getCodeStatus("281d5122")).willReturn(1)

        // Crea un ByteArray de ejemplo para representar los bytes de la imagen del código QR
        val ejemploBytesImagenQR: ByteArray = "281d5122".toByteArray()

        given(qrUseCase.getInfoForQr("281d5122"))
            .willReturn(QrInfo(1, 1, ejemploBytesImagenQR))

        mockMvc.perform(get("/{id}/qr", "281d5122"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG)) // Verifica que el contenido sea de tipo imagen
    }
}
