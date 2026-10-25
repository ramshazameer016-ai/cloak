
package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.CloakApplication
import com.example.core.audio.AudioChunkRecorder
import com.example.data.network.SupabaseSosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom

class SosForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "petal_health_sync_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_SOS_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_SOS_SERVICE"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"

        private const val TAG = "SosForegroundService"

        fun startService(
            context: Context,
            sessionId: String,
            accessToken: String = generateAccessToken()
        ) {
            val intent = Intent(context, SosForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_ACCESS_TOKEN, accessToken)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SosForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun generateAccessToken(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)

            return Base64.encodeToString(
                bytes,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        }
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var supabaseRepo: SupabaseSosRepository

    private var audioRecorder: AudioChunkRecorder? = null
    private var currentSessionId = ""
    private var currentAccessToken = ""

    @Volatile
    private var startInProgress = false

    @Volatile
    private var recordingStarted = false

    @Volatile
    private var stopRequested = false

    override fun onCreate() {
        super.onCreate()
        supabaseRepo = SupabaseSosRepository(this)
        createNotificationChannel()
        Log.i(TAG, "SOS service created")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                stopRequested = false

                currentSessionId =
                    intent.getStringExtra(EXTRA_SESSION_ID)
                        ?: "cloak_${System.currentTimeMillis()}"

                startForegroundWithNotification()

                if (stopRequested) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (!startInProgress && !recordingStarted) {
                    startInProgress = true

                    val sessionIdForSetup = currentSessionId
                    val tokenForSetup = intent.getStringExtra(EXTRA_ACCESS_TOKEN).orEmpty()

                    serviceScope.launch {
                        try {
                            registerSessionAndStartRecording(
                                sessionIdForSetup,
                                tokenForSetup
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "SOS startup failed", e)
                            updateAudioStatus("SOS setup failed")
                            updateUploadStatus("Session registration failed")
                        } finally {
                            startInProgress = false

                            if (stopRequested) {
                                stopAudioCapture()
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                            }
                        }
                    }
                } else {
                    Log.i(
                        TAG,
                        "Start ignored: setup or recording already active"
                    )
                }
            }

            ACTION_STOP -> {
                stopRequested = true
                stopAudioCapture()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> {
                Log.w(TAG, "Received unknown or empty service action")
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun registerSessionAndStartRecording(
        sessionId: String,
        token: String = ""
    ) {
        if (stopRequested) return

        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic) {
            Log.e(TAG, "Microphone permission is missing")
            updateAudioStatus("Microphone permission required")
            updateUploadStatus("Not uploading")
            return
        }

        if (
            sessionId.length !in 16..100 ||
            !sessionId.matches(Regex("[a-zA-Z0-9_-]+"))
        ) {
            Log.e(TAG, "Invalid SOS session ID")
            updateAudioStatus("Invalid SOS session ID")
            updateUploadStatus("Not uploading")
            return
        }

        val sessionToken = if (token.isNotBlank()) token else generateAccessToken()

        currentSessionId = sessionId
        currentAccessToken = sessionToken

        updateAudioStatus("Preparing secure SOS session")
        updateUploadStatus("Registering session")

        Log.i(TAG, "Registering SOS session")

        val registration = supabaseRepo.createAccessSession(
            sessionId = sessionId,
            token = sessionToken,
            triggerSource = "manual"
        )

        if (registration.isFailure) {
            Log.e(
                TAG,
                "Session registration failed",
                registration.exceptionOrNull()
            )

            if (currentAccessToken == sessionToken) {
                currentAccessToken = ""
            }

            updateAudioStatus("Session setup failed")
            updateUploadStatus("Check internet connection and Supabase")
            return
        }

        if (stopRequested || currentSessionId != sessionId) {
            Log.i(TAG, "SOS setup cancelled before recording started")
            return
        }

        updateUploadStatus("Waiting for first audio chunk")

        // Capture the session ID and token for this recording session.
        // Uploads won't depend on mutable service fields later.
        val sessionIdForUpload = sessionId
        val tokenForUpload = sessionToken

        audioRecorder = AudioChunkRecorder(
            context = this,
            scope = serviceScope,
            onError = { errorMsg ->
                updateAudioStatus(errorMsg)
            },
            onChunkReady = { chunkIndex: Int, chunkFile: File ->
                Log.i(
                    TAG,
                    "Chunk ready: index=$chunkIndex, bytes=${chunkFile.length()}"
                )

                if (chunkFile.length() <= 0L) {
                    Log.e(TAG, "Skipping empty audio chunk #$chunkIndex")
                    return@AudioChunkRecorder
                }

                serviceScope.launch(Dispatchers.IO) {
                    updateUploadStatus("Uploading chunk #${chunkIndex + 1}")

                    val result = supabaseRepo.uploadAudioChunk(
                        sessionId = sessionIdForUpload,
                        chunkIndex = chunkIndex,
                        audioFile = chunkFile,
                        accessToken = tokenForUpload
                    )

                    result.onSuccess { path ->
                        Log.i(TAG, "Audio chunk uploaded: $path")
                        updateUploadStatus("Uploaded chunk #${chunkIndex + 1}")
                    }.onFailure { error ->
                        Log.e(TAG, "Audio chunk upload failed", error)
                        updateUploadStatus(
                            "Upload failed: ${error.message ?: "Network error"}"
                        )
                    }
                }
            }
        )

        if (stopRequested) {
            audioRecorder = null
            currentAccessToken = ""
            return
        }

        audioRecorder?.start(sessionIdForUpload)

        recordingStarted = true
        updateAudioStatus("Recording")

        Log.i(
            TAG,
            "Recorder start requested for session $sessionIdForUpload"
        )
    }

    private fun startForegroundWithNotification() {
        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Cloak SOS")
                .setContentText("SOS recording service is active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasMic = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                val hasLoc =
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                var serviceTypes = 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasMic) {
                    serviceTypes = serviceTypes or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }

                if (hasLoc) {
                    serviceTypes = serviceTypes or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }

                if (serviceTypes != 0) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        serviceTypes
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            Log.i(TAG, "Foreground notification started")
        } catch (e: Exception) {
            Log.e(TAG, "Could not start foreground service", e)
            stopRequested = true
            stopSelf()
        }
    }

    private fun stopAudioCapture() {
        Log.i(TAG, "Stopping audio capture")

        audioRecorder?.stop()
        audioRecorder = null

        recordingStarted = false
        currentAccessToken = ""

        updateAudioStatus("Idle")
        updateUploadStatus("Idle")
    }

    private fun updateAudioStatus(status: String) {
        (applicationContext as? CloakApplication)
            ?.sosController
            ?.updateAudioRecordingStatus(status)
    }

    private fun updateUploadStatus(status: String) {
        (applicationContext as? CloakApplication)
            ?.sosController
            ?.updateChunkUploadStatus(status)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cloak SOS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SOS recording and upload status"
            }

            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "SOS service destroyed")
        stopRequested = true
        stopAudioCapture()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
