package com.example.sayit

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
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
import com.example.sayit.data.TaskRepository
import com.example.sayit.ui.SayitScreen
import com.example.sayit.ui.SayitTheme
import com.example.sayit.ui.TaskViewModel
import com.example.sayit.ui.TaskViewModelFactory
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(
            TaskRepository(
                AppDatabase.getInstance(applicationContext).taskDao()
            )
        )
    }

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (!spoken.isNullOrBlank()) {
            viewModel.handleVoiceText(spoken)
        } else {
            viewModel.onListeningFinishedWithoutResult()
            speak("没有听清楚，请再试一次。")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            viewModel.onListeningFailed("需要麦克风权限才能使用语音。")
            speak("需要麦克风权限才能使用语音。")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this, this)

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
                    onBackToCalendar = viewModel::handleBack
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

            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请直接说出任务命令")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.onListeningFailed("当前设备没有可用的语音识别服务。")
            speak("当前设备没有可用的语音识别服务。")
        }
    }

    private fun speak(message: String) {
        if (ttsReady) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "sayit-tts")
        }
    }
}
