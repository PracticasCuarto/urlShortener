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

    override fun sendFirstChannelMessage(message: String) {
        sendMessage("alcanzable", message)
    }

    override fun sendSecondChannelMessage(message: String) {
        sendMessage("Qr", message)
    }

    override fun sendThirdChannelMessage(message: String) {
        sendMessage("DB", message)
    }

    override fun sendMetricsChannelMessage(message: String) {
        sendMessage("Metrics", message)
    }

    override fun sendLocationChannelMessage(message: String) {
        sendMessage("Location", message)
    }

    private fun sendMessage(channel: String, message: String) {
        println("Sending message to channel $channel...")
        rabbitTemplate.convertAndSend(
            MessagingRabbitmqApplication.topicExchangeName,
            channel,
            message
        )
    }
}
