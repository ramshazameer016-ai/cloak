
package com.example.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioChunkRecorder(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onError: ((String) -> Unit)? = null,
    private val onChunkReady: (chunkIndex: Int, file: File) -> Unit
) {
    companion object {
        private const val TAG = "AudioChunkRecorder"
        const val CHUNK_DURATION_MS = 5000L
    }

    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private var chunkCounter = 0
    @Volatile private var isRecording = false

    @Synchronized
    fun start(sessionId: String) {
        if (isRecording) {
            Log.w(TAG, "Recording already running")
            return
        }

        val dir = File(context.cacheDir, "sos_audio_$sessionId")
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Could not create audio directory: ${dir.absolutePath}")
            return
        }

        isRecording = true
        chunkCounter = 0
        Log.i(TAG, "Starting recording for session $sessionId")

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive && isRecording) {
                    val index = chunkCounter++
                    val file = File(dir, "chunk_${index}.m4a")

                    Log.i(TAG, "Starting chunk $index")
                    if (!startSingleRecording(file)) {
                        Log.e(TAG, "Could not start chunk $index")
                        delay(1000L)
                        continue
                    }

                    try {
                        delay(CHUNK_DURATION_MS)
                    } finally {
                        stopSingleRecording()
                        deliverChunk(index, file)
                    }
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "Recording coroutine cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Recording loop failed", e)
            } finally {
                stopSingleRecording()
                isRecording = false
                Log.i(TAG, "Recording loop finished")
            }
        }
    }

    private fun startSingleRecording(file: File): Boolean {
        var recorder: MediaRecorder? = null

        return try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            Log.i(TAG, "Microphone recording started: ${file.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
            mediaRecorder = null
            false
        }
    }

    @Synchronized
    private fun stopSingleRecording() {
        val recorder = mediaRecorder ?: return
        mediaRecorder = null

        try {
            recorder.stop()
            Log.i(TAG, "MediaRecorder stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize audio chunk", e)
        } finally {
            try {
                recorder.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun deliverChunk(index: Int, file: File) {
        if (file.exists() && file.length() > 0L) {
            Log.i(TAG, "Chunk ready: ${file.name}, ${file.length()} bytes")
            try {
                onChunkReady(index, file)
            } catch (e: Exception) {
                Log.e(TAG, "Chunk callback failed", e)
            }
        } else {
            Log.e(TAG, "Chunk missing or empty: ${file.absolutePath}")
        }
    }

    @Synchronized
    fun stop() {
        if (!isRecording) return

        isRecording = false
        Log.i(TAG, "Stopping audio recorder")

        recordingJob?.cancel()
        recordingJob = null
        stopSingleRecording()
    }
}
