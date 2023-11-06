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
    @Deprecated("Lo vamos a quitar, no nos gusta")
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
        if (info.browser != "null" || info.os != "null") {
            // Guardar el resumen de la redirección
            redirectSummaryRepository.save(info)
            println("Almacenado en la base")
        }
        return redirection
    }

    override fun redirectTo(key: String) = shortUrlRepository
        .findByKey(key)
        ?.redirection
        ?: throw RedirectionNotFound(key)
}



