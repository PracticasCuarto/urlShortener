package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.rabbit.annotation.RabbitListener
import es.unizar.urlshortener.core.usecases.IsUrlReachableUseCase
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking

interface ListenerMetrics {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}


class ListenerMetricsImpl : ListenerMetrics {

//    private val httpClient = HttpClient {
//        install(JsonFeature)
//
//    }


    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName4])
    override fun receiveMessage(message: String) {
        println("Received message metrics")

        // Hacer una peticion POST a /api/update/metrics

//        val url = "http://localhost:8080/api/update/metrics"

//        runBlocking {
//            try {
//                val response = httpClient.post<String>(url) {
//                    contentType(ContentType.Application.Json)
//                    body = message
//                }
//
//                println("POST request successful. Response: $response")
//            } catch (e: Exception) {
//                println("POST request failed. Exception: $e")
//            }
//        }



        println("POST request successful. Response: ${message}")
    }
}
