package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import es.unizar.urlshortener.core.usecases.QrUseCase
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch

@Component
class Receiver {
    val latch = CountDownLatch(1)

    //@RabbitListener(queues = [MessagingRabbitmqApplication.queueName])
    fun receiveMessage(message: String) {
        println("Received <$message>")
        latch.countDown()

        // troceamos la entrada teniendo en cuenta el primer espacio para separar hash de url
        /*
        val hash = message.substringBefore(" ")
        val url = message.substringAfter(" ")
        println("Received <$hash>")
        println("Received <$url>")
        */
    }
}
