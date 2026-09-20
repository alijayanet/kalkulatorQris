package com.example.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class QrisNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        // Do not process notifications from our own app
        if (pkgName == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val effectiveBody = if (!bigText.isNullOrBlank()) bigText else text

        try {
            val payment = PaymentNotificationParser.parse(
                packageName = pkgName,
                title = title,
                text = effectiveBody
            )

            if (payment != null) {
                Log.d("QrisNotifListener", "Incoming payment detected: ${payment.amount} from ${payment.appName}")
                PaymentDetectorManager.postPayment(payment)
            }
        } catch (e: Exception) {
            Log.e("QrisNotifListener", "Error parsing notification", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("QrisNotifListener", "Notification Listener Connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("QrisNotifListener", "Notification Listener Disconnected.")
    }
}
