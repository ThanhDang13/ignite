package com.example.alarm.feature.ring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.alarm.core.notification.NotificationManager

/**
 * Receives notification action intents (Dismiss / Snooze tapped from the
 * shade or lockscreen, or notification swiped away) and forwards them to
 * RingService so the alarm fully stops — audio, vibration, foreground
 * service, and the notification itself.
 *
 * Registered statically in the manifest so it works even if the app
 * process has been killed.
 */
class RingNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(NotificationManager.EXTRA_ALARM_ID, -1L)
        Log.d(TAG, "Received ${intent.action} for alarm $alarmId")

        // Hold a brief wake lock so the device doesn't return to sleep before
        // the service receives the command.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmApp:RingNotificationReceiver"
        ).apply {
            setReferenceCounted(false)
            acquire(10_000L)
        }

        try {
            val serviceAction = when (intent.action) {
                NotificationManager.ACTION_NOTIFICATION_DISMISS -> RingService.ACTION_DISMISS
                NotificationManager.ACTION_NOTIFICATION_SNOOZE -> RingService.ACTION_SNOOZE
                else -> {
                    Log.w(TAG, "Unknown action ${intent.action}")
                    return
                }
            }

            // Always cancel the visible notification immediately so the user
            // gets feedback even if the service takes a moment to fully stop.
            NotificationManagerCompat.from(context)
                .cancel(NotificationManager.NOTIFICATION_ID_ALARM)

            val serviceIntent = Intent(context, RingService::class.java).apply {
                action = serviceAction
                putExtra("alarmId", alarmId)
            }
            // Use startForegroundService so the service can keep running long
            // enough to release MediaPlayer and post stats.
            context.startForegroundService(serviceIntent)
        } finally {
            if (wl.isHeld) wl.release()
        }
    }

    companion object {
        private const val TAG = "RingNotifReceiver"
    }
}
