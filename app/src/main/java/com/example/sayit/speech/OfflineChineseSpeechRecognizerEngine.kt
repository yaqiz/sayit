package com.example.sayit.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.util.concurrent.atomic.AtomicBoolean

class OfflineChineseSpeechRecognizerEngine(
    private val context: Context
) : SpeechRecognizerEngine {
    override val displayName: String = "离线中文语音"

    private val sampleRate = 16000
    private val maxRecordingMillis = 6000L
    private val modelDir = "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23"
    private val isRunning = AtomicBoolean(false)

    @Volatile
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var workerThread: Thread? = null

    @Volatile
    private var recognizer: OnlineRecognizer? = null

    override fun startRecognition(onResult: (SpeechRecognitionResult) -> Unit): SpeechStartRequest {
        if (isRunning.get()) {
            return SpeechStartRequest.ImmediateFailure("离线语音识别正在进行中，请稍后再试。")
        }

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return SpeechStartRequest.ImmediateFailure("需要麦克风权限才能使用离线语音识别。")
        }

        val localRecognizer = try {
            recognizer ?: createRecognizer().also { recognizer = it }
        } catch (error: Throwable) {
            return SpeechStartRequest.ImmediateFailure(
                "离线语音模型加载失败：${error.message ?: "未知错误"}"
            )
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            return SpeechStartRequest.ImmediateFailure("无法初始化本地录音缓冲区。")
        }

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return SpeechStartRequest.ImmediateFailure("无法打开麦克风进行离线识别。")
        }

        audioRecord = recorder
        isRunning.set(true)

        workerThread = Thread {
            runRecognition(localRecognizer, recorder, minBuffer, onResult)
        }.apply {
            name = "offline-chinese-asr"
            start()
        }

        return SpeechStartRequest.Started
    }

    override fun parseActivityResult(data: Intent?): SpeechRecognitionResult {
        return SpeechRecognitionResult.Error("离线中文语音不依赖系统语音返回结果。")
    }

    override fun stopRecognition() {
        isRunning.set(false)
        audioRecord?.runCatching {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
        }
    }

    private fun runRecognition(
        recognizer: OnlineRecognizer,
        recorder: AudioRecord,
        minBuffer: Int,
        onResult: (SpeechRecognitionResult) -> Unit
    ) {
        var stream: OnlineStream? = null

        try {
            recorder.startRecording()
            stream = recognizer.createStream("")

            val shortBuffer = ShortArray((minBuffer / 2).coerceAtLeast(1600))
            val startAt = SystemClock.elapsedRealtime()
            var finalText = ""

            while (isRunning.get() && SystemClock.elapsedRealtime() - startAt < maxRecordingMillis) {
                val read = recorder.read(shortBuffer, 0, shortBuffer.size)
                if (read <= 0) {
                    continue
                }

                val samples = FloatArray(read) { index -> shortBuffer[index] / 32768.0f }
                stream.acceptWaveform(samples, sampleRate)

                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream)
                }

                val text = recognizer.getResult(stream).text.orEmpty().trim()
                if (text.isNotBlank()) {
                    finalText = text
                }

                if (recognizer.isEndpoint(stream) && finalText.isNotBlank()) {
                    break
                }
            }

            val tailPaddings = FloatArray((0.8f * sampleRate).toInt())
            stream.acceptWaveform(tailPaddings, sampleRate)
            while (recognizer.isReady(stream)) {
                recognizer.decode(stream)
            }

            val text = recognizer.getResult(stream).text.orEmpty().trim().ifBlank { finalText }
            onResult(
                if (text.isBlank()) {
                    SpeechRecognitionResult.NoMatch("离线识别没有听清楚，请再试一次。")
                } else {
                    SpeechRecognitionResult.Success(text)
                }
            )
        } catch (error: Throwable) {
            onResult(
                SpeechRecognitionResult.Error(
                    "离线语音识别失败：${error.message ?: "未知错误"}"
                )
            )
        } finally {
            isRunning.set(false)
            stream?.release()
            recorder.runCatching {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
            }
            recorder.release()
            if (audioRecord === recorder) {
                audioRecord = null
            }
            workerThread = null
        }
    }

    private fun createRecognizer(): OnlineRecognizer {
        val transducerConfig = OnlineTransducerModelConfig().apply {
            encoder = "$modelDir/encoder-epoch-99-avg-1.int8.onnx"
            decoder = "$modelDir/decoder-epoch-99-avg-1.onnx"
            joiner = "$modelDir/joiner-epoch-99-avg-1.int8.onnx"
        }

        val modelConfig = OnlineModelConfig().apply {
            tokens = "$modelDir/tokens.txt"
            transducer = transducerConfig
            modelType = "zipformer"
            debug = false
        }

        val config = OnlineRecognizerConfig().apply {
            this.modelConfig = modelConfig
        }

        return OnlineRecognizer(context.assets, config)
    }
}
