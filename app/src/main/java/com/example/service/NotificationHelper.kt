package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "focus_lock_channel"
        const val NOTIFICATION_ID = 1001
        const val BLOCK_CHANNEL_ID = "focus_lock_block_channel"
        const val BLOCK_NOTIFICATION_ID = 2002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val generalChannel = NotificationChannel(
                CHANNEL_ID,
                "FocusLock Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for focus sessions and app limits"
            }
            notificationManager.createNotificationChannel(generalChannel)

            val blockChannel = NotificationChannel(
                BLOCK_CHANNEL_ID,
                "FocusLock Block Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays the mindful blocking screen when restricted apps are opened"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(blockChannel)
        }
    }

    fun showBlockFullScreenNotification(
        appName: String,
        packageName: String,
        usedMinutes: Int,
        limitMinutes: Int
    ) {
        val blockIntent = Intent(context, com.example.presentation.blocking.BlockActivity::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("PACKAGE_NAME", packageName)
            putExtra("USED_MINUTES", usedMinutes)
            putExtra("LIMIT_MINUTES", limitMinutes)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            blockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, BLOCK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("$appName is Blocked")
            .setContentText(
                if (limitMinutes == 0) "$appName is restricted in FocusLock. Tap to open lock screen."
                else "Daily limit of $limitMinutes min reached ($usedMinutes min used). Tap to view options."
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(BLOCK_NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun showNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: use real icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
