package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.RabbitMQSenderService

interface MsgUseCaseUpdateMetrics {
    fun sendMsg(canal: String, msj: String)
}

class MsgUseCaseUpdateMetricsImpl(
    private val rabbitMQSender: RabbitMQSenderService

) : MsgUseCaseUpdateMetrics {
    override fun sendMsg(canal: String, msj: String) {
        rabbitMQSender.sendMetricsChannelMessage(msj)
    }
}
