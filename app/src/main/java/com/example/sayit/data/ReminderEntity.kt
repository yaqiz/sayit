package com.example.sayit.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [Index("triggerAtMillis")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val triggerAtMillis: Long,
    val createdAt: Long = System.currentTimeMillis()
)
