package com.example

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.Test

class QrDimensionTest {
    @Test
    fun testQrDimensions() {
        val qris = "00020101021226590014ID.CO.QRIS.WWW01189360091100223000160215ID10200234567890303UME51440014ID.CO.QRIS.WWW0215ID10200234567890303UME5204541153033605405500005802ID5914ALI JAYA STORE6007JAKARTA61051011062070703A016304A1B2"
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 4,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(qris, BarcodeFormat.QR_CODE, 0, 0, hints)
        val moduleCount = matrix.width
        println("=== QR DIMENSION TEST ===")
        println("QRIS length: ${qris.length}")
        println("moduleCount (including margin 4): $moduleCount")
        for (dots in 3..6) {
            val totalDots = moduleCount * dots
            val totalBytes = (totalDots + 7) / 8
            println("dotsPerModule=$dots -> totalDots=$totalDots (${totalDots / 8.0} mm), qrBytesWidth=$totalBytes, rasterSize=${totalDots * 48} bytes, fits58mm=${totalDots <= 384}")
        }
    }

    @Test
    fun testEscPosRasterQrCanBeScanned() {
        val qris = "00020101021226590014ID.CO.QRIS.WWW01189360091100223000160215ID10200234567890303UME51440014ID.CO.QRIS.WWW0215ID10200234567890303UME5204541153033605405500005802ID5914ALI JAYA STORE6007JAKARTA61051011062070703A016304A1B2"
        val rasterBytes = com.example.printer.BluetoothThermalPrinter.generateEscPosRasterQr(qris, 32)
        org.junit.Assert.assertTrue("Raster bytes should not be empty", rasterBytes.isNotEmpty())

        // Header is 8 bytes: 1D 76 30 00 xL xH yL yH
        val xL = rasterBytes[4].toInt() and 0xFF
        val xH = rasterBytes[5].toInt() and 0xFF
        val yL = rasterBytes[6].toInt() and 0xFF
        val yH = rasterBytes[7].toInt() and 0xFF
        val widthBytes = xL + (xH shl 8)
        val heightDots = yL + (yH shl 8)
        val widthDots = widthBytes * 8

        println("Decoded raster header: widthDots=$widthDots, heightDots=$heightDots (xL=$xL, xH=$xH, yL=$yL, yH=$yH)")

        // Reconstruct binary bitmap array
        val luminances = ByteArray(widthDots * heightDots)
        var offset = 8
        for (y in 0 until heightDots) {
            for (xByte in 0 until widthBytes) {
                val b = rasterBytes[offset++].toInt() and 0xFF
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    val isBlack = (b and (1 shl (7 - bit))) != 0
                    luminances[y * widthDots + x] = if (isBlack) 0.toByte() else 255.toByte()
                }
            }
        }

        val source = com.google.zxing.RGBLuminanceSource(
            widthDots,
            heightDots,
            luminances.map { if (it.toInt() == 0) -0x1000000 else -0x1 }.toIntArray()
        )
        val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
        val reader = com.google.zxing.qrcode.QRCodeReader()
        val result = reader.decode(binaryBitmap)

        println("Scanned QR result: ${result.text}")
        org.junit.Assert.assertEquals("Scanned payload must match input QRIS", qris, result.text)
    }
}
