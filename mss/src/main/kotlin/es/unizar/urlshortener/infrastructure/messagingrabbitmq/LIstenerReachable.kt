package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.RabbitMQSenderService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase

interface ListenerReachable {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}


class ListenerReachableImpl(
    val isUrlReachable: IsUrlReachableUseCase,
    val rabbitMQSender: RabbitMQSenderService
) : ListenerReachable {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    override fun receiveMessage(message: String) {
        println("Received message queque Reachable")

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        val hash = message.substringBefore(" ")
        val url = message.substringAfter(" ")
        println("Received <$hash>")
        println("Received <$url>")

        // Generamos el código QR
        val state = isUrlReachable.isUrlReachable(url, hash)
        val stateInt = if (state) 1 else 0
        // escribir en la cola para que escriba en la DB
        rabbitMQSender.sendThirdChannelMessage("$hash $stateInt")
    }


}
