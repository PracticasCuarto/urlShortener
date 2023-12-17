@file:Suppress("TooGenericExceptionCaught")

import es.unizar.urlshortener.infrastructure.messagingrabbitmq.MessagingRabbitmqApplication

import org.springframework.amqp.rabbit.annotation.RabbitListener
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

interface ListenerMetrics {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName2])
    fun receiveMessage(message: String) {}
}

class ListenerMetricsImpl : ListenerMetrics {
    @RabbitListener(queues = [MessagingRabbitmqApplication.queueName4])
    override fun receiveMessage(message: String) {

        try {
            // URL a la que se enviará la solicitud POST
            val url = URL("http://localhost:8080/api/update/metrics")

            // Abrir conexión
            val connection = url.openConnection() as HttpURLConnection

            try {
                // Configurar la conexión para la solicitud POST
                connection.requestMethod = "POST"
                connection.doOutput = true

                // Establecer el tipo de contenido
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                // Datos que se enviarán en el cuerpo de la solicitud
                val postData = "key1=value1&key2=value2" // Por ej
                val postDataBytes = postData.toByteArray(Charsets.UTF_8)

                // Escribir los datos en el cuerpo de la solicitud
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(postDataBytes)
                outputStream.flush()

                // Obtener el código de respuesta
                connection.responseCode
            } finally {
                connection.disconnect() // Asegurarse de cerrar la conexión
            }

        } catch (e: Exception) {
            // Manejo de excepciones detallado
            println("Error making POST request: ${e.message}")
        }
    }
}
