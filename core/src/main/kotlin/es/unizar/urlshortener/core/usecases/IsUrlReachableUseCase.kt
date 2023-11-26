@file:Suppress("WildcardImport","TooManyFunctions","TooGenericExceptionCaught","MagicNumber","NewLineAtEndOfFile")
package es.unizar.urlshortener.core.usecases

import java.net.HttpURLConnection
import java.net.URL

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
class IsUrlReachableUseCaseImpl : IsUrlReachableUseCase {
    override fun isUrlReachable(urlString: String): Int {
        var attempt = 0
        val maxAttempts = 3
        while (attempt < maxAttempts) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                // connection.requestMethod = "GET"
                //connection.connectTimeout = 5000 // Tiempo de espera en milisegundos

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return HttpURLConnection.HTTP_OK
                } else {
                    attempt++
                    Thread.sleep(1000) // Espacio de 1 segundo entre intentos
                }
            } catch (e: Exception) {
                // Log o manejo de la excepción si es necesario
                attempt++
                Thread.sleep(1000) // Espacio de 1 segundo entre intentos
                println("Error al verificar la URL: ${e.message} en el intento $attempt")
            }
        }
        return HttpURLConnection.HTTP_BAD_REQUEST
    }
}