package com.example.sayit.speech

sealed interface SpeechRecognitionResult {
    data class Success(val text: String) : SpeechRecognitionResult
    data class NoMatch(val message: String) : SpeechRecognitionResult
    data class Error(val message: String) : SpeechRecognitionResult
}
