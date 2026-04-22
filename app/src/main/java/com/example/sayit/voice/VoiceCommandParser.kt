package com.example.sayit.voice

sealed interface VoiceCommand {
    data class AddTask(val title: String) : VoiceCommand
    data class CompleteTask(val query: String) : VoiceCommand
    data class QueryTasks(val completed: Boolean) : VoiceCommand
    data object DeleteTodayTasks : VoiceCommand
    data class Unknown(val originalText: String) : VoiceCommand
}

object VoiceCommandParser {
    fun parse(text: String): VoiceCommand {
        val spoken = text.trim()
        if (spoken.isBlank()) {
            return VoiceCommand.Unknown(text)
        }

        if (isOutstandingQuery(spoken)) {
            return VoiceCommand.QueryTasks(completed = false)
        }
        if (isCompletedQuery(spoken)) {
            return VoiceCommand.QueryTasks(completed = true)
        }
        if (isDeleteTodayTasks(spoken)) {
            return VoiceCommand.DeleteTodayTasks
        }

        extractAddTask(spoken)?.let { return VoiceCommand.AddTask(it) }
        extractCompleteTask(spoken)?.let { return VoiceCommand.CompleteTask(it) }

        return VoiceCommand.Unknown(spoken)
    }

    private fun isOutstandingQuery(text: String): Boolean {
        val normalized = normalize(text)
        val keywords = listOf(
            "tell me about my today's outstanding tasks",
            "tell me my today's outstanding tasks",
            "tell me my outstanding tasks for today",
            "what are my outstanding tasks today",
            "what are my today's outstanding tasks",
            "tell me today's outstanding tasks",
            "tell me my unfinished tasks today",
            "what are my unfinished tasks today"
        )
        return keywords.any { normalized.contains(normalize(it)) }
    }

    private fun isCompletedQuery(text: String): Boolean {
        val normalized = normalize(text)
        val keywords = listOf(
            "tell me about my today's completed tasks",
            "tell me my today's completed tasks",
            "tell me my completed tasks for today",
            "what are my completed tasks today",
            "tell me today's completed tasks",
            "what did i complete today"
        )
        return keywords.any { normalized.contains(normalize(it)) }
    }

    private fun isDeleteTodayTasks(text: String): Boolean {
        val normalized = normalize(text)
        val keywords = listOf(
            "delete all my tasks for today",
            "delete all my today tasks",
            "delete all tasks for today",
            "delete today's tasks",
            "clear all my tasks for today",
            "clear today's tasks"
        )
        return keywords.any { normalized.contains(normalize(it)) }
    }

    private fun extractAddTask(text: String): String? {
        val trimmed = text.trim()
        val patterns = listOf(
            Regex("^i need to\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^i have to\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^remind me to\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^add task\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^add\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^record\\s+(.+)$", RegexOption.IGNORE_CASE)
        )
        return patterns.firstNotNullOfOrNull { regex ->
            regex.matchEntire(trimmed)?.groupValues?.get(1)?.trim()?.ifBlank { null }
        }
    }

    private fun extractCompleteTask(text: String): String? {
        val trimmed = text.trim()
        val patterns = listOf(
            Regex("^(.+?)\\s+done$", RegexOption.IGNORE_CASE),
            Regex("^mark\\s+(.+?)\\s+as\\s+done$", RegexOption.IGNORE_CASE),
            Regex("^complete\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^mark\\s+(.+?)\\s+completed$", RegexOption.IGNORE_CASE)
        )
        return patterns.firstNotNullOfOrNull { regex ->
            regex.matchEntire(trimmed)?.groupValues?.get(1)?.trim()?.ifBlank { null }
        }
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
