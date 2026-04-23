package com.example.sayit.speech

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent

class SystemChineseSpeechRecognizerEngine(
    private val packageManager: PackageManager
) : SpeechRecognizerEngine {
    override val displayName: String = "系统中文语音"

    override fun startRecognition(onResult: (SpeechRecognitionResult) -> Unit): SpeechStartRequest {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请直接说出任务命令")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        return try {
            if (intent.resolveActivity(packageManager) == null) {
                SpeechStartRequest.ImmediateFailure("当前设备没有可用的系统中文语音识别服务。")
            } else {
                SpeechStartRequest.LaunchIntent(intent)
            }
        } catch (_: ActivityNotFoundException) {
            SpeechStartRequest.ImmediateFailure("当前设备没有可用的系统中文语音识别服务。")
        }
    }

    override fun parseActivityResult(data: Intent?): SpeechRecognitionResult {
        val spoken = data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        return if (spoken.isNullOrBlank()) {
            SpeechRecognitionResult.NoMatch("没有听清楚，请再试一次。")
        } else {
            SpeechRecognitionResult.Success(spoken)
        }
    }
}
