package es.unizar.urlshortener.core.usecases

// import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/*
 * Privet global variables
 */
private const val MAX_ATTEMPTS = 3
private const val WAIT_TIME = 1000L

/**
 * Given an url in string format, returns an int that represents the status code of the url.
 * If the url is reachable, the status code will be 200 (HTTP_OK).
 * If the url is not reachable, the status code will be 400 (HTTP_BAD_REQUEST).
 */
interface IsUrlReachableUseCase {
    fun isUrlReachable(urlString: String): Int
}

/**
 * Implementation of [IsUrlReachableUseCase].
 */
class IsUrlReachableUseCaseImpl(
    val connect: (it: String) -> Boolean = {
    val url = URL(it)
    val connection = url.openConnection() as HttpURLConnection
    // connection.requestMethod = "GET"
    //connection.connectTimeout = 5000 // Tiempo de espera en milisegundos

        connection.responseCode == HttpURLConnection.HTTP_OK

}
    // PONER UNA COMA ANTES DE ESTO (despues de la llave) MARCOS
    //private val shortUrlEntityRepository: ShortUrlRepositoryService
) : IsUrlReachableUseCase {
    override fun isUrlReachable(urlString: String): Int {
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            runCatching {
                if (!connect(urlString)) {
                    attempt++
                    Thread.sleep(WAIT_TIME) // Espacio de 1 segundo entre intentos
                }
            }.onFailure {
                // Manejar problemas de entrada/salida durante la conexión
                println("Error de E/S al conectar con la URL: ${it.message}")
                attempt++
                Thread.sleep(WAIT_TIME) // Espacio de 1 segundo entre intentos
            }
        }
        return if (attempt == MAX_ATTEMPTS) {
            // Si se ha superado el número máximo de intentos, se devuelve un código de error
            HttpURLConnection.HTTP_BAD_REQUEST
        } else {
            // Si no se ha superado el número máximo de intentos, se devuelve un código de éxito
            HttpURLConnection.HTTP_OK
        }
    }
}

val isUrlReachableUseCaseGoodMock = IsUrlReachableUseCaseImpl( { true })
val isUrlReachableUseCaseBadMock = IsUrlReachableUseCaseImpl( { false })


// La parte de probar la conexion 3 veces hacerla una interfaz y que esta implementacion actual que sea una
// implementacion concreta
// A parte poder tener otras implementaciones especiales que sirvan para los tests (una devuelve siempre bien,
// otra siempre mal, otra que a la tercera devuelve bien, etc) Hacerlo en app o delivery
