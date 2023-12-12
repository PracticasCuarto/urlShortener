package es.unizar.urlshortener.core.usecases


import es.unizar.urlshortener.core.ClickRepositoryService
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

/**
 * Interface that represents the use case that returns the system info.
 *
 */
interface ReturnSystemInfoUseCase {
    /**
     * Returns the system info.
     * @param key the key of the URL.
     *
     * @return the system info.
     */
    fun returnSystemInfo(key: String): SystemInfo

    /**
     * Updates the system info every minute.
     */
    fun updateSystemInfo()
}

/**
 * Data class that contains the info of the system.
 *
 */

data class SystemInfo (
    val memoryUsed: Double? = null,
    val upTime: Double? = null,
    val totalRedirecciones: Int? = null,
    val totalRedireccionesHash: Int? = null
)

const val BYTES = 1024
const val MILISECONDS = 1000

/**
 * Implementation of [ReturnSystemInfoUseCase].
 */
@EnableScheduling
class ReturnSystemInfoUseCaseImpl(
    private val metricsEndpoint: MetricsEndpoint,
    private val clickRepository: ClickRepositoryService

) : ReturnSystemInfoUseCase {

    private var totalRedirecciones: Int = 0
    private var totalRedireccionesHash: Int = 0
    private var usedMemoryInMb: Double = 0.0
    private var uptimeInSeconds: Double = 0.0

    // Función que actualiza periódicamente la información del sistema
    @Scheduled(fixedRate = 6000)
    override fun updateSystemInfo() {
        // Obtener la métrica jvm.memory.used
        val usedMemoryMetrics = metricsEndpoint.metric("jvm.memory.used", null)
        val usedMemory = usedMemoryMetrics.measurements.firstOrNull()?.value
        usedMemoryInMb = usedMemory?.div((BYTES * BYTES))!! // Convertir bytes a megabytes

        // Obtener la métrica process.uptime
        val uptimeMetrics = metricsEndpoint.metric("process.uptime", null)
        val uptime = uptimeMetrics.measurements.firstOrNull()?.value
        uptimeInSeconds = uptime?.div(MILISECONDS)!! // Convertir milisegundos a segundos

        // Obtener la cantidad total de URLs acortadas solicitadas en la ultima hora
        totalRedirecciones = clickRepository.obtainNumClicks()

        println("usedMemoryInMb: $usedMemoryInMb")
        println("uptimeInSeconds: $uptimeInSeconds")
        println("totalURLsolicitadas: $totalRedirecciones")

    }

    override fun returnSystemInfo(key: String): SystemInfo {
        // printear todas las variables

        // Metrica numero de veces que se ha hecho click en la URL
        totalRedireccionesHash = clickRepository.obtainNumClicks(key)

        println("totalURLsolicitadas: $totalRedireccionesHash")

        return SystemInfo(usedMemoryInMb, uptimeInSeconds, totalRedirecciones, totalRedireccionesHash)
    }
}

