package com.example.sayit

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.sayit.data.AppDatabase
import com.example.sayit.data.ReminderRepository
import com.example.sayit.data.TaskRepository
import com.example.sayit.reminder.ReminderAlarmScheduler
import com.example.sayit.reminder.ReminderNotifier
import com.example.sayit.speech.SpeechMode
import com.example.sayit.speech.SpeechRecognitionResult
import com.example.sayit.speech.SpeechRecognizerEngine
import com.example.sayit.speech.SpeechRecognizerFactory
import com.example.sayit.speech.SpeechStartRequest
import com.example.sayit.ui.SayitScreen
import com.example.sayit.ui.SayitTheme
import com.example.sayit.ui.TaskViewModel
import com.example.sayit.ui.TaskViewModelFactory
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val speechMode = SpeechMode.OFFLINE_CHINESE
    private val viewModel: TaskViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        TaskViewModelFactory(
            repository = TaskRepository(database.taskDao()),
            reminderRepository = ReminderRepository(
                reminderDao = database.reminderDao(),
                reminderScheduler = ReminderAlarmScheduler(applicationContext)
            )
        )
    }

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var speechEngine: SpeechRecognizerEngine

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleRecognitionResult(speechEngine.parseActivityResult(result.data))
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            viewModel.onListeningFailed("需要麦克风权限才能使用语音。")
            speak("需要麦克风权限才能使用语音。")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.onListeningFailed("通知权限未开启，到点后可能无法显示提醒。")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this, this)
        speechEngine = SpeechRecognizerFactory.create(this, speechMode)
        viewModel.setSpeechEngineLabel(speechEngine.displayName)
        ReminderNotifier.ensureChannel(this)
        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.speechMessages.collect { message ->
                    speak(message)
                }
            }
        }

        setContent {
            SayitTheme {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                SayitScreen(
                    uiState = state,
                    onStartListening = ::startVoiceFlow,
                    onToggleTask = viewModel::setTaskCompletion,
                    onOpenCalendar = viewModel::openCalendar,
                    onOpenDate = viewModel::openTasksForDate,
                    onBackPressed = {
                        if (!viewModel.handleBack()) {
                            finish()
                        }
                    }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.SIMPLIFIED_CHINESE)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                val fallback = textToSpeech?.setLanguage(Locale.CHINA)
                ttsReady = fallback != TextToSpeech.LANG_MISSING_DATA &&
                    fallback != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    override fun onDestroy() {
        speechEngine.stopRecognition()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun startVoiceFlow() {
        viewModel.onListeningStarted()
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> {
                launchSpeechRecognizer()
            }

            else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchSpeechRecognizer() {
        when (val startRequest = speechEngine.startRecognition(::handleRecognitionResult)) {
            is SpeechStartRequest.LaunchIntent -> {
                try {
                    speechLauncher.launch(startRequest.intent)
                } catch (_: ActivityNotFoundException) {
                    val message = "当前设备没有可用的语音识别服务。"
                    viewModel.onListeningFailed(message)
                    speak(message)
                }
            }

            is SpeechStartRequest.ImmediateFailure -> {
                viewModel.onListeningFailed(startRequest.message)
                speak(startRequest.message)
            }

            SpeechStartRequest.Started -> Unit
        }
    }

    private fun handleRecognitionResult(result: SpeechRecognitionResult) {
        runOnUiThread {
            when (result) {
                is SpeechRecognitionResult.Success -> viewModel.handleVoiceText(result.text)
                is SpeechRecognitionResult.NoMatch -> {
                    viewModel.onListeningFinishedWithoutResult()
                    speak(result.message)
                }

                is SpeechRecognitionResult.Error -> {
                    viewModel.onListeningFailed(result.message)
                    speak(result.message)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun speak(message: String) {
        if (ttsReady) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "sayit-tts")
        }
    }
}
