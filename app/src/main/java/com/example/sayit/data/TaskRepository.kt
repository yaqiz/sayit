package com.example.sayit.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
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
            return VoiceActionResult("没听清任务内容，请再说一次。")
        }

        val dateKey = date.toString()
        val dayLabel = dayLabel(date)
        val todayTasks = taskDao.getTasksForDate(dateKey)
        val normalized = normalizeForMatch(title)
        val duplicate = todayTasks.firstOrNull { it.normalizedTitle == normalized }
        if (duplicate != null) {
            return VoiceActionResult("${dayLabel}已经有这个任务：${duplicate.title}", duplicate.id)
        }

        val task = TaskEntity(
            title = title,
            normalizedTitle = normalized,
            createdDate = dateKey
        )
        val id = taskDao.insert(task)
        return VoiceActionResult("已记录${dayLabel}任务：$title", id)
    }

    suspend fun markTaskCompletedForToday(rawQuery: String): VoiceActionResult {
        return markTaskCompletedForDate(rawQuery, LocalDate.now())
    }

    suspend fun markTaskCompletedForDate(rawQuery: String, date: LocalDate): VoiceActionResult {
        val query = sanitizeTaskTitle(rawQuery)
        if (query.isBlank()) {
            return VoiceActionResult("没听清你要完成哪个任务，请再说一次。")
        }

        val dateKey = date.toString()
        val dayLabel = dayLabel(date)
        val todayTasks = taskDao.getTasksForDate(dateKey)
        if (todayTasks.isEmpty()) {
            return VoiceActionResult("${dayLabel}还没有任何任务。")
        }

        val match = findBestMatch(query, todayTasks)
            ?: return VoiceActionResult("没有找到和${query}相关的任务。")

        if (match.isCompleted) {
            return VoiceActionResult("${match.title} 已经完成过了。", match.id)
        }

        taskDao.updateCompletion(match.id, true, System.currentTimeMillis())
        return VoiceActionResult("已完成任务：${match.title}", match.id)
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
            VoiceActionResult("${dayLabel}没有可删除的任务。")
        } else {
            VoiceActionResult("已删除${dayLabel}的全部任务，共${deletedCount}项。")
        }
    }

    suspend fun buildSummaryForDate(completed: Boolean, date: LocalDate): VoiceActionResult {
        val todayTasks = taskDao.getTasksForDate(date.toString())
        val filtered = todayTasks.filter { it.isCompleted == completed }
        val dayLabel = dayLabel(date)
        if (filtered.isEmpty()) {
            return if (completed) {
                VoiceActionResult("${dayLabel}还没有已完成任务。")
            } else {
                VoiceActionResult("${dayLabel}没有还没完成的任务。")
            }
        }

        val prefix = if (completed) "${dayLabel}已完成" else "${dayLabel}还没完成"
        val titles = filtered.joinToString("，") { it.title }
        return VoiceActionResult("$prefix ${filtered.size} 项：$titles")
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
            "今天"
        } else {
            "${date.monthValue}月${date.dayOfMonth}日"
        }
    }

    private fun sanitizeTaskTitle(raw: String): String {
        return raw.trim()
            .trim('。', '，', ',', '.', '！', '？', '、', ' ')
            .replace(Regex("\\s+"), "")
    }

    private fun normalizeForMatch(raw: String): String {
        return raw.lowercase()
            .replace(Regex("[\\p{Punct}，。！？、；：‘’“”《》【】（）()\\s]"), "")
            .replace("任务", "")
            .replace("人物", "")
            .replace("今天", "")
            .replace("一下", "")
            .replace("给我", "")
            .replace("告诉我", "")
    }
}
