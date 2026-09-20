package com.example.notification

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PaymentDetectorManager {

    private val _paymentEvents = MutableSharedFlow<ParsedPayment>(extraBufferCapacity = 10)
    val paymentEvents: SharedFlow<ParsedPayment> = _paymentEvents.asSharedFlow()

    @Volatile
    var lastDetectedPayment: ParsedPayment? = null
        private set

    /**
     * Dispatches an incoming payment event received from NotificationListenerService or Simulator.
     */
    fun postPayment(payment: ParsedPayment) {
        lastDetectedPayment = payment
        _paymentEvents.tryEmit(payment)
    }

    /**
     * Checks whether the user has granted 'Notification Access' to this application in Android Settings.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    /**
     * Opens Android System Settings directly to Notification Listener access page.
     */
    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to application details if listener settings not directly accessible
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (ignored: Exception) {}
        }
    }
}
