package com.example.sayit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sayit.data.TaskEntity
import com.example.sayit.data.TaskRepository
import com.example.sayit.data.VoiceActionResult
import com.example.sayit.voice.VoiceCommand
import com.example.sayit.voice.VoiceCommandParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class ScreenMode {
    TASK_LIST,
    CALENDAR
}

data class DayTaskSummary(
    val totalCount: Int = 0,
    val completedCount: Int = 0
) {
    val hasTasks: Boolean get() = totalCount > 0
    val allCompleted: Boolean get() = hasTasks && completedCount == totalCount
    val hasPending: Boolean get() = totalCount > completedCount
}

data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val tasksByDate: Map<LocalDate, DayTaskSummary> = emptyMap(),
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonths: List<YearMonth> = buildVisibleMonths(),
    val screenMode: ScreenMode = ScreenMode.TASK_LIST,
    val isListening: Boolean = false,
    val isProcessingVoice: Boolean = false,
    val lastHeardText: String = "",
    val statusMessage: String = "Tap the button and say something like: I need to go to supermarket"
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {
    private val today = LocalDate.now()
    private val calendarStart = today.minusMonths(12).withDayOfMonth(1)
    private val calendarEnd = today.plusMonths(12).withDayOfMonth(today.plusMonths(12).lengthOfMonth())
    private val selectedDateFlow = MutableStateFlow(today)

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _speechMessages = MutableSharedFlow<String>()
    val speechMessages: SharedFlow<String> = _speechMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                repository.observeTasksForDate(date)
            }.collect { tasks ->
                _uiState.update { current -> current.copy(tasks = tasks) }
            }
        }

        viewModelScope.launch {
            repository.observeTasksBetween(calendarStart, calendarEnd).collect { tasks ->
                val tasksByDate = tasks
                    .groupBy { LocalDate.parse(it.createdDate) }
                    .mapValues { (_, dayTasks) ->
                        DayTaskSummary(
                            totalCount = dayTasks.size,
                            completedCount = dayTasks.count { it.isCompleted }
                        )
                    }
                _uiState.update { current -> current.copy(tasksByDate = tasksByDate) }
            }
        }
    }

    fun handleVoiceText(text: String) {
        _uiState.update {
            it.copy(
                lastHeardText = text,
                isListening = false,
                isProcessingVoice = true,
                statusMessage = "Processing: $text"
            )
        }
        viewModelScope.launch {
            val selectedDate = uiState.value.selectedDate
            val result = when (val command = VoiceCommandParser.parse(text)) {
                is VoiceCommand.AddTask -> repository.addTaskForDate(command.title, selectedDate)
                is VoiceCommand.CompleteTask -> repository.markTaskCompletedForDate(command.query, selectedDate)
                is VoiceCommand.QueryTasks -> repository.buildSummaryForDate(command.completed, selectedDate)
                VoiceCommand.DeleteTodayTasks -> repository.deleteAllTasksForToday()
                is VoiceCommand.Unknown -> VoiceActionResult(
                    "I couldn't recognize a supported command. Try: I need to go to supermarket."
                )
            }
            publishMessage(result.message, speak = true)
        }
    }

    fun onListeningStarted() {
        _uiState.update {
            it.copy(
                isListening = true,
                isProcessingVoice = false,
                statusMessage = "Listening... Say your task command."
            )
        }
    }

    fun onListeningFinishedWithoutResult() {
        _uiState.update {
            it.copy(
                isListening = false,
                isProcessingVoice = false,
                statusMessage = "I didn't catch that. Please try again."
            )
        }
    }

    fun onListeningFailed(message: String) {
        _uiState.update {
            it.copy(
                isListening = false,
                isProcessingVoice = false,
                statusMessage = message
            )
        }
    }

    fun setTaskCompletion(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompletion(task.id, completed)
            val message = if (completed) {
                "Marked as done: ${task.title}"
            } else {
                "Marked as outstanding: ${task.title}"
            }
            publishMessage(message, speak = false)
        }
    }

    fun openCalendar() {
        _uiState.update { it.copy(screenMode = ScreenMode.CALENDAR) }
    }

    fun openTasksForDate(date: LocalDate) {
        selectedDateFlow.value = date
        _uiState.update { it.copy(selectedDate = date, screenMode = ScreenMode.TASK_LIST) }
    }

    fun handleBack(): Boolean {
        val current = _uiState.value
        return when (current.screenMode) {
            ScreenMode.TASK_LIST -> {
                _uiState.update { it.copy(screenMode = ScreenMode.CALENDAR) }
                true
            }

            ScreenMode.CALENDAR -> {
                selectedDateFlow.value = today
                _uiState.update {
                    it.copy(
                        selectedDate = today,
                        screenMode = ScreenMode.TASK_LIST
                    )
                }
                true
            }
        }
    }

    private suspend fun publishMessage(message: String, speak: Boolean) {
        _uiState.update {
            it.copy(
                statusMessage = message,
                isListening = false,
                isProcessingVoice = false
            )
        }
        if (speak) {
            _speechMessages.emit(message)
        }
    }
}

private fun buildVisibleMonths(): List<YearMonth> {
    val current = YearMonth.now()
    return (-12..12).map { offset -> current.plusMonths(offset.toLong()) }
}

class TaskViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
