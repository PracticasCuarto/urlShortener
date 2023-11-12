package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.InformationNotFound

/**
 * Given a key returns a [String] that contains the JSON info
 * of the users that have clicked the link.
 *
 */
interface ReturnInfoUseCase {
    fun returnInfo(key: String): List<Info>
}

/**
 * Data class that contains the info of the users that have clicked the link.
 *
 */
data class Info (
    val ip: String? = null,
    val os: String? = null,
    val browser: String? = null
)

/**
 * Implementation of [ReturnInfoUseCase].
 */
class ReturnInfoUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : ReturnInfoUseCase {
    override fun returnInfo(key: String): List<Info> {
        val clickList = clickRepository.findByHash(key)

        if (clickList.isEmpty()) {
            throw InformationNotFound("No information found for key: $key")
        }

        return clickList.map {
            Info(it.properties.ip, it.properties.os, it.properties.browser)
        }
    }
}

