package com.example.sayit.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sayit.MainActivity
import com.example.sayit.R

object ReminderNotifier {
    const val CHANNEL_ID = "sayit_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "SayIt 语音提醒通知"
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun showReminder(context: Context, reminderId: Long, content: String) {
        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.sayit_header_icon)
            .setContentTitle("提醒")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
    }
}
