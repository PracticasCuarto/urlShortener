package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.usecases.QrUseCase
import es.unizar.urlshortener.core.usecases.QrUseCaseImpl
import org.springframework.amqp.rabbit.annotation.RabbitListener

interface ListenerQr {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName])
    fun receiveMessage(message: String) {}
}


class ListenerQrImpl (
    val qrUseCase: QrUseCase
) : ListenerQr {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName])
    override fun receiveMessage(message: String) {
        println("Received HEMOS CONSEGIUDO ENTRAR EN EL METODO LISTENER")

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        val hash = message.substringBefore(" ")
        val url = message.substringAfter(" ")
        println("Received <$hash>")
        println("Received <$url>")

        // Generamos el código QR
        val qrCode = qrUseCase.generateQRCode(url, hash)

    }


}