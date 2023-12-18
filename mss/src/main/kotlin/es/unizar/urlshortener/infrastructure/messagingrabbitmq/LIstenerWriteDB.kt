package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase

interface ListenerWriteDB {
    fun receiveMessage(message: String) {}
}


class ListenerWriteDBImpl (
    val isUrlReachable: IsUrlReachableUseCase
) : ListenerWriteDB {
    override fun receiveMessage(message: String) {

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash del estado
        val hash = message.substringBefore(" ")
        val state = message.substringAfter(" ")

        // Escribimos en la base de datos el estado de la url
        isUrlReachable.setCodeStatus(hash, state.toInt())
    }
}
