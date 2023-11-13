package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.InformationNotFound

/**
 * Given a key returns a [String] that contains the JSON info
 * of the users that have clicked the link.
 *
 */
interface RedirectLimitUseCase {

    // Devuelve el limite de redirecciones permitido para un hash
    fun obtainLimit(hash: String): Int

    // Devuelve el numero total de redirecciones en la ultima hora para un hash
    fun obtainNumRedirects(hash: String): Int

    // Devuelve true si se permite una nueva redireccion (y se actualiza el total de redirecciones)
    fun newRedirect(hash: String): Boolean
}

/**
 * Implementation of [ReturnInfoUseCase].
 */
class RedirectLimitUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectLimitUseCase {

    // Devuelve el limite de redirecciones permitido para un hash
    override fun obtainLimit(hash: String): Int {
        val shortUrl = shortUrlRepository.findByKey(hash)
        return shortUrl?.properties?.limit ?: 0
    }

    // Devuelve el numero total de redirecciones en la ultima hora para un hash
    override fun obtainNumRedirects(hash: String): Int {
        return shortUrlRepository.obtenerNumRedirecciones(hash)
    }

    // Devuelve true si se permite una nueva redireccion (y se actualiza el total de redirecciones)
    override fun newRedirect(hash: String): Boolean {
        val shortUrl = shortUrlRepository.findByKey(hash)
        val limit = shortUrl?.properties?.limit ?: 0
        val numRedirecciones = shortUrl?.properties?.numRedirecciones ?: 0

        if (numRedirecciones < limit) {
            shortUrlRepository.actualizarNumRedirecciones(hash, numRedirecciones + 1)
            return true
        }
        return false
    }


}

