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
            speak("I didn't catch that. Please try again.")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            viewModel.onListeningFailed("Microphone permission is required for voice commands.")
            speak("Microphone permission is required for voice commands.")
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
                    onSubmitCommand = viewModel::handleVoiceText,
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
            val result = textToSpeech?.setLanguage(Locale.US)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                val fallback = textToSpeech?.setLanguage(Locale.UK)
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
        val languageTag = Locale.US.toLanguageTag()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a task command")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        if (intent.resolveActivity(packageManager) == null) {
            val message = "English speech recognition is not available on this device."
            viewModel.onListeningFailed(message)
            speak(message)
            return
        }

        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            val message = "English speech recognition is not available on this device."
            viewModel.onListeningFailed(message)
            speak(message)
        }
    }

    private fun speak(message: String) {
        if (ttsReady) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "sayit-tts")
        }
    }
}
