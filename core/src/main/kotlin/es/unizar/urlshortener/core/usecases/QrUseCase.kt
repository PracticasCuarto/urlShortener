@file:Suppress("WildcardImport","UnusedParameter","MagicNumber","MaxLineLength")
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
        // AÑADIR EN LA BASE QUE QR ESTA EN PROCESO (2)

        //val p1 = getCodeStatus(hash)
        //println("Valor antes durante el Qr: $p1")

        // Sleep de 5 segundos para simular el tiempo de creación del código QR
        //Thread.sleep(15000)

        // Guardar el código QR en el archivo
        File(outputPath).writeBytes(byteArray)

        // Marcar el código QR como "creado"
        qrCodeStatusMap[hash] = false
        // AÑADIR EN LA BASE QUE QR ESTA CREADO (1)

        return byteArray
    }

    override fun getQrImageBytes(id: String): ByteArray? {
        // EN VEZ DE EN LA CARPETA MIRAR EN LA BASE DE DATOS QUE HAY QR
        val qrImagePath = File(qrCodeFolder, "$id.png")

        // COMPROBAR QUE ES ALCANZABLE
        return if (qrImagePath.exists()) {
            qrImagePath.readBytes()
        } else {
            // No existe en la base de datos, devolverá una respuesta de tipo 404
            null
            // COMPROBAR TB LA RESPUESTA EN CASO DE QUE ESTE PENDIENTE
        }
    }

    override fun getCodeStatus(hash: String): Int {
        println("Valores actuales del mapa al entrar a getCodeStatus:")
        qrCodeStatusMap.forEach { (key, value) ->
            println("$key: $value")
        }

        return when {
            qrCodeStatusMap.containsKey(hash) && qrCodeStatusMap[hash] == true -> 1 // En proceso de creación
            qrCodeStatusMap.containsKey(hash) && qrCodeStatusMap[hash] == false -> 2 // Creado
            else -> 0 // No existe
        }
    }
}
