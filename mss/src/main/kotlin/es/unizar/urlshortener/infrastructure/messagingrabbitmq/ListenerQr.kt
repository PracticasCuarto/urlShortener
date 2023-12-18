package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.usecases.QrUseCase
import org.springframework.amqp.rabbit.annotation.RabbitListener

interface ListenerQr {
    fun receiveMessage(message: String) {}
}


class ListenerQrImpl (
    val qrUseCase: QrUseCase
) : ListenerQr {
    override fun receiveMessage(message: String) {

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        val hash = message.substringBefore(" ")
        val url = message.substringAfter(" ")

        // Generamos el código QR
        qrUseCase.generateQRCode(url, hash)

    }


}
