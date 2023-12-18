@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
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
    fun isUrlReachable(urlString: String, hash: String): Boolean

    fun setCodeStatus(hash: String, status: Int)

    fun getCodeStatus(hash: String) : Int

    fun existsID(id: String): Boolean

    fun  getInfoForReachable(id: String)

}

/**
 * Implementation of [IsUrlReachableUseCase].
 */
class IsUrlReachableUseCaseImpl(
    val connect: (it: String) -> Boolean = {
    val url = URL(it)
    val connection = url.openConnection() as HttpURLConnection
        connection.responseCode == HttpURLConnection.HTTP_OK

},
            val shortUrlEntityRepository: ShortUrlRepositoryService
) : IsUrlReachableUseCase {


    override fun isUrlReachable(urlString: String, hash: String): Boolean {
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            runCatching {
                if (connect(urlString)) {
                    attempt = MAX_ATTEMPTS+1 // Si la conexión es correcta, se sale del bucle
                                            // sumamos uno por el if de mas abajo
                } else {
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
            // Si se ha superado el número máximo de intentos, se devuelve false
            false
        } else {
            // Si no se ha superado el número máximo de intentos, se devuelve true
            true
        }
    }

    override fun setCodeStatus(hash: String, status: Int) {
        // escribimos en la base de datos el estado del calculo de la alcanzabilidad
        // 0 no existe, 1 creado y 2 creandose.
        shortUrlEntityRepository.updateAlcanzable(hash, status)
    }

    override fun getCodeStatus(hash: String): Int {
        return shortUrlEntityRepository.obtainAlcanzable(hash)
    }

    override fun existsID(id: String): Boolean {
        return shortUrlEntityRepository.existe(id)
    }

    override fun getInfoForReachable(id: String) {
        if (!existsID(id)) {
            throw RedirectionNotFound("El ID no existe en la base de datos")
        }

        val alcanzable = getCodeStatus(id)

         if (alcanzable == 2){
             // La URL corta existe, pero no sabemos todavia si es alcanzable o no
             throw CalculandoException("Alcanzabilidad en proceso de creacion")
         }
        else if (alcanzable == 0){
            // La URL corta no es alcanzable
            throw InvalidExist( "No se puede redirigir a esta URL")
        }

    }
}

fun isUrlReachableUseCaseGoodMock( shortUrlEntityRepository:
                                   ShortUrlRepositoryService ) = IsUrlReachableUseCaseImpl( { true },
                                                                                            shortUrlEntityRepository)
fun isUrlReachableUseCaseBadMock( shortUrlEntityRepository:
                                  ShortUrlRepositoryService ) = IsUrlReachableUseCaseImpl( { false },
                                                                                            shortUrlEntityRepository)


// La parte de probar la conexion 3 veces hacerla una interfaz y que esta implementacion actual que sea una
// implementacion concreta
// A parte poder tener otras implementaciones especiales que sirvan para los tests (una devuelve siempre bien,
// otra siempre mal, otra que a la tercera devuelve bien, etc) Hacerlo en app o delivery
