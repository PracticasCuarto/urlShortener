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
        return BindingBuilder.bind(queue).to(exchange).with("qr")
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
        return BindingBuilder.bind(queue2).to(exchange).with("reachable")
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

    @Bean
    fun listenerAdapter2(receiver2: ListenerReachableImpl?): MessageListenerAdapter {
        return MessageListenerAdapter(receiver2, "receiveMessage")
    }

    // COLA 3

    @Bean
    fun queue3(): Queue {
        return Queue(queueName3, false)
    }

    @Bean
    fun binding3(queue3: Queue?, exchange: TopicExchange?): Binding {
        return BindingBuilder.bind(queue3).to(exchange).with("db")
    }

    @Bean
    fun container3(
        connectionFactory: ConnectionFactory?,
        listenerAdapter3: MessageListenerAdapter?
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory!!
        container.setQueueNames(queueName3)
        container.setMessageListener(listenerAdapter3!!)
        return container
    }

    @Bean
    fun listenerAdapter3(receiver3: ListenerWriteDBImpl?): MessageListenerAdapter {
        return MessageListenerAdapter(receiver3, "receiveMessage")
    }


    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        return RabbitTemplate(connectionFactory)
    }

    // COLA 4

    @Bean
    fun queue4(): Queue {
        return Queue(queueName4, false)
    }

    @Bean
    fun binding4(queue4: Queue?, exchange: TopicExchange?): Binding {
        return BindingBuilder.bind(queue4).to(exchange).with("Metrics")
    }

    @Bean
    fun container4(
        connectionFactory: ConnectionFactory?,
        listenerAdapter4: MessageListenerAdapter?
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory!!
        container.setQueueNames(queueName4)
        container.setMessageListener(listenerAdapter4!!)
        return container
    }

    @Bean
    fun listenerAdapter4(receiver4: ListenerMetricsImpl?): MessageListenerAdapter {
        return MessageListenerAdapter(receiver4, "receiveMessage")
    }

    // COLA 5

    @Bean
    fun queue5(): Queue {
        return Queue(queueName5, false)
    }

    @Bean
    fun binding5(queue5: Queue?, exchange: TopicExchange?): Binding {
        return BindingBuilder.bind(queue5).to(exchange).with("Location")
    }

    @Bean
    fun container5(
        connectionFactory: ConnectionFactory?,
        listenerAdapter5: MessageListenerAdapter?
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory!!
        container.setQueueNames(queueName5)
        container.setMessageListener(listenerAdapter5!!)
        return container
    }

    @Bean
    fun listenerAdapter5(receiver5: ListenerLocationImpl?): MessageListenerAdapter {
        return MessageListenerAdapter(receiver5, "receiveMessage")
    }

    companion object {
        const val topicExchangeName = "spring-boot-exchange"
        const val queueName = "cola_1"
        const val queueName2 = "cola_2"
        const val queueName3 = "cola_3"
        const val queueName4 = "cola_4"
        const val queueName5 = "cola_5"
        @Throws(InterruptedException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MessagingRabbitmqApplication::class.java, *args).close()
        }
    }
}
