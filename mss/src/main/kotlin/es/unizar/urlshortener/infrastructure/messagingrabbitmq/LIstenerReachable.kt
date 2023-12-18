package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.RabbitMQSenderService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase

interface ListenerReachable {
//    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}


class ListenerReachableImpl(
    var isUrlReachable: IsUrlReachableUseCase,
    val rabbitMQSender: RabbitMQSenderService
) : ListenerReachable {
//    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    override fun receiveMessage(message: String) {

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        val hash = message.substringBefore(" ")
        val url = message.substringAfter(" ")

        // comprobamos si la url es alcanzable, en ese caso 1, en caso contrario 0
        // hay que traducir el true o false que se devuelve a 1 o 0
        val state = isUrlReachable.isUrlReachable(url, hash)
        val stateInt = if (state) 1 else 0
        // escribir en la cola para que escriba en la base de datos
        rabbitMQSender.writeDBChannelMessage("$hash $stateInt")
    }


}
