package com.example.core.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class NativeAudioFeedbackManager {

    fun playSuccessTone() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 150
                // Double chime: first half 523Hz (C5), second half 659Hz (E5)
                val size = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(size)
                val half = size / 2
                
                for (i in 0 until half) {
                    val t = i.toDouble() / sampleRate
                    buffer[i] = (Math.sin(2.0 * Math.PI * 523.0 * t) * Short.MAX_VALUE * 0.3).toInt().toShort()
                }
                for (i in half until size) {
                    val t = (i - half).toDouble() / sampleRate
                    buffer[i] = (Math.sin(2.0 * Math.PI * 659.0 * t) * Short.MAX_VALUE * 0.3).toInt().toShort()
                }
                
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, size)
                track.play()
                Thread.sleep(durationMs.toLong() + 30)
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playErrorTone() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 300
                // Triple buzzer: 220Hz -> 165Hz -> 110Hz
                val size = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(size)
                val segment = size / 3
                
                for (i in 0 until segment) {
                    val t = i.toDouble() / sampleRate
                    buffer[i] = (Math.sin(2.0 * Math.PI * 220.0 * t) * Short.MAX_VALUE * 0.3).toInt().toShort()
                }
                for (i in segment until segment * 2) {
                    val t = (i - segment).toDouble() / sampleRate
                    buffer[i] = (Math.sin(2.0 * Math.PI * 165.0 * t) * Short.MAX_VALUE * 0.3).toInt().toShort()
                }
                for (i in segment * 2 until size) {
                    val t = (i - segment * 2).toDouble() / sampleRate
                    buffer[i] = (Math.sin(2.0 * Math.PI * 110.0 * t) * Short.MAX_VALUE * 0.3).toInt().toShort()
                }
                
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, size)
                track.play()
                Thread.sleep(durationMs.toLong() + 30)
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun release() {
        // AudioTrack instances are short lived and released automatically
    }
}

object QualityMatcher {
    fun calculateSimilarity(s1: String, s2: String): Float {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0) return if (len2 == 0) 1.0f else 0.0f
        
        val dp = IntArray(len2 + 1) { it }
        for (i in 1..len1) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..len2) {
                val temp = dp[j]
                val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
                prev = temp
            }
        }
        val maxLen = maxOf(len1, len2)
        return 1.0f - (dp[len2].toFloat() / maxLen)
    }
}
