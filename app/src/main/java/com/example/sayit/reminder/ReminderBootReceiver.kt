package com.example.sayit.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sayit.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val reminderDao = AppDatabase.getInstance(context).reminderDao()
                val scheduler = ReminderAlarmScheduler(context)
                reminderDao.getPendingReminders(System.currentTimeMillis()).forEach { reminder ->
                    scheduler.schedule(reminder)
                }
            }
            pendingResult.finish()
        }
    }
}
