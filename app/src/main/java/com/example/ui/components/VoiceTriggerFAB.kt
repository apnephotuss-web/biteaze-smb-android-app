package com.example.ui.components

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.example.core.database.AppDatabase
import com.example.core.voice.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceTriggerFAB(
    mode: OperationMode,
    onIntentRecognized: (VoiceCommandIntent) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val voiceController = remember { VoiceEngineLifecycleController(context) }
    val parser = remember { LocalCommandParser() }
    val audioFeedback = remember { NativeAudioFeedbackManager() }

    val voiceState by voiceController.voiceState.collectAsState()

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(voiceController)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(voiceController)
            audioFeedback.release()
        }
    }

    LaunchedEffect(micPermissionState.status.isGranted) {
        if (micPermissionState.status.isGranted) {
            voiceController.onPermissionGranted()
        }
    }

    LaunchedEffect(voiceState) {
        when (val state = voiceState) {
            is VoiceState.Success -> {
                scope.launch(Dispatchers.Default) {
                    val db = (context.applicationContext as com.example.SyncPosApplication).database
                    val inventory = db.productDao().getActiveVoiceIndexingCatalog()
                    
                    val intent = parser.parseCommand(state.commandText, mode, inventory)
                    
                    launch(Dispatchers.Main) {
                        if (intent is VoiceCommandIntent.Unrecognized) {
                            audioFeedback.playErrorTone()
                        } else {
                            audioFeedback.playSuccessTone()
                        }
                        onIntentRecognized(intent)
                        voiceController.resetState()
                    }
                }
            }
            is VoiceState.Error -> {
                voiceController.resetState()
            }
            else -> {}
        }
    }

    val isListening = voiceState is VoiceState.Listening

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .background(
                color = if (isListening) Color(0xFF10B981) else Color(0xFFF97316),
                shape = CircleShape
            )
            .clickable {
                if (!micPermissionState.status.isGranted) {
                    micPermissionState.launchPermissionRequest()
                } else {
                    if (isListening) {
                        voiceController.stopListening()
                    } else {
                        voiceController.startListening()
                    }
                }
            }
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Command",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
