package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.qris.QrisEngine
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean = true
)

data class ReceiptData(
    val storeName: String,
    val storeAddress: String = "",
    val storePhone: String = "",
    val invoiceNumber: String,
    val customerName: String = "",
    val date: Date = Date(),
    val items: List<Pair<String, Long>> = emptyList(),
    val subtotal: Long,
    val fee: Long = 0,
    val totalAmount: Long,
    val paymentMethod: String = "QRIS Dinamis",
    val qrisPayload: String = "",
    val footerMessage: String = "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar."
)

object BluetoothThermalPrinter {

    // Standard SPP (Serial Port Profile) UUID for Bluetooth Thermal Printers
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS Command Constants
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val ESC_DOUBLE_HEIGHT_ON = byteArrayOf(0x1B, 0x21, 0x10)
    private val ESC_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)
    private val ESC_FEED_PAPER = byteArrayOf(0x1B, 0x64, 0x06)
    private val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x00)

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<BluetoothPrinterDevice> {
        val list = mutableListOf<BluetoothPrinterDevice>()
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val bonded = try { bluetoothAdapter.bondedDevices } catch (e: SecurityException) { null }
                if (bonded != null && bonded.isNotEmpty()) {
                    for (device in bonded) {
                        val name = try { device.name } catch (e: Exception) { null } ?: "Printer Bluetooth"
                        list.add(
                            BluetoothPrinterDevice(
                                name = name,
                                address = device.address,
                                isPaired = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Permission or hardware issue
        }

        // Add demo device if no bonded printers exist
        if (list.isEmpty()) {
            list.add(BluetoothPrinterDevice("Printer Thermal POS-58 (Demo)", "00:11:22:33:44:55", true))
        }
        return list
    }

    /**
     * Sanitizes non-ASCII / Unicode characters to clean printable ASCII for thermal printers.
     */
    fun sanitizeForPrinter(text: String): String {
        return text
            .replace("×", "x")
            .replace("✕", "x")
            .replace("✖", "x")
            .replace("⨯", "x")
            .replace("÷", "/")
            .replace("•", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("‘", "'")
            .replace("’", "'")
            .replace("🟢", "[OK]")
            .replace("🟡", "[PENDING]")
            .replace("🔴", "[GAGAL]")
            .replace("✅", "[OK]")
    }

    /**
     * Builds ESC/POS formatted receipt binary data.
     */
    fun buildEscPosBytes(receipt: ReceiptData, paperWidthChars: Int = 32): ByteArray {
        val out = mutableListOf<Byte>()

        fun append(bytes: ByteArray) {
            for (b in bytes) out.add(b)
        }

        fun appendText(text: String) {
            val cleanText = sanitizeForPrinter(text)
            for (b in cleanText.toByteArray(Charsets.US_ASCII)) out.add(b)
        }

        fun appendLine(text: String = "") {
            appendText(text + "\n")
        }

        fun appendDashes() {
            appendLine("-".repeat(paperWidthChars))
        }

        fun appendKeyValue(key: String, value: String) {
            val cleanKey = sanitizeForPrinter(key)
            val cleanValue = sanitizeForPrinter(value)
            val spaceNeeded = paperWidthChars - cleanKey.length - cleanValue.length
            if (spaceNeeded > 0) {
                appendLine(cleanKey + " ".repeat(spaceNeeded) + cleanValue)
            } else {
                appendLine(cleanKey)
                appendLine(" ".repeat(maxOf(0, paperWidthChars - cleanValue.length)) + cleanValue)
            }
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))

        // Initialize Printer
        append(byteArrayOf(0x1B, 0x40)) // ESC @
        append(ESC_NORMAL)

        // Header
        append(ESC_ALIGN_CENTER)
        append(ESC_BOLD_ON)
        append(ESC_DOUBLE_HEIGHT_ON)
        appendLine(receipt.storeName.uppercase())
        append(ESC_NORMAL)
        append(ESC_BOLD_OFF)
        if (receipt.storeAddress.isNotBlank()) {
            appendLine(receipt.storeAddress)
        }
        if (receipt.storePhone.isNotBlank()) {
            appendLine("Telp/WA: ${receipt.storePhone}")
        }
        appendLine("STRUK PEMBAYARAN QRIS")
        append(ESC_ALIGN_LEFT)
        appendDashes()

        // Meta Info
        appendKeyValue("No. Transaksi", receipt.invoiceNumber)
        appendKeyValue("Waktu", dateFormat.format(receipt.date))
        if (receipt.customerName.isNotBlank()) {
            appendKeyValue("Pelanggan", receipt.customerName)
        }
        appendKeyValue("Metode", "QRIS Dinamis")
        appendDashes()

        // Amounts
        if (receipt.items.isNotEmpty()) {
            for (item in receipt.items) {
                appendKeyValue(item.first, QrisEngine.formatRupiah(item.second))
            }
            appendDashes()
        }

        appendKeyValue("Subtotal", QrisEngine.formatRupiah(receipt.subtotal))
        if (receipt.fee > 0) {
            appendKeyValue("Biaya Layanan", QrisEngine.formatRupiah(receipt.fee))
        }
        append(ESC_BOLD_ON)
        appendKeyValue("TOTAL BAYAR", QrisEngine.formatRupiah(receipt.totalAmount))
        append(ESC_BOLD_OFF)
        appendDashes()

        // Simple and high-contrast QRIS Barcode section for customer scanning from receipt
        if (receipt.qrisPayload.isNotBlank()) {
            append(ESC_ALIGN_CENTER)
            append(ESC_BOLD_ON)
            appendLine("SCAN QRIS PEMBAYARAN")
            append(ESC_BOLD_OFF)
            
            val qrBytes = generateEscPosRasterQr(receipt.qrisPayload, paperWidthChars)
            if (qrBytes.isNotEmpty()) {
                append(ESC_ALIGN_LEFT)
                append(qrBytes)
                append(byteArrayOf(0x0D, 0x0A))
            }
            
            append(ESC_ALIGN_CENTER)
            appendLine("Scan via BCA, DANA, GoPay, OVO, dll.")
            append(ESC_ALIGN_LEFT)
            appendDashes()
        }

        // Keterangan / Footer Struk tepat di bawah QRIS (agar jika terpotong, yang terpotong adalah catatan footer bukan QRIS)
        append(ESC_ALIGN_CENTER)
        append(ESC_BOLD_ON)
        appendLine("--- CATATAN TRANSAKSI ---")
        append(ESC_BOLD_OFF)
        val cleanFooter = receipt.footerMessage.ifBlank { "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar." }
        for (line in cleanFooter.split("\n")) {
            if (line.isNotBlank()) {
                appendLine(line.trim())
            }
        }
        appendLine("Simpan struk ini sebagai bukti pembayaran sah.")
        appendLine("Layanan: 081947215703 • ALIJAYA-NET")
        append(ESC_ALIGN_CENTER)
        appendLine("================================")
        appendLine("*** TERIMA KASIH ***")

        // Paper feed pas ~1 cm melewati gerigi pemotong (hemat kertas & rapi)
        for (i in 0 until 3) {
            append(byteArrayOf(0x0D, 0x0A))
        }

        return out.toByteArray()
    }

    /**
     * Converts QR payload into pixel-perfect ESC/POS Raster Bit Image (GS v 0).
     * Uses ErrorCorrectionLevel.L and 4-module quiet zone with exact integer module dot scaling
     * to eliminate thermal dot bleed and guarantee instant scanning on smartphone cameras.
     */
    fun generateEscPosRasterQr(payload: String, paperWidthChars: Int = 32): ByteArray {
        return try {
            val safeContent = if (payload.isNotBlank()) payload.trim() else QrisEngine.DEFAULT_STATIC_QRIS

            // 1. Generate QR matrix with standard 4-module quiet zone and Medium error correction (EMVCo QRIS standard)
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 4,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(safeContent, BarcodeFormat.QR_CODE, 0, 0, hints)
            val moduleCount = matrix.width // Total modules including 4-module quiet zone

            // 2. Paper dot dimensions (384 dots for standard 58mm printer)
            val totalPaperDots = if (paperWidthChars >= 40) 576 else 384
            val totalBytesWidth = totalPaperDots / 8 // 48 bytes per line

            // 3. Integer module scaling:
            // For 58mm paper: 4 dots/module gives 212x212 dots (~26.5mm), perfect centering, fits within 1 byte (yH=0) so no 16-bit firmware bugs.
            // For 80mm paper: 5 or 6 dots/module (~33-40mm).
            val maxAllowedDots = if (paperWidthChars >= 40) 400 else 240
            val dotsPerModule = maxOf(3, minOf(if (paperWidthChars >= 40) 6 else 4, maxAllowedDots / moduleCount))

            val qrDotsWidth = moduleCount * dotsPerModule
            val qrDotsHeight = moduleCount * dotsPerModule

            // Align width to multiple of 8
            val qrBytesWidth = (qrDotsWidth + 7) / 8
            val actualWidthDots = qrBytesWidth * 8

            val leftMarginDots = maxOf(0, (totalPaperDots - actualWidthDots) / 2)
            val leftMarginBytes = leftMarginDots / 8
            val rightMarginBytes = maxOf(0, totalBytesWidth - qrBytesWidth - leftMarginBytes)

            val xL = (totalBytesWidth and 0xFF).toByte()
            val xH = ((totalBytesWidth shr 8) and 0xFF).toByte()
            val yL = (qrDotsHeight and 0xFF).toByte()
            val yH = ((qrDotsHeight shr 8) and 0xFF).toByte()

            val result = mutableListOf<Byte>()
            // GS v 0 0: ESC/POS raster bit image command
            result.add(0x1D.toByte())
            result.add(0x76.toByte())
            result.add(0x30.toByte())
            result.add(0x00.toByte())
            result.add(xL)
            result.add(xH)
            result.add(yL)
            result.add(yH)

            // Render each module row
            for (mY in 0 until moduleCount) {
                // Repeat each row dotsPerModule times (vertical integer scaling)
                for (subY in 0 until dotsPerModule) {
                    // Left margin bytes
                    for (m in 0 until leftMarginBytes) {
                        result.add(0x00.toByte())
                    }

                    // QR image bytes
                    var currentByte = 0
                    var bitIndex = 7
                    for (mX in 0 until moduleCount) {
                        val isBlack = matrix.get(mX, mY)
                        for (subX in 0 until dotsPerModule) {
                            if (isBlack) {
                                currentByte = currentByte or (1 shl bitIndex)
                            }
                            bitIndex--
                            if (bitIndex < 0) {
                                result.add(currentByte.toByte())
                                currentByte = 0
                                bitIndex = 7
                            }
                        }
                    }
                    if (bitIndex != 7) {
                        result.add(currentByte.toByte())
                    }

                    // Right margin bytes
                    for (m in 0 until rightMarginBytes) {
                        result.add(0x00.toByte())
                    }
                }
            }
            result.toByteArray()
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    /**
     * Attempts Bluetooth socket print connection with 4-tier fallback (Insecure SPP -> Secure SPP -> Reflection Insecure -> Reflection).
     */
    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        deviceAddress: String,
        receipt: ReceiptData
    ): Result<String> = withContext(Dispatchers.IO) {
        if (deviceAddress.contains("Demo", ignoreCase = true) || deviceAddress == "00:11:22:33:44:55") {
            return@withContext Result.failure(Exception("Printer thermal fisik belum dipilih. Silakan pasangkan (pairing) printer Bluetooth Anda di HP, lalu pilih di menu Printer."))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung pada perangkat ini"))

            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(Exception("Bluetooth HP nonaktif. Harap aktifkan Bluetooth terlebih dahulu."))
            }

            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)

            // Safe cancel discovery
            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: Throwable) {
                // Ignore
            }

            // 4-Tier Socket Connection Strategy
            socket = try {
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
            } catch (e1: Exception) {
                try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
                } catch (e2: Exception) {
                    try {
                        val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                        (method.invoke(device, 1) as BluetoothSocket).apply { connect() }
                    } catch (e3: Exception) {
                        try {
                            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            (method.invoke(device, 1) as BluetoothSocket).apply { connect() }
                        } catch (e4: Exception) {
                            throw Exception("Tidak dapat terhubung ke printer ($deviceAddress). Pastikan printer dalam keadaan HIDUP dan sudah dipasangkan di Pengaturan Bluetooth HP.")
                        }
                    }
                }
            }

            kotlinx.coroutines.delay(100)

            outputStream = socket.outputStream
            val bytes = buildEscPosBytes(receipt)
            // Stream data in 512-byte packets to avoid serial buffer overrun on POS-58 printers
            val chunkSize = 512
            var offset = 0
            while (offset < bytes.size) {
                val len = minOf(chunkSize, bytes.size - offset)
                outputStream.write(bytes, offset, len)
                outputStream.flush()
                offset += len
                kotlinx.coroutines.delay(25)
            }
            // Give printer motor ample time to finish printing footer and feed paper completely
            kotlinx.coroutines.delay(1200)

            val deviceName = try { device.name } catch (e: Exception) { null } ?: deviceAddress
            Result.success("Struk berhasil dicetak ke printer $deviceName! 🖨️")
        } catch (e: SecurityException) {
            Result.failure(Exception("Izin Bluetooth belum diaktifkan. Harap izinkan akses 'Perangkat di Sekitar' di Info Aplikasi."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal terhubung ke printer ($deviceAddress)"))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    /**
     * Tests connection to a physical thermal printer and prints a small sample test slip.
     */
    @SuppressLint("MissingPermission")
    suspend fun testPrinterConnection(deviceAddress: String): Result<String> = withContext(Dispatchers.IO) {
        if (deviceAddress.contains("Demo", ignoreCase = true) || deviceAddress == "00:11:22:33:44:55") {
            return@withContext Result.failure(Exception("Pilih printer fisik di daftar perangkat untuk melakukan tes koneksi."))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung pada perangkat ini"))

            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(Exception("Bluetooth HP nonaktif. Harap aktifkan Bluetooth terlebih dahulu."))
            }

            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)

            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: Throwable) {
                // Ignore
            }

            // 4-Tier Socket Connection Strategy
            socket = try {
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
            } catch (e1: Exception) {
                try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
                } catch (e2: Exception) {
                    try {
                        val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                        (method.invoke(device, 1) as BluetoothSocket).apply { connect() }
                    } catch (e3: Exception) {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        (method.invoke(device, 1) as BluetoothSocket).apply { connect() }
                    }
                }
            }

            kotlinx.coroutines.delay(100)
            outputStream = socket.outputStream

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
            val testText = StringBuilder().apply {
                append("\n")
                append("================================\n")
                append("     TES KONEKSI PRINTER POS   \n")
                append("================================\n")
                append("Perangkat : ${device.name ?: deviceAddress}\n")
                append("Alamat MAC: $deviceAddress\n")
                append("Status    : TERHUBUNG NORMAL 🟢\n")
                append("Waktu     : ${dateFormat.format(Date())}\n")
                append("--------------------------------\n")
                append("Printer thermal Bluetooth Anda\n")
                append("siap mencetak struk transaksi!\n")
                append("================================\n\n\n\n")
            }.toString()

            val initBytes = byteArrayOf(0x1B, 0x40, 0x1B, 0x61, 0x01) // ESC @ and center
            outputStream.write(initBytes)
            outputStream.write(sanitizeForPrinter(testText).toByteArray(Charsets.US_ASCII))
            outputStream.write(byteArrayOf(0x1B, 0x64, 0x04)) // feed 4 lines
            outputStream.flush()

            kotlinx.coroutines.delay(1000)

            val deviceName = try { device.name } catch (e: Exception) { null } ?: deviceAddress
            Result.success("✅ Tes Berhasil! Printer $deviceName terhubung dan mencetak sampel.")
        } catch (e: SecurityException) {
            Result.failure(Exception("Izin Bluetooth belum diberikan. Harap izinkan akses 'Perangkat di Sekitar' di Info Aplikasi."))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal tes koneksi ($deviceAddress): ${e.localizedMessage ?: "Printer tidak merespons"}"))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}
