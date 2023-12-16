package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RabbitMQSenderService

interface MsgUseCaseReachable {
    fun sendMsg(canal: String, msj: String)
}

class MsgUseCaseReachableImpl(
    private val rabbitMQSender: RabbitMQSenderService
) : MsgUseCaseReachable {
    override fun sendMsg(canal: String, msj: String) {
        println("Sending message to channel $canal...")
        rabbitMQSender.sendSecondChannelMessage(msj)
    }
}
