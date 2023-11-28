@file:Suppress("WildcardImport","UnusedParameter","MagicNumber","MaxLineLength")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import java.io.ByteArrayOutputStream
import java.io.File

const val QRCODEFOLDER: String = "../CodigosQr"

interface QrUseCase {
    fun generateQRCode(url: String, hash: String): ByteArray
    fun getQrImageBytes(id: String): ByteArray?
    fun getCodeStatus(hash: String): Int
    fun getInfoForQr(id: String): QrInfo
}

data class QrInfo(
    val hayQr: Int,
    val alcanzable: Int,
    val imageBytes: ByteArray?
) {

    //hacer un equals y un hashcode como en ia (recomendacion de IntelIJ)
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

    override fun generateQRCode(url: String, hash: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        QRCode.from(url).to(ImageType.PNG).writeTo(byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val fileName = "${hash}.png"
        val outputPath = File(QRCODEFOLDER, fileName).toString()

        // Marcar el código QR como "en proceso de creación"
        qrCodeStatusMap[hash] = true

        // AÑADIR EN LA BASE QUE QR ESTA EN PROCESO (2)
        shortUrlEntityRepository.updateHayQr(hash, 2)


        //val p1 = getCodeStatus(hash)
        //println("Valor antes durante el Qr: $p1")

        // Sleep de 5 segundos para simular el tiempo de creación del código QR
        //Thread.sleep(15000)

        // Guardar el código QR en el archivo
        File(outputPath).writeBytes(byteArray)

        // Marcar el código QR como "creado"
        qrCodeStatusMap[hash] = false

        // AÑADIR EN LA BASE QUE QR ESTA CREADO (1)
        shortUrlEntityRepository.updateHayQr(hash, 1)

        return byteArray
    }

    override fun getQrImageBytes(id: String): ByteArray? {

        val qrImagePath = File(QRCODEFOLDER, "$id.png")

        // COMPROBAR QUE ES ALCANZABLE
        return if (qrImagePath.exists()) {
            qrImagePath.readBytes()
        } else {
            // No existe en la base de datos, devolverá una respuesta de tipo 404
            null
            // COMPROBAR TB LA RESPUESTA EN CASO DE QUE ESTE PENDIENTE
        }
    }

    // Función la cual devuelve el estado en el que se encuentra el qr. 0 no existe, 1 creado y 2 creandose.
    override fun getCodeStatus(hash: String): Int {
        println("Valores actuales del mapa al entrar a getCodeStatus:")
        qrCodeStatusMap.forEach { (key, value) ->
            println("$key: $value")
        }

        return shortUrlEntityRepository.obtainHayQr(hash)
    }

    // Devuelve la informacion del Qr de la base de datos y la imagen si es pertinente.
    override fun getInfoForQr(id: String): QrInfo {
        val hayQr = shortUrlEntityRepository.obtainHayQr(id)
        val alcanzable = shortUrlEntityRepository.obtainAlcanzable(id)
        return when {
            // PARA QUE FUNCIONE DE MOMENTO ALCANZABLE A 0 PORQUE NADIE LO MODIFICA !!!!!!!!!!!!!!!!!!!!!!!!!
            hayQr == 1 && alcanzable == 1 -> {
                // La URL corta existe, es redireccionable y tiene un código QR
                val imageBytes = getQrImageBytes(id)
                QrInfo(hayQr, alcanzable, imageBytes)
            }
            hayQr == 2 -> {
                // La URL corta existe, pero el código QR está en proceso de creación
                QrInfo(hayQr, alcanzable, null)
            }
            else -> {
                // Otros casos como no redireccionable, no operativa, spam, etc.
                QrInfo(hayQr, alcanzable, null)
            }
        }
    }
}
