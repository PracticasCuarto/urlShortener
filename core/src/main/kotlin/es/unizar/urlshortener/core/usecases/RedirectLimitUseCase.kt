@file:Suppress("EmptyDefaultConstructor", "UnusedPrivateProperty")
/**
 * Package containing use cases related to the core functionality of URL shortener.
 */
package es.unizar.urlshortener.core.usecases

import io.micrometer.core.instrument.Counter
import java.time.LocalDateTime

/**
 * Use case interface for managing redirection limits and statistics.
 */
interface RedirectLimitUseCase {

    /**
     * Checks if a new redirection is allowed, and updates the total number of redirects if allowed.
     *
     * @param hash The hash associated with the shortened link.
     */
    fun newRedirect(hash: String) : Boolean

}

/**
 * Data class representing information about the redirects of a given url
 * @property hash Hash associated with the shortened link.
 * @property time Time of the last redirect.
 * @property counter Number of redirects in the last hour.
 * @property limit Maximum number of redirects allowed in an hour (0 if no limit)
 **/

data class Redirects(
    val hash: String? = null,
    val time: LocalDateTime? = null,
    val counter: Counter? = null,
    val limit: Int? = 0
)

/**
 * Implementation of the [RedirectLimitUseCase] interface.
 *
 * @property shortUrlRepository Service for accessing short URL-related data in the repository.
 */
class RedirectLimitUseCaseImpl(
) : RedirectLimitUseCase {

    // Lista para almacenar redirecciones
    private val redirectsList = mutableListOf<Redirects>()

    /**
     * Checks if a new redirection is allowed, and updates the total number of redirects if allowed.
     *
     * @param hash The hash associated with the shortened link.
     */
    override fun newRedirect(hash: String) : Boolean {
        val exists = findRedirectByHash(hash)
        if (!exists) {
            // Si no existe no hacemos nada
            return false
        }

        val redirect = obtainRedirectByHash(hash)

        // Obtener el numero de veces que se ha hecho la redireccion
        val numRedirecciones = redirect?.counter?.count()?.toInt()
        return false
    }

    // Función para buscar un elemento en la lista por hash
    private fun findRedirectByHash(hashActual: String): Boolean {
        return redirectsList.any { it.hash == hashActual }
    }

    // Función para buscar un elemento en la lista por hash
    private fun obtainRedirectByHash(hashActual: String): Redirects? {
        return redirectsList.find { it.hash == hashActual }
    }


}

//private val counter: Counter = Counter.builder("news_fetch_request_total").
//tag("version", "v1").
//description("News Fetch Count").
//register(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

// Printear el nombre del counter
//println("El nombre del counter es: ${counter.id.name}")
//println("Nueva redireccion")
//counter.increment()
//println("El contador es: ${counter.count()}")

