package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase
import es.unizar.urlshortener.core.usecases.MsgUseCaseWriteDBImpl

interface ListenerReachable {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}


class ListenerReachableImpl(
    val isUrlReachable: IsUrlReachableUseCase,
    val msgUseCaseWriteDB: MsgUseCaseWriteDBImpl
) : ListenerQr {
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
        msgUseCaseWriteDB.sendMsg(MessagingRabbitmqApplication.queueName3, "$hash $stateInt")
    }


}
