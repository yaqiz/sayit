package com.example.sayit.speech

import android.content.Context

enum class SpeechMode {
    SYSTEM_CHINESE,
    OFFLINE_CHINESE
}

object SpeechRecognizerFactory {
    fun create(context: Context, mode: SpeechMode): SpeechRecognizerEngine {
        return when (mode) {
            SpeechMode.SYSTEM_CHINESE -> SystemChineseSpeechRecognizerEngine(context.packageManager)
            SpeechMode.OFFLINE_CHINESE -> OfflineChineseSpeechRecognizerEngine(context)
        }
    }
}
