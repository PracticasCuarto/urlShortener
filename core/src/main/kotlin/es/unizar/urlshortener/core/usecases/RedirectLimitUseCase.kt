/**
 * Package containing use cases related to the core functionality of URL shortener.
 */
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Use case interface for managing redirection limits and statistics.
 */
interface RedirectLimitUseCase {

    /**
     * Returns the allowed redirection limit for a given hash.
     *
     * @param hash The hash associated with the shortened link.
     * @return The allowed redirection limit.
     */
    fun obtainLimit(hash: String): Int

    /**
     * Returns the total number of redirects in the last hour for a given hash.
     *
     * @param hash The hash associated with the shortened link.
     * @return The total number of redirects in the last hour.
     */
    fun obtainNumRedirects(hash: String): Int

    /**
     * Checks if a new redirection is allowed, and updates the total number of redirects if allowed.
     *
     * @param hash The hash associated with the shortened link.
     */
    fun newRedirect(hash: String)

    /**
     * Resets the amount of redirects for a given hash in the last hour.
     *
     * @param hash The hash associated with the shortened link.
     */
    fun reiniciarNumRedirecciones(hash: String)

    fun obtenerHora(hash: String): LocalDateTime?

    fun actualizarHora(hash: String, hora: LocalDateTime)
}

/**
 * Implementation of the [RedirectLimitUseCase] interface.
 *
 * @property shortUrlRepository Service for accessing short URL-related data in the repository.
 */
class RedirectLimitUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectLimitUseCase {

    /**
     * Returns the allowed redirection limit for a given hash.
     *
     * @param hash The hash associated with the shortened link.
     * @return The allowed redirection limit.
     */
    override fun obtainLimit(hash: String): Int {
        return shortUrlRepository.obtainLimit(hash)
    }

    /**
     * Returns the total number of redirects in the last hour for a given hash.
     *
     * @param hash The hash associated with the shortened link.
     * @return The total number of redirects in the last hour.
     */
    override fun obtainNumRedirects(hash: String): Int {
        return shortUrlRepository.obtenerNumRedirecciones(hash)
    }

    /**
     * Checks if a new redirection is allowed, and updates the total number of redirects if allowed.
     *
     * @param hash The hash associated with the shortened link.
     */
    override fun newRedirect(hash: String) {
        val numRedirecciones: Int = shortUrlRepository.obtenerNumRedirecciones(hash)
        shortUrlRepository.actualizarNumRedirecciones(hash, numRedirecciones + 1)
    }

    override fun reiniciarNumRedirecciones(hash: String) {
        shortUrlRepository.reiniciarNumRedirecciones(hash)
    }

    override fun obtenerHora(hash: String): LocalDateTime? {
        return shortUrlRepository.obtenerHoraRedireccion(hash)
    }

    override fun actualizarHora(hash: String, hora: LocalDateTime) {
        shortUrlRepository.actualizarHoraRedireccion(hash, hora)
    }
}
