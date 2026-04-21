package com.example.sayit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE createdDate = :date ORDER BY isCompleted ASC, createdAt DESC")
    fun observeTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE createdDate BETWEEN :startDate AND :endDate ORDER BY createdDate ASC, isCompleted ASC, createdAt DESC")
    fun observeTasksBetween(startDate: String, endDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE createdDate = :date ORDER BY isCompleted ASC, createdAt DESC")
    suspend fun getTasksForDate(date: String): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateCompletion(taskId: Long, completed: Boolean, completedAt: Long?)

    @Query("DELETE FROM tasks WHERE createdDate = :date")
    suspend fun deleteTasksForDate(date: String): Int
}
