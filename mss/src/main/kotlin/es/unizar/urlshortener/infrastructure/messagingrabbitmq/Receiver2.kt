package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch

@Component
class Receiver2 {
    val latch = CountDownLatch(1)

    //@RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {
        println("Received DOS <$message>")
        latch.countDown()
    }
}
