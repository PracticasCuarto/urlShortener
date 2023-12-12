package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase

interface ListenerReachable {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}


class ListenerReachableImpl (
    val isUrlReachable: IsUrlReachableUseCase
) : ListenerQr {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    override fun receiveMessage(message: String) {
        println("Received HEMOS CONSEGIUDO ENTRAR EN EL METODO LISTENER")

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        val hash = message.substringBefore(" ")
        val url = message.substringAfter(" ")
        println("Received <$hash>")
        println("Received <$url>")

        // Generamos el código QR
        isUrlReachable.isUrlReachable(url, hash)

    }


}
