package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RabbitMQSenderService

interface MsgUseCase {
    fun sendMsg(canal: String, msj: String)
}

class MsgUseCaseImpl(
    private val rabbitMQSender: RabbitMQSenderService

) : MsgUseCase {
    override fun sendMsg(canal: String, msj: String) {
        println("Sending message to channel $canal...")
        rabbitMQSender.sendFirstChannelMessage(msj)
        //rabbitMQSender.sendSecondChannelMessage(msj)

    }
}
