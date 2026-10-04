package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "focus_lock_channel"
        const val NOTIFICATION_ID = 1001
        const val BLOCK_CHANNEL_ID = "focus_lock_block_channel"
        const val BLOCK_NOTIFICATION_ID = 2002
        private const val BRAND_CYAN = 0xFF24DFEC.toInt()
        private const val BRAND_AMBER = 0xFFFFB300.toInt()
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
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableLights(true)
                lightColor = BRAND_CYAN
            }
            notificationManager.createNotificationChannel(generalChannel)

            val blockChannel = NotificationChannel(
                BLOCK_CHANNEL_ID,
                context.getString(R.string.block_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.block_channel_desc)
                setBypassDnd(true)
                enableLights(true)
                lightColor = BRAND_CYAN
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

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_custom_logo)
        } catch (e: Exception) {
            null
        }

        val title = if (limitMinutes == 0) {
            "Focus mode: ON. Distractions: BYE 🔒"
        } else {
            "Time to touch grass 🌱 — $appName paused"
        }

        val body = if (limitMinutes == 0) {
            "$appName is locked right now so you can stay in your flow state. Tap to view options."
        } else {
            "You've used ${com.example.util.FormatUtils.formatHoursMinutes(usedMinutes)} / ${com.example.util.FormatUtils.formatHoursMinutes(limitMinutes)} on $appName today. Protect your streak or complete a mindful challenge."
        }

        val builder = NotificationCompat.Builder(context, BLOCK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .apply { largeIcon?.let { setLargeIcon(it) } }
            .setSubText("FocusLock • Distraction Shield")
            .setColor(BRAND_CYAN)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
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

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_custom_logo)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .apply { largeIcon?.let { setLargeIcon(it) } }
            .setSubText("FocusLock • Stay Locked In")
            .setColor(BRAND_CYAN)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(message)
            )
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

    fun showLimitWarningNotification(appName: String, usedMinutes: Int, limitMinutes: Int) {
        val remaining = (limitMinutes - usedMinutes).coerceAtLeast(0)
        val title = "Heads up! ${com.example.util.FormatUtils.formatRemaining(remaining)} on $appName ⏳"
        val message = "You've used ${com.example.util.FormatUtils.formatHoursMinutes(usedMinutes)} of your ${com.example.util.FormatUtils.formatHoursMinutes(limitMinutes)} daily budget. Wrap up soon to keep your streak alive 🔥"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appName.hashCode() + 500,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_custom_logo)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .apply { largeIcon?.let { setLargeIcon(it) } }
            .setSubText("FocusLock • Mindful Check-In")
            .setColor(BRAND_AMBER)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(appName.hashCode() + 500, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showFocusModeNotification(isActive: Boolean, remainingMinutes: Int? = null) {
        val title = if (isActive) {
            "Locked In 🔒 Deep Focus Active"
        } else {
            "You're on a roll! 🔥 Session Complete"
        }
        val message = if (isActive) {
            "Focus mode: ON. Distractions: BYE. ${remainingMinutes ?: 25} min left in your flow state ✨"
        } else {
            "Deep focus crushed! +50 XP earned and your streak is thriving. Time to touch grass 🌱"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("shortcut_destination", "FOCUS")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_custom_logo)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .apply { largeIcon?.let { setLargeIcon(it) } }
            .setSubText(if (isActive) "FocusLock • Flow State" else "FocusLock • +50 XP Earned")
            .setColor(BRAND_CYAN)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(3003, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
