package es.unizar.urlshortener.core

import java.time.LocalDateTime

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click

    fun findByHash(hash: String): List<Click>


}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl

    fun obtenerNumRedirecciones(id: String): Int

    fun actualizarNumRedirecciones(id: String, numeroRedirecciones: Int)

    fun reiniciarNumRedirecciones(id: String)

    fun obtainLimit(hash: String): Int

    fun obtenerHoraRedireccion(id: String): LocalDateTime?

    fun actualizarHoraRedireccion(id: String, horaRedireccionActual: LocalDateTime)
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}
