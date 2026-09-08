package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.printer.ReceiptData
import com.example.qris.QrisEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageShareUtils {

    /**
     * Creates a high-fidelity visual QRIS Card / Receipt Bitmap.
     */
    fun createQrisCardBitmap(
        context: Context,
        merchantName: String,
        merchantCity: String,
        nmid: String,
        invoice: String,
        amount: Long,
        qrisPayload: String,
        customerName: String = ""
    ): Bitmap {
        val width = 720
        val height = 1100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Main Card White Background with rounded corners & shadow
        val cardPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(30f, 30f, (width - 30).toFloat(), (height - 30).toFloat())
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        // Top Header Banner (QRIS Red)
        val headerPaint = Paint().apply {
            color = Color.rgb(211, 47, 47)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val headerPath = android.graphics.Path().apply {
            addRoundRect(
                RectF(30f, 30f, (width - 30).toFloat(), 150f),
                floatArrayOf(32f, 32f, 32f, 32f, 0f, 0f, 0f, 0f),
                android.graphics.Path.Direction.CW
            )
        }
        canvas.drawPath(headerPath, headerPaint)

        // QRIS Banner Text
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("QRIS DINAMIS NASIONAL", width / 2f, 95f, headerTextPaint)

        val subHeaderTextPaint = Paint().apply {
            color = Color.rgb(255, 235, 238)
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Satu QR Code untuk Semua Pembayaran", width / 2f, 130f, subHeaderTextPaint)

        // Merchant Details
        val merchantTitlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 32f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(merchantName.uppercase(), width / 2f, 205f, merchantTitlePaint)

        val metaPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("NMID: $nmid  •  $merchantCity", width / 2f, 240f, metaPaint)

        // Draw Generated QR Code Bitmap
        val qrBitmap = QrisEngine.generateQrBitmap(qrisPayload, 420)
        canvas.drawBitmap(qrBitmap, ((width - 420) / 2).toFloat(), 270f, null)

        // Amount Box Container
        val amountBoxPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val amountRect = RectF(70f, 720f, (width - 70).toFloat(), 840f)
        canvas.drawRoundRect(amountRect, 20f, 20f, amountBoxPaint)

        val totalLabelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("TOTAL TAGIHAN PEMBAYARAN", width / 2f, 760f, totalLabelPaint)

        val amountValPaint = Paint().apply {
            color = Color.rgb(211, 47, 47)
            textSize = 46f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(QrisEngine.formatRupiah(amount), width / 2f, 815f, amountValPaint)

        // Invoice & Timestamp Info
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date()) + " WIB"
        val footerInfoPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        if (customerName.isNotBlank()) {
            canvas.drawText("Pelanggan: $customerName", width / 2f, 875f, footerInfoPaint)
            canvas.drawText("No. Transaksi: $invoice  •  $dateStr", width / 2f, 915f, footerInfoPaint)
        } else {
            canvas.drawText("No. Transaksi: $invoice", width / 2f, 885f, footerInfoPaint)
            canvas.drawText("Dibuat pada: $dateStr", width / 2f, 920f, footerInfoPaint)
        }

        // Security / Footer Badge
        val gpnBadgePaint = Paint().apply {
            color = Color.rgb(15, 118, 110)
            textSize = 18f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("DIJAMIN OLEH BANK INDONESIA & ASOSIASI SISTEM PEMBAYARAN INDONESIA", width / 2f, 980f, gpnBadgePaint)

        val appTagPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 17f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Dibuat via Aplikasi Kalkulator QRIS Dinamis", width / 2f, 1020f, appTagPaint)

        return bitmap
    }

    /**
     * Saves bitmap to device MediaStore gallery directly.
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val fileName = "QRIS_${System.currentTimeMillis()}.png"
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KalkulatorQRIS")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Gagal menginisialisasi penyimpanan galeri"))

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menyimpan gambar ke galeri: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Shares receipt/QR image via Android Intent.
     */
    suspend fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        subject: String,
        text: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "images")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val file = File(cachePath, "share_qris_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(
                context,
                authority,
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Kode QRIS / Struk").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membagikan: ${e.localizedMessage ?: e.message}"))
        }
    }
}
