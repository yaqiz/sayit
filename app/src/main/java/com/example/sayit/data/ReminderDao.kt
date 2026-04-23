package com.example.sayit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminders WHERE triggerAtMillis >= :fromTime ORDER BY triggerAtMillis ASC")
    suspend fun getPendingReminders(fromTime: Long): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteById(reminderId: Long)
}
