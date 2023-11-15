package es.unizar.urlshortener.core.usecases


import org.springframework.boot.actuate.metrics.MetricsEndpoint

interface ReturnSystemInfoUseCase {
    fun returnSystemInfo(key: String): SystemInfo
}

/**
 * Data class that contains the info of the system.
 *
 */

data class SystemInfo (
    val memoryUsed: Double? = null,
    val upTime: Double? = null,
    val totalURL: Int? = null,
    val totalURLHour: Int? = null
)

const val BYTES = 1024
const val MILISECONDS = 1000
const val METRICAEJEMPLO = 5678

/**
 * Implementation of [ReturnSystemInfoUseCase].
 */

class ReturnSystemInfoUseCaseImpl(
    private val metricsEndpoint: MetricsEndpoint
) : ReturnSystemInfoUseCase {
    override fun returnSystemInfo(key: String): SystemInfo {

        // Obtener la métrica jvm.memory.used
        val usedMemoryMetrics = metricsEndpoint.metric("jvm.memory.used", null)
        val usedMemory = usedMemoryMetrics.measurements.firstOrNull()?.value
        val usedMemoryInMb = usedMemory?.div((BYTES * BYTES)) // Convertir bytes a megabytes

        // Obtener la métrica process.uptime
        val uptimeMetrics = metricsEndpoint.metric("process.uptime", null)
        val uptime = uptimeMetrics.measurements.firstOrNull()?.value
        val uptimeInSeconds = uptime?.div(MILISECONDS) // Convertir milisegundos a segundos

        // Obtener la métrica de cantidad total de URLs acortadas
        // De la base de datos (entities) tabla ShortUrlEntity
        val totalURLMetrics = METRICAEJEMPLO

        // Metrica para ver cuanta gente ha utilizado el servicio en la ultima hora


        return SystemInfo(usedMemoryInMb, uptimeInSeconds, totalURLMetrics)
    }
}

