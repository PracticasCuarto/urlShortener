package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RabbitMQSenderService

interface MsgUseCaseLocation {
    fun sendMsg(canal: String, msj: String)
}

class MsgUseCaseLocationImpl(
    private val rabbitMQSender: RabbitMQSenderService

) : MsgUseCaseLocation {
    override fun sendMsg(canal: String, msj: String) {
        println("Sending message to channel $canal...")
        rabbitMQSender.sendLocationChannelMessage(msj)
    }
}
