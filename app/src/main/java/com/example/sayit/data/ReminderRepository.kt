package com.example.sayit.data

import com.example.sayit.reminder.ReminderScheduler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) {
    suspend fun createReminder(content: String, triggerAtMillis: Long): VoiceActionResult {
        val normalizedContent = content.trim()
            .replace(Regex("^[,.!?，。！？、\\s]+|[,.!?，。！？、\\s]+$"), "")

        if (normalizedContent.isBlank()) {
            return VoiceActionResult("没听清提醒内容，请再说一次。")
        }
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return VoiceActionResult("这个提醒时间已经过去了，请重新设置。")
        }

        val reminder = ReminderEntity(
            content = normalizedContent,
            triggerAtMillis = triggerAtMillis
        )
        val reminderId = reminderDao.insert(reminder)
        val savedReminder = reminder.copy(id = reminderId)
        reminderScheduler.schedule(savedReminder)

        val timeLabel = DateTimeFormatter.ofPattern("M月d日 HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(triggerAtMillis))
        return VoiceActionResult("已设置提醒：$timeLabel $normalizedContent")
    }
}
