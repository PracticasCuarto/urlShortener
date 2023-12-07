/**
 * Package containing use cases related to the core functionality of URL shortener.
 */
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.InformationNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Use case interface for returning information about users who have clicked a shortened link.
 */
interface ReturnInfoUseCase {

    /**
     * Given a key, returns a list of [Info] objects containing information about users who clicked the link.
     *
     * @param key The key associated with the shortened link.
     * @return A list of [Info] objects representing user click information.
     * @throws InformationNotFound if no information is found for the given key.
     */
    fun returnInfo(key: String): InfoHash
}

/**
 * Data class representing information about users who have clicked the link.
 *
 * @property ip IP address of the user.
 * @property os Operating system of the user.
 * @property browser Web browser used by the user.
 */
data class Info(
    val ip: String? = null,
    val os: String? = null,
    val browser: String? = null
)

data class InfoHash(
    val limit: Long? = null,
    var numRedirecciones: Int? = null,
    var lista: List<Info>? = null
)

/**
 * Implementation of the [ReturnInfoUseCase] interface.
 *
 * @property clickRepository Service for accessing click-related data in the repository.
 */
class ReturnInfoUseCaseImpl(
    private val clickRepository: ClickRepositoryService,
    private val shortUrlRepository: ShortUrlRepositoryService
) : ReturnInfoUseCase {

    /**
     * Given a key, returns a list of [Info] objects containing information about users who clicked the link.
     *
     * @param key The key associated with the shortened link.
     * @return A list of [Info] objects representing user click information.
     * @throws InformationNotFound if no information is found for the given key.
     */
    override fun returnInfo(key: String): InfoHash {
        val clickList = clickRepository.findByHash(key)

        if (clickList.isEmpty()) {
            throw InformationNotFound("No information found for key: $key")
        }

        val listInfo =  clickList.map {
            Info(it.properties.ip, it.properties.os, it.properties.browser)
        }

        val redirecciones = clickList.size
        val limite = shortUrlRepository.obtainLimit(key)

        return InfoHash(limite, redirecciones, listInfo)
    }
}
