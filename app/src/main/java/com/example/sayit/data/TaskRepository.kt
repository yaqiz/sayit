package com.example.sayit.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

data class VoiceActionResult(
    val message: String,
    val matchedTaskId: Long? = null
)

class TaskRepository(private val taskDao: TaskDao) {
    fun observeTodayTasks(): Flow<List<TaskEntity>> = taskDao.observeTasksForDate(todayKey())
    fun observeTasksForDate(date: LocalDate): Flow<List<TaskEntity>> = taskDao.observeTasksForDate(date.toString())
    fun observeTasksBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>> =
        taskDao.observeTasksBetween(startDate.toString(), endDate.toString())

    suspend fun addTaskForToday(rawTitle: String): VoiceActionResult {
        return addTaskForDate(rawTitle, LocalDate.now())
    }

    suspend fun addTaskForDate(rawTitle: String, date: LocalDate): VoiceActionResult {
        val title = sanitizeTaskTitle(rawTitle)
        if (title.isBlank()) {
            return VoiceActionResult("I couldn't catch the task. Please try again.")
        }

        val dateKey = date.toString()
        val dayLabel = dayLabel(date)
        val tasksForDate = taskDao.getTasksForDate(dateKey)
        val normalized = normalizeForMatch(title)
        val duplicate = tasksForDate.firstOrNull { it.normalizedTitle == normalized }
        if (duplicate != null) {
            return VoiceActionResult("This task already exists for $dayLabel: ${duplicate.title}", duplicate.id)
        }

        val task = TaskEntity(
            title = title,
            normalizedTitle = normalized,
            createdDate = dateKey
        )
        val id = taskDao.insert(task)
        return VoiceActionResult("Added task for $dayLabel: $title", id)
    }

    suspend fun markTaskCompletedForToday(rawQuery: String): VoiceActionResult {
        return markTaskCompletedForDate(rawQuery, LocalDate.now())
    }

    suspend fun markTaskCompletedForDate(rawQuery: String, date: LocalDate): VoiceActionResult {
        val query = sanitizeTaskTitle(rawQuery)
        if (query.isBlank()) {
            return VoiceActionResult("I couldn't tell which task you wanted to complete. Please try again.")
        }

        val dateKey = date.toString()
        val dayLabel = dayLabel(date)
        val tasksForDate = taskDao.getTasksForDate(dateKey)
        if (tasksForDate.isEmpty()) {
            return VoiceActionResult("There are no tasks for $dayLabel.")
        }

        val match = findBestMatch(query, tasksForDate)
            ?: return VoiceActionResult("I couldn't find a task related to \"$query\".")

        if (match.isCompleted) {
            return VoiceActionResult("${match.title} is already marked as done.", match.id)
        }

        taskDao.updateCompletion(match.id, true, System.currentTimeMillis())
        return VoiceActionResult("Marked as done: ${match.title}", match.id)
    }

    suspend fun setTaskCompletion(taskId: Long, completed: Boolean) {
        taskDao.updateCompletion(taskId, completed, if (completed) System.currentTimeMillis() else null)
    }

    suspend fun buildTodaySummary(completed: Boolean): VoiceActionResult {
        return buildSummaryForDate(completed, LocalDate.now())
    }

    suspend fun deleteAllTasksForToday(): VoiceActionResult {
        return deleteAllTasksForDate(LocalDate.now())
    }

    suspend fun deleteAllTasksForDate(date: LocalDate): VoiceActionResult {
        val deletedCount = taskDao.deleteTasksForDate(date.toString())
        val dayLabel = dayLabel(date)
        return if (deletedCount == 0) {
            VoiceActionResult("There are no tasks to delete for $dayLabel.")
        } else {
            VoiceActionResult("Deleted all tasks for $dayLabel. Total removed: $deletedCount.")
        }
    }

    suspend fun buildSummaryForDate(completed: Boolean, date: LocalDate): VoiceActionResult {
        val tasksForDate = taskDao.getTasksForDate(date.toString())
        val filtered = tasksForDate.filter { it.isCompleted == completed }
        val dayLabel = dayLabel(date)

        if (filtered.isEmpty()) {
            return if (completed) {
                VoiceActionResult("There are no completed tasks for $dayLabel.")
            } else {
                VoiceActionResult("There are no outstanding tasks for $dayLabel.")
            }
        }

        val prefix = if (completed) {
            "Completed tasks for $dayLabel"
        } else {
            "Outstanding tasks for $dayLabel"
        }
        val titles = filtered.joinToString(", ") { it.title }
        return VoiceActionResult("$prefix (${filtered.size}): $titles")
    }

    private fun findBestMatch(query: String, tasks: List<TaskEntity>): TaskEntity? {
        val normalizedQuery = normalizeForMatch(query)
        if (normalizedQuery.isBlank()) return null

        return tasks
            .map { task -> task to similarity(normalizedQuery, task.normalizedTitle) }
            .filter { (_, score) -> score >= 0.45 }
            .sortedWith(
                compareByDescending<Pair<TaskEntity, Double>> { it.second }
                    .thenBy { it.first.isCompleted }
                    .thenByDescending { it.first.createdAt }
            )
            .firstOrNull()
            ?.first
    }

    private fun similarity(left: String, right: String): Double {
        if (left == right) return 1.0
        if (left.contains(right) || right.contains(left)) {
            return min(left.length, right.length).toDouble() / max(left.length, right.length) + 0.25
        }

        val common = commonCharCount(left, right).toDouble() / max(left.length, right.length)
        val prefix = commonPrefixLength(left, right).toDouble() / max(left.length, right.length)
        return max(common, prefix)
    }

    private fun commonCharCount(left: String, right: String): Int {
        val counts = mutableMapOf<Char, Int>()
        right.forEach { char -> counts[char] = (counts[char] ?: 0) + 1 }

        var total = 0
        left.forEach { char ->
            val count = counts[char] ?: 0
            if (count > 0) {
                total += 1
                counts[char] = count - 1
            }
        }
        return total
    }

    private fun commonPrefixLength(left: String, right: String): Int {
        val minLength = min(left.length, right.length)
        for (index in 0 until minLength) {
            if (left[index] != right[index]) {
                return index
            }
        }
        return minLength
    }

    private fun todayKey(): String = LocalDate.now().toString()

    private fun dayLabel(date: LocalDate): String {
        val today = LocalDate.now()
        return if (date == today) {
            "today"
        } else {
            date.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }

    private fun sanitizeTaskTitle(raw: String): String {
        return raw.trim()
            .trim('.', ',', '!', '?', ';', ':', '"', '\'')
            .replace(Regex("\\s+"), " ")
    }

    private fun normalizeForMatch(raw: String): String {
        return raw.lowercase()
            .replace(Regex("[\\p{Punct}\\s]"), "")
            .replace("task", "")
            .replace("today", "")
            .replace("please", "")
            .replace("my", "")
    }
}
