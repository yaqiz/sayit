package com.example.sayit.voice

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

sealed interface VoiceCommand {
    data class AddTask(val title: String) : VoiceCommand
    data class CompleteTask(val query: String) : VoiceCommand
    data class QueryTasks(val completed: Boolean) : VoiceCommand
    data object DeleteSelectedDateTasks : VoiceCommand
    data class CreateReminder(val content: String, val triggerAtMillis: Long) : VoiceCommand
    data class Unknown(val originalText: String) : VoiceCommand
}

object VoiceCommandParser {
    fun parse(text: String): VoiceCommand {
        val spoken = text.trim()
        if (spoken.isBlank()) {
            return VoiceCommand.Unknown(text)
        }

        if (isQuery(spoken, completed = false)) {
            return VoiceCommand.QueryTasks(completed = false)
        }
        if (isQuery(spoken, completed = true)) {
            return VoiceCommand.QueryTasks(completed = true)
        }
        if (isDeleteSelectedDateTasks(spoken)) {
            return VoiceCommand.DeleteSelectedDateTasks
        }
        extractReminder(spoken)?.let { return it }

        extractAddTask(spoken)?.let { return VoiceCommand.AddTask(it) }
        extractCompleteTask(spoken)?.let { return VoiceCommand.CompleteTask(it) }

        return VoiceCommand.Unknown(spoken)
    }

    private fun isQuery(text: String, completed: Boolean): Boolean {
        val normalized = text.replace(" ", "")
        val keywords = if (completed) {
            listOf(
                "告诉我今天完成的任务",
                "告诉我今天已完成的任务",
                "告诉我今天完成的人物",
                "今天完成了什么",
                "今天做完了什么",
                "今天已完成任务"
            )
        } else {
            listOf(
                "告诉我今天还没完成的任务",
                "告诉我今天没完成的任务",
                "告诉我今天未完成的任务",
                "告诉我今天还没完成的人物",
                "告诉我今天未完成的人物",
                "今天还有什么没完成",
                "今天还没完成任务",
                "今天未完成任务",
                "今天还有什么任务"
            )
        }
        return keywords.any { normalized.contains(it) }
    }

    private fun isDeleteSelectedDateTasks(text: String): Boolean {
        val normalized = text.replace(" ", "")
        val keywords = listOf(
            "删除今天的所有任务",
            "删除今天所有任务",
            "删除当天的所有任务",
            "删除当天所有任务",
            "删除当前页面的所有任务",
            "删除当前页面所有任务",
            "删除当前页的所有任务",
            "删除当前页所有任务",
            "删掉今天的所有任务",
            "删掉今天所有任务",
            "删掉当天的所有任务",
            "删掉当天所有任务",
            "清空今天的所有任务",
            "清空今天所有任务",
            "清空当天的所有任务",
            "清空当天所有任务",
            "清空当前页面的所有任务",
            "清空当前页面所有任务"
        )
        return keywords.any { normalized.contains(it) }
    }

    private fun extractAddTask(text: String): String? {
        val normalized = text.replace(" ", "")
        val prefixRegex = Regex("^(记录一下|记录|记一下|记下|添加任务|添加|新增任务|新增|帮我记录|帮我记下)")
        if (!prefixRegex.containsMatchIn(normalized)) {
            return null
        }

        var content = normalized.replaceFirst(prefixRegex, "")
        content = content.replaceFirst(Regex("^(我今天|今天)(要|想|需要|准备)?"), "")
        content = content.replaceFirst(Regex("^完成(?=.{2,})"), "")
        content = content.replaceFirst(Regex("^去(?=.{2,})"), "")
        return content.ifBlank { null }
    }

    private fun extractCompleteTask(text: String): String? {
        val normalized = text.replace(" ", "")

        Regex("^把(.+?)(标记)?完成(了)?$").matchEntire(normalized)?.let { return it.groupValues[1] }
        Regex("^(.+?)(标记)?完成(了)?$").matchEntire(normalized)?.let { return it.groupValues[1] }
        Regex("^完成(.+)$").matchEntire(normalized)?.let { return it.groupValues[1] }

        return null
    }

    private fun extractReminder(text: String): VoiceCommand.CreateReminder? {
        val normalized = text.replace(" ", "")

        Regex("^(\\d{1,3})分钟后提醒我(.+)$").matchEntire(normalized)?.let { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return null
            val content = match.groupValues[2]
            val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
            return VoiceCommand.CreateReminder(content, triggerAtMillis)
        }

        Regex("^今天(上午|中午|下午|晚上)?(\\d{1,2})点(?:(\\d{1,2})分?)?提醒我(.+)$").matchEntire(normalized)?.let { match ->
            val period = match.groupValues[1]
            val hour = match.groupValues[2].toIntOrNull() ?: return null
            val minute = match.groupValues[3].ifBlank { "0" }.toIntOrNull() ?: return null
            val content = match.groupValues[4]
            val actualHour = convertHour(period, hour) ?: return null
            val triggerAt = LocalDateTime.of(LocalDate.now(), LocalTime.of(actualHour, minute))
            val triggerAtMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return VoiceCommand.CreateReminder(content, triggerAtMillis)
        }

        return null
    }

    private fun convertHour(period: String, hour: Int): Int? {
        if (hour !in 0..23) return null

        return when (period) {
            "上午" -> when (hour) {
                in 1..11 -> hour
                12 -> 0
                else -> hour
            }

            "中午" -> when (hour) {
                in 1..10 -> hour + 12
                11, 12 -> hour
                else -> null
            }

            "下午", "晚上" -> when (hour) {
                in 1..11 -> hour + 12
                12 -> 12
                else -> null
            }

            else -> hour
        }
    }
}
