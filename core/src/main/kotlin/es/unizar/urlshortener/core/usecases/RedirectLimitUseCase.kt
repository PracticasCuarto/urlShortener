/**
 * Package containing use cases related to the core functionality of URL shortener.
 */
package es.unizar.urlshortener.core.usecases

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.Bucket
import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.time.Duration

const val HORA : Long = 60L


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

    /**
     * Adds a new redirect to the list of redirects.
     *
     * @param hash The hash associated with the shortened link.
     * @param limit The maximum number of redirects allowed in an hour.
     */
    fun addNewRedirect(hash: String, limite: Long)

    /**
     * Returns the total number of redirects.
     */
    fun obtainTotalNumberOfRedirects(): Int

    /**
     * Returns the number of redirects for a given hash.
     *
     * @param hash The hash associated with the shortened link.
     */
    fun obtainNumberOfRedirectsByHash(hash: String): Int

    // Función para buscar un elemento en la lista por hash
    fun findRedirectByHash(hashActual: String): Boolean

    // Función para buscar un elemento en la lista por hash
    fun obtainRedirectByHash(hashActual: String): Redirects?

}

/**
 * Data class representing information about the redirects of a given url
 * @property hash Hash associated with the shortened link.
 * @property counter Number of redirects in the last hour.
 * @property limit Maximum number of redirects allowed in an hour (0 if no limit)
 **/

data class Redirects(
    val hash: String? = null,
    val bucket :Bucket,
    val limit: Long = 0,
)

/**
 * Implementation of the [RedirectLimitUseCase] interface.
 *
 * @property shortUrlRepository Service for accessing short URL-related data in the repository.
 */
class RedirectLimitUseCaseImpl: RedirectLimitUseCase {

    // Lista para almacenar redirecciones
    private val redirectsList = mutableListOf<Redirects>()

    private val counter: Counter = Counter.builder("total_redirecciones").
    tag("version", "v1").
    description("Numero total de redirecciones").
    register(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

    /**
     * Checks if a new redirection is allowed, and updates the total number of redirects if allowed.
     *
     * @param hash The hash associated with the shortened link.
     */
    override fun newRedirect(hash: String) : Boolean {
        println("Comprobando si la redirección es posible, con hash: $hash")
        val exists = findRedirectByHash(hash)
        var resultado = false

        println("Redirección existe: $exists")

        val redirect = obtainRedirectByHash(hash)
        if (!exists) {
            resultado = true
        }
        else if (redirect?.limit == 0L|| redirect?.bucket?.tryConsume(1) == true) {
                println("Redirección permitida, total de redirecciones: ${redirect.bucket.availableTokens}")
                counter.increment()
                resultado = true
        }
        else {
            println("Redirección no permitida")
        }
        return resultado
    }

    override fun obtainNumberOfRedirectsByHash(hash: String): Int {
        val redirect = obtainRedirectByHash(hash)
        val avaliableTokens = redirect?.bucket?.availableTokens
        val totalTokens = redirect?.limit
        if (totalTokens != null && totalTokens != 0L && avaliableTokens != null) {
                return totalTokens.toInt() - avaliableTokens.toInt()
        } else {
            return avaliableTokens?.toInt() ?: 0
        }
    }

    override fun obtainTotalNumberOfRedirects(): Int {
        return counter.count().toInt()
    }

    /**
     * Adds a new redirect to the list of redirects.
     *
     * @param hash The hash associated with the shortened link.
     * @param limit The maximum number of redirects allowed in an hour.
     */
    override fun `addNewRedirect`(hash: String, limite: Long) {

        var limit: Bandwidth = Bandwidth.builder()
            .capacity( if (limite == 0L) 1 else limite)
            .refillIntervally(if (limite == 0L) 1 else limite, Duration.ofMinutes(1))
            .build()

        val bucket: Bucket = Bucket.builder()
            .addLimit(limit)
            .build()

        // Añadimos el hash a la lista de redirecciones
        redirectsList.add(Redirects(hash, bucket, limite))
        println("Redirección añadida, con $limite redirecciones por hora")
    }

    // Función para buscar un elemento en la lista por hash
    override fun findRedirectByHash(hashActual: String): Boolean {
        // Mostrar el contenido completo de la lista
        return redirectsList.any { it.hash == hashActual }
    }

    // Función para buscar un elemento en la lista por hash
    override fun obtainRedirectByHash(hashActual: String): Redirects? {
        return redirectsList.find { it.hash == hashActual }
    }

}

