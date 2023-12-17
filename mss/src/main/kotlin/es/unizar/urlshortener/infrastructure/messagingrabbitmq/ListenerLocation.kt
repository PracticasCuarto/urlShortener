package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.usecases.LocationUseCase
import es.unizar.urlshortener.core.usecases.QrUseCase
import org.springframework.amqp.rabbit.annotation.RabbitListener

interface ListenerLocation {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName5])
    fun receiveMessage(message: String) {}
}


class ListenerLocationImpl (
    val locationUseCase: LocationUseCase
) : ListenerLocation {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName5])
    override fun receiveMessage(message: String) {
        println("Received procesando location <$message>")

        // troceamos la entrada teniendo en cuenta el primer espacio para separar valores

        val userAgent = message.substringBefore("||||||")
        val request = message.substringAfter("||||||")

        println("Received <$userAgent>")
        println("Received <$request>")

        // Generamos el código QR
//        locationUseCase.obtenerInformacionUsuario(userAgent, request)

    }


}
