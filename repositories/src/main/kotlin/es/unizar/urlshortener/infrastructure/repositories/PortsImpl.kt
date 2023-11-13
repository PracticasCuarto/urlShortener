@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*

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

//    override fun actualizarNumRedirecciones(id: String, numRedirecciones: Int) {
//        // 1. Buscar la entidad de URL corta por su hash
//        val shortUrlEntity = shortUrlEntityRepository.findByHash(id)
//
//        // 2. Verificar si la entidad existe
//        if (shortUrlEntity != null) {
//            // 3. Actualizar el valor de numRedirecciones en la entidad
//            shortUrlEntity.numRedirecciones = numRedirecciones
//
//            // 4. Guardar la entidad actualizada en la base de datos
//            shortUrlEntityRepository.save(shortUrlEntity)
//        }
//    }

    override fun actualizarNumRedirecciones(id: String, _numRedirecciones: Int) {
        shortUrlEntityRepository.findByHash(id)?.apply {
            numRedirecciones = _numRedirecciones
            shortUrlEntityRepository.save(this)
        }
    }
}

