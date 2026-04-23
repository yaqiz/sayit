package com.example.sayit.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.sayit.data.ReminderEntity

class ReminderAlarmScheduler(
    private val context: Context
) : ReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminder: ReminderEntity) {
        val pendingIntent = buildPendingIntent(context, reminder)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pendingIntent
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pendingIntent
                )
            }

            else -> {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_CONTENT = "extra_reminder_content"

        fun buildPendingIntent(context: Context, reminder: ReminderEntity): PendingIntent {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_REMINDER_CONTENT, reminder.content)
            }
            return PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
