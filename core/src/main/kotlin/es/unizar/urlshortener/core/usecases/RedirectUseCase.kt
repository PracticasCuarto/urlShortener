@file:Suppress("UnusedPrivateProperty")


package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RedirectSummary
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.RedirectSummaryRepositoryService


/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String, info: RedirectSummary): Redirection
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val redirectSummaryRepository: RedirectSummaryRepositoryService
) : RedirectUseCase {
    override fun redirectTo(key: String, info: RedirectSummary): Redirection {
        val shortUrl = shortUrlRepository.findByKey(key)
        val redirection : Redirection
        if (shortUrl != null) {
            // Realizar la redirección
            redirection = shortUrl.redirection
        } else {
            throw RedirectionNotFound(key)
        }

        // Guardar el resumen de la redirección
        //redirectSummaryRepository.save(info)
        println("RedirectSummaryRepository.save(info) called")
        return redirection
    }

    override fun redirectTo(key: String) = shortUrlRepository
        .findByKey(key)
        ?.redirection
        ?: throw RedirectionNotFound(key)
}



