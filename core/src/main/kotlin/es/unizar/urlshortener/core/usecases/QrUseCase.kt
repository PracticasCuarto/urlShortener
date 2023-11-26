@file:Suppress("WildcardImport","UnusedParameter","MagicNumber")
package es.unizar.urlshortener.core.usecases

import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import java.io.ByteArrayOutputStream
import java.io.File

interface QrUseCase {
    fun generateQRCode(url: String, hash: String): ByteArray
    fun getQrImageBytes(id: String): ByteArray?
    fun getCodeStatus(hash: String): Int
}

class QrUseCaseImpl(private val qrCodeFolder: String = "../CodigosQr") : QrUseCase {

    // Mapa para rastrear el estado de los códigos QR
    private val qrCodeStatusMap: MutableMap<String, Boolean> = mutableMapOf()

    override fun generateQRCode(url: String, hash: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        QRCode.from(url).to(ImageType.PNG).writeTo(byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val fileName = "${hash}.png"
        val outputPath = File(qrCodeFolder, fileName).toString()

        // Marcar el código QR como "en proceso de creación"
        qrCodeStatusMap[hash] = true

        val p1 = getCodeStatus(hash)
        println("Valor antes durante el Qr: $p1")

        // Guardar el código QR en el archivo
        File(outputPath).writeBytes(byteArray)

        // Marcar el código QR como "creado"
        qrCodeStatusMap[hash] = false

        return byteArray
    }

    override fun getQrImageBytes(id: String): ByteArray? {
        val qrImagePath = File(qrCodeFolder, "$id.png")

        return if (qrImagePath.exists()) {
            qrImagePath.readBytes()
        } else {
            // No existe en la base de datos, devolverá una respuesta de tipo 404
            null
        }
    }

    override fun getCodeStatus(hash: String): Int {
        return when {
            qrCodeStatusMap.containsKey(hash) && qrCodeStatusMap[hash] == true -> 1 // En proceso de creación
            qrCodeStatusMap.containsKey(hash) && qrCodeStatusMap[hash] == false -> 2 // Creado
            else -> 0 // No existe
        }
    }
}
