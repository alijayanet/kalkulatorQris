package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.qris.QrisEngine

object NotificationHelper {

    private const val CHANNEL_ID = "qris_payment_channel"
    private const val CHANNEL_NAME = "Transaksi & Notifikasi QRIS"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi status real-time pembayaran QRIS Dinamis"
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPaymentSuccessNotification(
        context: Context,
        invoice: String,
        amount: Long,
        merchantName: String
    ) {
        initNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            invoice.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedAmount = QrisEngine.formatRupiah(amount)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Pembayaran QRIS Diterima! 💰")
            .setContentText("$formattedAmount berhasil dibayar ke $merchantName ($invoice)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Pembayaran QRIS sebesar $formattedAmount berhasil diterima secara real-time untuk transaksi $invoice di $merchantName. Transaksi telah otomatis dicatat di riwayat.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(invoice.hashCode(), notification)
    }

    fun showQrisGeneratedNotification(
        context: Context,
        invoice: String,
        amount: Long
    ) {
        initNotificationChannel(context)

        val formattedAmount = QrisEngine.formatRupiah(amount)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Kode QRIS Siap di-Scan")
            .setContentText("QRIS Dinamis $formattedAmount untuk $invoice siap discan pelanggan.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(invoice.hashCode() + 1, notification)
    }
}
