@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    override fun findByHash(hash: String): List<Click> = clickEntityRepository.findByHash(hash).map { it.toDomain() }

}

    /**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    override fun obtainLimit(hash: String): Int {
        return shortUrlEntityRepository.findByHash(hash)?.limit ?: 0
    }

    override fun obtenerNumRedirecciones(id: String): Int = shortUrlEntityRepository.findByHash(id)?.
    toDomain()?.properties?.numRedirecciones ?: 0

    override fun actualizarNumRedirecciones(id: String, numeroRedirecciones: Int) {
        shortUrlEntityRepository.findByHash(id)?.apply {
            numRedirecciones = numeroRedirecciones
            shortUrlEntityRepository.save(this)
        }
    }

    override fun reiniciarNumRedirecciones(id: String) {
        shortUrlEntityRepository.findByHash(id)?.apply {
            numRedirecciones = 0
            shortUrlEntityRepository.save(this)
        }
    }

    override fun obtenerHoraRedireccion(id: String): LocalDateTime? {
        return shortUrlEntityRepository.findByHash(id)?.horaRedireccion
    }

    override fun actualizarHoraRedireccion(id: String, horaRedireccionActual: LocalDateTime) {
        shortUrlEntityRepository.findByHash(id)?.apply {
            horaRedireccion = horaRedireccionActual
            shortUrlEntityRepository.save(this)
        }
    }

}

