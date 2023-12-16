package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase

interface ListenerWriteDB {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}


class ListenerWriteDBImpl (
    val isUrlReachable: IsUrlReachableUseCase
) : ListenerQr {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    override fun receiveMessage(message: String) {
        println("Received message queque WriteDB")

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        val hash = message.substringBefore(" ")
        val state = message.substringAfter(" ")
        println("Received <$hash>")
        println("Received <$state>")

        // Escribimos en la base de datos el estado de la url
        isUrlReachable.setCodeStatus(hash, state.toInt())
    }
}
