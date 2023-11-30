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

    override fun obtainLimit(hash: String): Long {
        return shortUrlEntityRepository.findByHash(hash)?.limit ?: 0L
    }

    override fun obtainAlcanzable(hash: String): Int {
        return shortUrlEntityRepository.findByHash(hash)?.alcanzable ?: 0
    }

    override fun obtainHayQr(hash: String): Int {
        return shortUrlEntityRepository.findByHash(hash)?.hayQr ?: 0
    }

    override fun updateAlcanzable(hash: String, alcanzable: Int) {
        shortUrlEntityRepository.findByHash(hash)?.apply {
            this.alcanzable = alcanzable
            shortUrlEntityRepository.save(this)
        }
    }

    override fun updateHayQr(hash: String, hayQr: Int) {
        shortUrlEntityRepository.findByHash(hash)?.apply {
            this.hayQr = hayQr
            shortUrlEntityRepository.save(this)
        }
    }

}

