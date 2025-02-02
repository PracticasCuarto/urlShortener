@file:Suppress("WildcardImport", "ForbiddenComment", "TooManyFunctions")

package es.unizar.urlshortener

import ListenerMetricsImpl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.messagingrabbitmq.*
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring B,oot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired val metricsEndpoint: MetricsEndpoint,
    @Autowired val rabbitmqSender: RabbitTemplate
) {
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun returnInfoUseCase() = ReturnInfoUseCaseImpl(clickRepositoryService(), shortUrlRepositoryService(),
        redirectLimitUseCase())

    @Bean
    fun redirectLimitUseCase() = RedirectLimitUseCaseImpl()

    @Bean
    fun returnSystemInfoUseCase() = ReturnSystemInfoUseCaseImpl(metricsEndpoint, clickRepositoryService())

    @Bean
    fun isUrlReachableUseCase(shortUrlRepositoryService: ShortUrlRepositoryService) =
        IsUrlReachableUseCaseImpl(shortUrlEntityRepository = shortUrlRepositoryService)

    @Bean
    fun qrUseCase() = QrUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun locationUseCase() = LocationUseCaseImpl(logClickUseCase())

    @Bean
    fun rabbitMQSenderService() = RabbitMQSenderImpl(rabbitmqSender)

    @Bean
    fun listenerQr() = ListenerQrImpl(qrUseCase())

    @Bean
    fun listenerReachable() = ListenerReachableImpl(isUrlReachableUseCase(shortUrlRepositoryService()),
        rabbitMQSenderService())

    @Bean
    fun listenerWriteDB() = ListenerWriteDBImpl(isUrlReachableUseCase(shortUrlRepositoryService()))

    @Bean
    fun listenerMetrics() = ListenerMetricsImpl()

    @Bean
    fun listenerLocation() = ListenerLocationImpl(locationUseCase())

}
