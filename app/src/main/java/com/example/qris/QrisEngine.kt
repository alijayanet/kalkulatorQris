package com.example.qris

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.text.NumberFormat
import java.util.Locale

data class MerchantInfo(
    val merchantName: String = "TOKO QRIS MANDIRI",
    val merchantCity: String = "JAKARTA",
    val postalCode: String = "10110",
    val nmid: String = "ID1020304050607",
    val mpan: String = "936001234567890",
    val rawPayload: String = "",
    val isDynamic: Boolean = false
)

object QrisEngine {

    /**
     * Default Indonesian National QRIS Template for demonstration & instant startup.
     */
    const val DEFAULT_STATIC_QRIS =
        "00020101021126600016ID.CO.QRIS.WWW01189360012345678901230215ID10203040506070303UME51440014ID.CO.QRIS.WWW0215ID10203040506070303UME5204541153033605802ID5918TOKO QRIS MANDIRI6007JAKARTA61051011062070703A016304"

    /**
     * Calculates CRC16-CCITT (polynomial 0x1021, init 0xFFFF) in uppercase 4-digit hex.
     */
    fun calculateCrc16(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021

        val bytes = data.toByteArray(Charsets.ISO_8859_1)
        for (b in bytes) {
            for (i in 0..7) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.US, "%04X", crc)
    }

    /**
     * Parses standard EMVCo TLV string into map of tags.
     */
    fun parseTlv(payload: String): Map<String, String> {
        val tlvMap = mutableMapOf<String, String>()
        var index = 0
        val length = payload.length

        while (index + 4 <= length) {
            try {
                val tag = payload.substring(index, index + 2)
                val lenStr = payload.substring(index + 2, index + 4)
                val len = lenStr.toInt()
                val valueStart = index + 4
                val valueEnd = valueStart + len
                if (valueEnd > length) break
                val value = payload.substring(valueStart, valueEnd)
                tlvMap[tag] = value
                index = valueEnd
            } catch (e: Exception) {
                break
            }
        }
        return tlvMap
    }

    /**
     * Extracts merchant info from raw QRIS string.
     */
    fun parseMerchantInfo(payload: String): MerchantInfo {
        if (payload.isBlank()) return MerchantInfo()
        val tags = parseTlv(payload)
        val name = tags["59"] ?: "TOKO QRIS MANDIRI"
        val city = tags["60"] ?: "JAKARTA"
        val postal = tags["61"] ?: "10110"
        val isDynamic = tags["01"] == "12"

        // Search for NMID inside merchant tags 26-51
        var foundNmid = "ID1020304050607"
        var foundMpan = "936001234567890"

        for (tagKey in listOf("26", "27", "28", "29", "30", "51")) {
            val subData = tags[tagKey]
            if (subData != null) {
                val subTags = parseTlv(subData)
                if (subTags["02"] != null) foundNmid = subTags["02"]!!
                if (subTags["01"] != null) foundMpan = subTags["01"]!!
            }
        }

        return MerchantInfo(
            merchantName = name,
            merchantCity = city,
            postalCode = postal,
            nmid = foundNmid,
            mpan = foundMpan,
            rawPayload = payload,
            isDynamic = isDynamic
        )
    }

    /**
     * Converts any static QRIS into a standard dynamic QRIS containing the specified amount.
     */
    fun generateDynamicQris(
        baseStaticQris: String,
        amount: Long,
        fee: Long = 0L,
        tipType: String = "" // "PERCENTAGE" or "FIXED"
    ): String {
        val cleanBase = if (baseStaticQris.isNotBlank()) baseStaticQris.trim() else DEFAULT_STATIC_QRIS
        val tags = parseTlv(cleanBase).toMutableMap()

        // 1. Tag 01: Initiation Point Method -> "12" for Dynamic QR
        tags["01"] = "12"

        // 2. Tag 53: Currency -> "360" (IDR)
        if (!tags.containsKey("53")) {
            tags["53"] = "360"
        }

        // 3. Tag 54: Transaction Amount -> nominal (format "50000" or "50000.00")
        if (amount > 0) {
            tags["54"] = amount.toString()
        } else {
            tags.remove("54")
        }

        // 4. Optional Fee/Tip Tag 55 (Tip indicator) & 56/57 (Fee)
        if (fee > 0) {
            tags["55"] = "02" // Fixed fee
            tags["57"] = fee.toString()
        }

        // Reconstruct EMVCo string in canonical tag order
        val orderedKeys = listOf(
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "26", "27", "28", "29", "30", "31", "32", "33", "34", "35",
            "36", "37", "38", "39", "40", "41", "42", "43", "44", "45",
            "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62"
        )

        val sb = StringBuilder()
        // Add known ordered tags
        for (k in orderedKeys) {
            val v = tags[k]
            if (v != null && v.isNotEmpty()) {
                val lenStr = String.format(Locale.US, "%02d", v.length)
                sb.append(k).append(lenStr).append(v)
            }
        }

        // Add any additional tags not covered in ordered list (excluding 63)
        for ((k, v) in tags) {
            if (!orderedKeys.contains(k) && k != "63" && v.isNotEmpty()) {
                val lenStr = String.format(Locale.US, "%02d", v.length)
                sb.append(k).append(lenStr).append(v)
            }
        }

        // Append Tag 63 with length 04
        sb.append("6304")
        val crc = calculateCrc16(sb.toString())
        return sb.toString() + crc
    }

    /**
     * Creates a custom template static QRIS from merchant profile data.
     */
    fun createTemplateQris(
        merchantName: String,
        merchantCity: String,
        postalCode: String,
        nmid: String,
        mpan: String
    ): String {
        val cleanName = merchantName.take(25).uppercase().trim().ifBlank { "TOKO QRIS" }
        val cleanCity = merchantCity.take(15).uppercase().trim().ifBlank { "JAKARTA" }
        val cleanPostal = postalCode.take(10).trim().ifBlank { "10110" }
        val cleanNmid = nmid.trim().ifBlank { "ID1020304050607" }
        val cleanMpan = mpan.trim().ifBlank { "936001234567890" }

        // Tag 26 Sub-tags: 00=GUI, 01=MPAN, 02=NMID, 03=Criteria (UME)
        val tag26Builder = StringBuilder()
        val gui = "ID.CO.QRIS.WWW"
        tag26Builder.append("00").append(String.format(Locale.US, "%02d", gui.length)).append(gui)
        tag26Builder.append("01").append(String.format(Locale.US, "%02d", cleanMpan.length)).append(cleanMpan)
        tag26Builder.append("02").append(String.format(Locale.US, "%02d", cleanNmid.length)).append(cleanNmid)
        tag26Builder.append("0303UME")
        val tag26Val = tag26Builder.toString()

        val sb = StringBuilder()
        sb.append("000201") // Format Indicator
        sb.append("010211") // Static initiation
        sb.append("26").append(String.format(Locale.US, "%02d", tag26Val.length)).append(tag26Val)
        sb.append("52045411") // MCC: Grocery / Retail
        sb.append("5303360")  // Currency: IDR 360
        sb.append("5802ID")   // Country ID
        sb.append("59").append(String.format(Locale.US, "%02d", cleanName.length)).append(cleanName)
        sb.append("60").append(String.format(Locale.US, "%02d", cleanCity.length)).append(cleanCity)
        sb.append("61").append(String.format(Locale.US, "%02d", cleanPostal.length)).append(cleanPostal)
        sb.append("62070703A01")
        sb.append("6304")

        val crc = calculateCrc16(sb.toString())
        return sb.toString() + crc
    }

    /**
     * Generates a high-quality Bitmap from QR payload using ZXing.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val safeContent = if (content.isNotBlank()) content else DEFAULT_STATIC_QRIS
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(safeContent, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            val fallback = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(fallback)
            canvas.drawColor(Color.WHITE)
            fallback
        }
    }

    /**
     * Decodes a QR code payload from a given Bitmap.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Currency formatter helper for Indonesian Rupiah.
     */
    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp " + formatter.format(amount)
    }
}
