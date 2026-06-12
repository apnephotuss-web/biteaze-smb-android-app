package com.example.core.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceEngineLifecycleController(private val context: Context) : DefaultLifecycleObserver {
    private var standbyRecognizer: SpeechRecognizer? = null
    private var activeRecognizer: SpeechRecognizer? = null
    
    private val audioFeedback = NativeAudioFeedbackManager()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var autoResetJob: Job? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var isContinuousListeningEnabled = false
    private var isQuietRestarting = false
    private var vadJob: Job? = null

    private val wakeWords = setOf(
        "new order", "new-order", "new borders", "new border", "near order", "now order", "know order", "no order", "yo order", "newer order", "nu order",
        "navo order", "navo-order", "nawo order", "navu order", "nava order",
        "naya order", "naya-order", "nyaya order", "naye order"
    )

    private val completionTriggers = setOf(
        "done", "dan", "dun", "done done", "completo", "complete"
    )

    init {
        // Standby continuous mode disabled. Voice only starts manually on button click
    }

    fun onPermissionGranted() {
        if (isContinuousListeningEnabled && _voiceState.value is VoiceState.Idle) {
            startStandbyListening()
        }
    }

    fun setContinuousListening(enabled: Boolean) {
        isContinuousListeningEnabled = enabled
        if (enabled) {
            if (_voiceState.value is VoiceState.Idle) {
                startStandbyListening()
            }
        } else {
            stopStandby()
        }
    }

    private fun startStandbyListening() {
        if (!isContinuousListeningEnabled) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d("VoiceEngine", "VAD cannot start: RECORD_AUDIO permission not granted")
            return
        }
        
        vadJob?.cancel()
        vadJob = coroutineScope.launch(Dispatchers.Default) {
            var audioRecord: AudioRecord? = null
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else 2048
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("VoiceEngine", "AudioRecord not initialized")
                    return@launch
                }
                
                audioRecord?.startRecording()
                Log.d("VoiceEngine", "VAD silent monitor started successfully")
                
                val buffer = ShortArray(1024)
                var consecutiveSpeeches = 0
                
                while (isContinuousListeningEnabled && _voiceState.value is VoiceState.Idle) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = Math.sqrt(sum / read)
                        // Calibrate RMS threshold: silent room is typically < 100, voice starts around 300-800+
                        if (rms > 400.0) {
                            consecutiveSpeeches++
                            // Require a minimum duration of spoken sound to run SpeechRecognizer to avoid short pops
                            if (consecutiveSpeeches >= 10) { 
                                Log.d("VoiceEngine", "Voice activity detected (RMS: $rms). Checking for wake words...")
                                consecutiveSpeeches = 0
                                
                                // Temporarily stop VAD to let SpeechRecognizer use the microphone
                                try {
                                    audioRecord?.stop()
                                    audioRecord?.release()
                                } catch (ex: Exception) {
                                    // Ignore
                                }
                                audioRecord = null
                                
                                // Trigger transient speech recognizer check
                                runSpeechRecognizerWakeCheck()
                                break
                            }
                        } else {
                            consecutiveSpeeches = maxOf(0, consecutiveSpeeches - 1)
                        }
                    }
                    delay(30) // sleep a bit to keep CPU usage extremely low (~0-1%)
                }
            } catch (e: Exception) {
                Log.e("VoiceEngine", "Error in VAD: ${e.message}")
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun runSpeechRecognizerWakeCheck() {
        coroutineScope.launch(Dispatchers.Main) {
            if (_voiceState.value !is VoiceState.Idle) return@launch
            
            try {
                if (standbyRecognizer == null) {
                    standbyRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                
                standbyRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    
                    override fun onError(error: Int) {
                        Log.d("VoiceEngine", "Wake check recognizer error: $error")
                        silentlyGoBackToVad()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (checkWakeWord(matches)) {
                            // Wake word detected! Trigger active listening
                            triggerActiveListening()
                        } else {
                            silentlyGoBackToVad()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                
                standbyRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceEngine", "Failed starting wake check SpeechRecognizer: ${e.message}")
                silentlyGoBackToVad()
            }
        }
    }

    private fun silentlyGoBackToVad() {
        try {
            standbyRecognizer?.stopListening()
            standbyRecognizer?.cancel()
            standbyRecognizer?.destroy()
            standbyRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
        coroutineScope.launch {
            delay(1000)
            if (_voiceState.value is VoiceState.Idle && isContinuousListeningEnabled) {
                startStandbyListening()
            }
        }
    }

    private fun checkWakeWord(matches: ArrayList<String>?): Boolean {
        if (matches.isNullOrEmpty()) return false
        for (match in matches) {
            val lowercaseMatch = match.lowercase()
            for (word in wakeWords) {
                if (lowercaseMatch.contains(word)) {
                    return true
                }
            }
        }
        return false
    }

    private fun triggerActiveListening() {
        stopStandby()
        audioFeedback.playSuccessTone()
        startListening()
    }

    private fun stopStandby() {
        vadJob?.cancel()
        vadJob = null
        try {
            standbyRecognizer?.stopListening()
            standbyRecognizer?.cancel()
            standbyRecognizer?.destroy()
            standbyRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startListening() {
        stopStandby()
        autoResetJob?.cancel()
        
        _voiceState.value = VoiceState.Listening

        coroutineScope.launch(Dispatchers.Main) {
            try {
                if (activeRecognizer == null) {
                    activeRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                }

                activeRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.Processing
                    }

                    override fun onError(error: Int) {
                        Log.d("VoiceEngine", "Active error code: $error")
                        val errMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech heard"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout waiting"
                            else -> "Interrupted"
                        }
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            _voiceState.value = VoiceState.Error(errMsg)
                            startAutoResetTimer()
                        } else {
                            triggerCompletionError(errMsg)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            triggerCompletionSuccess(text)
                        } else {
                            triggerCompletionError("No speech details captured")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.lowercase() ?: ""
                        if (text.isNotBlank()) {
                            // Check for Active Completion triggers: done, dan, complete, etc.
                            for (completed in completionTriggers) {
                                if (text.endsWith(completed) || text.contains("\\b$completed\\b".toRegex())) {
                                    activeRecognizer?.stopListening()
                                    _voiceState.value = VoiceState.Processing
                                    triggerCompletionSuccess(text)
                                    break
                                }
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                activeRecognizer?.startListening(intent)
            } catch (e: Exception) {
                triggerCompletionError("Failed to start voice: ${e.message}")
            }
        }
    }

    private fun triggerCompletionSuccess(text: String) {
        _voiceState.value = VoiceState.Success(text)
        startAutoResetTimer()
    }

    private fun triggerCompletionError(message: String) {
        audioFeedback.playErrorTone()
        _voiceState.value = VoiceState.Error(message)
        startAutoResetTimer()
    }

    private fun startAutoResetTimer() {
        autoResetJob?.cancel()
        autoResetJob = coroutineScope.launch {
            delay(1500)
            resetState()
        }
    }

    fun resetState() {
        _voiceState.value = VoiceState.Idle
        try {
            activeRecognizer?.stopListening()
            activeRecognizer?.cancel()
            activeRecognizer?.destroy()
            activeRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (isContinuousListeningEnabled) {
            startStandbyListening()
        }
    }

    fun stopListening() {
        resetState()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        isContinuousListeningEnabled = false
        stopStandby()
        try {
            activeRecognizer?.stopListening()
            activeRecognizer?.cancel()
            activeRecognizer?.destroy()
            activeRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioFeedback.release()
    }
}
