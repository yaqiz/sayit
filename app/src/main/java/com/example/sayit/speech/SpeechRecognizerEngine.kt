package com.example.sayit.speech

import android.content.Intent

sealed interface SpeechStartRequest {
    data class LaunchIntent(val intent: Intent) : SpeechStartRequest
    data object Started : SpeechStartRequest
    data class ImmediateFailure(val message: String) : SpeechStartRequest
}

interface SpeechRecognizerEngine {
    val displayName: String

    fun startRecognition(onResult: (SpeechRecognitionResult) -> Unit): SpeechStartRequest

    fun parseActivityResult(data: Intent?): SpeechRecognitionResult

    fun stopRecognition() = Unit
}
