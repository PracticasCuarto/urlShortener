package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

interface ListenerMetrics {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}


class ListenerMetricsImpl : ListenerMetrics {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName4])
    override fun receiveMessage(message: String) {
        println("Received message metrics")

        // Hacer una peticion POST a /api/update/metrics

        val url = "http://localhost:8080/api/update/metrics"

//        val (_, response, result) = url
//            .httpPost()
//            .body(message)
//            .responseString()
//
//        when (result) {
//            is Result.Success -> {
//                println("POST request successful. Response: ${response.body().asString("application/json")}")
//            }
//            is Result.Failure -> {
//                println("POST request failed. Error: ${result.error}")
//            }
//        }

        println("POST request successful. Response: ${message}")
    }
}
