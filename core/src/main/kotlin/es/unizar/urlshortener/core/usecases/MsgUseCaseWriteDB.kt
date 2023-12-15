package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RabbitMQSenderService

interface MsgUseCaseWriteDB {
    fun sendMsg(canal: String, msj: String)
}

class MsgUseCaseWriteDBImpl(
    private val rabbitMQSender: RabbitMQSenderService

) : MsgUseCase {
    override fun sendMsg(canal: String, msj: String) {
        println("Sending message to channel $canal...")
        rabbitMQSender.sendThirdChannelMessage(msj)
    }
}
