package com.example.sayit.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sayit.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderAlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val content = intent.getStringExtra(ReminderAlarmScheduler.EXTRA_REMINDER_CONTENT).orEmpty()
        if (reminderId <= 0L || content.isBlank()) return

        ReminderNotifier.showReminder(context, reminderId, content)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AppDatabase.getInstance(context).reminderDao().deleteById(reminderId)
            }
            pendingResult.finish()
        }
    }
}
