@file:Suppress("WildcardImport", "ForbiddenComment", "NewLineAtEndOfFile","TooManyFunctions",
    "UnusedPrivateProperty")

package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.RabbitMQSenderService
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

//@Component("customRabbitMQSender")
class RabbitMQSenderImpl(
    private val rabbitTemplate: RabbitTemplate
) : RabbitMQSenderService {

    override fun qrChannelMessage(message: String) {
        sendMessage("qr", message)
    }

    override fun reachableChannelMessage(message: String) {
        sendMessage("reachable", message)
    }

    override fun writeDBChannelMessage(message: String) {
        sendMessage("db", message)
    }

    override fun sendMetricsChannelMessage(message: String) {
        sendMessage("Metrics", message)
    }

    override fun sendLocationChannelMessage(message: String) {
        sendMessage("Location", message)
    }

    private fun sendMessage(channel: String, message: String) {
        rabbitTemplate.convertAndSend(
            MessagingRabbitmqApplication.topicExchangeName,
            channel,
            message
        )
    }
}
