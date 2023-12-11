@file:Suppress("SpreadOperator", "TooManyFunctions", )

package es.unizar.urlshortener.infrastructure.messagingrabbitmq

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class MessagingRabbitmqApplication {
    @Bean
    fun queue(): Queue {
        return Queue(queueName, false)
    }


    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(topicExchangeName)
    }

    @Bean
    fun binding(queue: Queue?, exchange: TopicExchange?): Binding {
        return BindingBuilder.bind(queue).to(exchange).with("alcanzable")
    }

    @Bean
    fun container(
        connectionFactory: ConnectionFactory?,
        listenerAdapter: MessageListenerAdapter?
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory!!
        container.setQueueNames(queueName)      //de que cola escucha
        container.setMessageListener(listenerAdapter!!)
        return container
    }

    @Bean
    fun listenerAdapter(receiver: ListenerQrImpl?): MessageListenerAdapter {
        return MessageListenerAdapter(receiver, "receiveMessage")
    }

    // COLA 2

    @Bean
    fun queue2(): Queue {
        return Queue(queueName2, false)
    }

    @Bean
    fun binding2(queue2: Queue?, exchange: TopicExchange?): Binding {
        return BindingBuilder.bind(queue2).to(exchange).with("Qr")
    }

    @Bean
    fun container2(
        connectionFactory: ConnectionFactory?,
        listenerAdapter2: MessageListenerAdapter?
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory!!
        container.setQueueNames(queueName2)
        container.setMessageListener(listenerAdapter2!!)
        return container
    }

    //@Bean
    //fun listenerAdapter2(receiver2: Receiver2?): MessageListenerAdapter {
    //    return MessageListenerAdapter(receiver2, "receiveMessage")
    //}

    @Bean
    fun listenerAdapter2(receiver2: ListenerReachableImpl?): MessageListenerAdapter {
        return MessageListenerAdapter(receiver2, "receiveMessage")
    }


    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        return RabbitTemplate(connectionFactory)
    }

    companion object {
        const val topicExchangeName = "spring-boot-exchange"
        const val queueName = "cola_1"
        const val queueName2 = "cola_2"
        @Throws(InterruptedException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MessagingRabbitmqApplication::class.java, *args).close()
        }
    }
}
