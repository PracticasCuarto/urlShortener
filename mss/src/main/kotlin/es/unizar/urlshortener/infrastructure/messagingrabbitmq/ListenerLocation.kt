package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.usecases.LocationUseCase
import es.unizar.urlshortener.core.usecases.QrUseCase
import org.springframework.amqp.rabbit.annotation.RabbitListener

const val PARTES = 3

interface ListenerLocation {
    fun receiveMessage(message: String) {}
}


class ListenerLocationImpl (
    val locationUseCase: LocationUseCase
) : ListenerLocation {
    override fun receiveMessage(message: String) {

        val parts = message.split("||||||")

        if (parts.size == PARTES) {
            val id = parts[0]
            val userAgent = parts[1]
            val request = parts[2]

            // Generamos el código QR
            locationUseCase.obtenerInformacionUsuario(userAgent, request, id)
        } else {
            println("El formato del mensaje no es válido.")
        }

    }


}
