package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.usecases.QrUseCase
import org.springframework.amqp.rabbit.annotation.RabbitListener

interface ListenerQr {
//    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName])
    fun receiveMessage(message: String) {}
}


class ListenerQrImpl (
    val qrUseCase: QrUseCase
) : ListenerQr {
//    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName])
    override fun receiveMessage(message: String) {

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        val hash = message.substringBefore(" ")
        val url = message.substringAfter(" ")
        println("Received <$hash>")
        println("Received <$url>")

        // Generamos el código QR
        qrUseCase.generateQRCode(url, hash)

    }


}
