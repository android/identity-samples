package com.example.android.authentication.myvault

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.android.authentication.myvault.ui.MainActivity

/**
 * Creates and registers a notification channel with the system.
 *
 * This is a utility extension function that creates a Notification Channel with
 * [NotificationManager.IMPORTANCE_HIGH] to show pop-up notification on receiving
 * signals from the RP apps
 *
 * @param channelName The user-visible name of the channel.
 *                    This is displayed in the system's notification settings.
 * @param channelDescription The user-visible description of the channel.
 *                           This is displayed in the system's notification settings.
 */
fun Context.createNotificationChannel(
    channelName: String,
    channelDescription: String,
) {
    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        channelName,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = channelDescription
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

/**
 * Utility extension function that displays a system notification with the given title and content.
 *
 * @param title The title of the notification.
 * @param content The main content text of the notification.
 */
fun Context.showNotification(
    title: String,
    content: String,
) {
    val intent = Intent(this, MainActivity::class.java)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.android_secure)
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(this)) {
        if (ActivityCompat.checkSelfPermission(
                this@showNotification,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            return@with
        }
        notify(NOTIFICATION_ID, builder.build())
    }
}
