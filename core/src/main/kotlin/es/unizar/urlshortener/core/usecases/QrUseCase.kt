package es.unizar.urlshortener.core.usecases


import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.InformationNotFound
import es.unizar.urlshortener.core.CalculandoException
import es.unizar.urlshortener.core.InvalidExist
import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import java.io.ByteArrayOutputStream
import java.io.File

const val QRCODEFOLDER: String = "../CodigosQr"

interface QrUseCase {
    fun generateQRCode(url: String, hash: String): Boolean
    fun getQrImageBytes(id: String): ByteArray?
    fun getCodeStatus(hash: String): Int
    fun getInfoForQr(id: String): QrInfo
}

data class QrInfo(
    val hayQr: Int,
    val alcanzable: Int,
    val imageBytes: ByteArray?
) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QrInfo

        if (hayQr != other.hayQr) return false
        if (alcanzable != other.alcanzable) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hayQr
        result = 31 * result + alcanzable
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}

class QrUseCaseImpl(
    private val shortUrlEntityRepository: ShortUrlRepositoryService,

) : QrUseCase {

    // Mapa para rastrear el estado de los códigos QR
    private val qrCodeStatusMap: MutableMap<String, Boolean> = mutableMapOf()

    override fun generateQRCode(url: String, hash: String): Boolean {
        val byteArrayOutputStream = ByteArrayOutputStream()
        QRCode.from(url).to(ImageType.PNG).writeTo(byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val fileName = "${hash}.png"
        val outputPath = File(QRCODEFOLDER, fileName).toString()

        // Marcar el código QR como "en proceso de creación"
        qrCodeStatusMap[hash] = true

        // AÑADIR EN LA BASE QUE QR ESTA EN PROCESO (2)
        shortUrlEntityRepository.updateHayQr(hash, 2)

        // Guardar el código QR en el archivo
        File(outputPath).writeBytes(byteArray)

        // Marcar el código QR como "creado"
        qrCodeStatusMap[hash] = false

        // Añadir en la base de datos QR creado (1)
        shortUrlEntityRepository.updateHayQr(hash, 1)

        return true
    }

    override fun getQrImageBytes(id: String): ByteArray? {

        val qrImagePath = File(QRCODEFOLDER, "$id.png")

        // Comprobar que existe el QR en la carpeta de QRs
        return if (qrImagePath.exists()) {
            qrImagePath.readBytes()
        } else {
            // No existe el QR
            null
        }
    }

    // Función la cual devuelve el estado en el que se encuentra el qr.
    // 0 no existe, 1 creado y 2 creandose.
    override fun getCodeStatus(hash: String): Int {
        qrCodeStatusMap.forEach { (key, value) ->
            println("$key: $value")
        }

        return shortUrlEntityRepository.obtainHayQr(hash)
    }

    // Devuelve la informacion del Qr de la base de datos y la imagen si es pertinente.
    override fun getInfoForQr(id: String): QrInfo {
        val hayQr = shortUrlEntityRepository.obtainHayQr(id)
        val alcanzable = shortUrlEntityRepository.obtainAlcanzable(id)
        val existeId = shortUrlEntityRepository.existe(id)
        return when {
            !existeId ->
                throw InformationNotFound("El id introducido no existe")
            hayQr == 2 || alcanzable == 2 ->
                // La URL corta existe, pero el código QR está en proceso de creación o
                // no sabemos si es alcanzable o no
                throw CalculandoException("Qr o URL en proceso de creacion")

            hayQr == 0 || alcanzable == 0->
                // QR o URL no son alcanzable o no se puede generar
                throw InvalidExist( "No se puede redirigir a esta URL corta")

            else -> {
                // Caso correcto devolviendo la imagen, hayQR y alcanzable
                val imageBytes = getQrImageBytes(id)
                QrInfo(hayQr, alcanzable, imageBytes)
            }
        }
    }
}
