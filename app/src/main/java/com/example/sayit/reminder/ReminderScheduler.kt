package com.example.sayit.reminder

import com.example.sayit.data.ReminderEntity

interface ReminderScheduler {
    fun schedule(reminder: ReminderEntity)
}
