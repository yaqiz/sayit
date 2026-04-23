package com.example.sayit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sayit.data.ReminderRepository
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
    val speechEngineLabel: String = "",
    val screenMode: ScreenMode = ScreenMode.TASK_LIST,
    val isListening: Boolean = false,
    val isProcessingVoice: Boolean = false,
    val lastHeardText: String = "",
    val statusMessage: String = "点击按钮后直接说：记录小红书文案"
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val reminderRepository: ReminderRepository
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
                statusMessage = "正在处理：$text"
            )
        }
        viewModelScope.launch {
            val selectedDate = uiState.value.selectedDate
            val result = when (val command = VoiceCommandParser.parse(text)) {
                is VoiceCommand.AddTask -> repository.addTaskForDate(command.title, selectedDate)
                is VoiceCommand.CompleteTask -> repository.markTaskCompletedForDate(command.query, selectedDate)
                is VoiceCommand.QueryTasks -> repository.buildSummaryForDate(command.completed, selectedDate)
                VoiceCommand.DeleteSelectedDateTasks -> repository.deleteAllTasksForDate(selectedDate)
                is VoiceCommand.CreateReminder -> reminderRepository.createReminder(command.content, command.triggerAtMillis)
                is VoiceCommand.Unknown -> VoiceActionResult(
                    "没有识别到有效指令。可以说：记录小红书文案，告诉我今天还没完成的任务，或者：5分钟后提醒我去看烤箱。"
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
                statusMessage = "正在听，请直接说任务指令..."
            )
        }
    }

    fun setSpeechEngineLabel(label: String) {
        _uiState.update { it.copy(speechEngineLabel = label) }
    }

    fun onListeningFinishedWithoutResult() {
        _uiState.update {
            it.copy(
                isListening = false,
                isProcessingVoice = false,
                statusMessage = "没有听清楚，请再试一次。"
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
                "已完成：${task.title}"
            } else {
                "已改为未完成：${task.title}"
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
                if (current.selectedDate == today) {
                    false
                } else {
                    _uiState.update { it.copy(screenMode = ScreenMode.CALENDAR) }
                    true
                }
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
    private val repository: TaskRepository,
    private val reminderRepository: ReminderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, reminderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
