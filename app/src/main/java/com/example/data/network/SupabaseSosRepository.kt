
package com.example.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SupabaseSosRepository(private val context: Context) {

    companion object {
        private const val TAG = "SupabaseSosRepo"
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType =
        "application/json; charset=utf-8".toMediaType()

    private val audioMediaType = "audio/mp4".toMediaType()

    private fun getBaseUrl(): String =
        SupabaseConfig.getSupabaseUrl(context).trimEnd('/')

    private fun getAnonKey(): String =
        SupabaseConfig.getSupabaseAnonKey(context)

    /**
     * Registers a new SOS session and its access token
     * through the Cloak Edge Function.
     */
    suspend fun createAccessSession(
        sessionId: String,
        token: String,
        triggerSource: String = "manual"
    ): Result<Unit> = withContext(Dispatchers.IO) {

        val url =
            "${getBaseUrl()}/functions/v1/cloak-sos-access/session"

        val json = JSONObject().apply {
            put("session_id", sessionId)
            put("token", token)
            put("trigger_source", triggerSource)
        }

        executeWithRetry("createAccessSession") {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getAnonKey())
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        Exception(
                            "Session creation failed: HTTP ${response.code}: " +
                                response.body?.string()
                        )
                    )
                }
            }
        }
    }

    /**
     * Logs a new SOS session into the sos_sessions table.
     * Keep this only if your existing app uses it and the required
     * database permissions are configured.
     */
    suspend fun recordSession(
        sessionId: String,
        triggerSource: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        val url = "${getBaseUrl()}/rest/v1/sos_sessions"

        val json = JSONObject().apply {
            put("session_id", sessionId)
            put("trigger_source", triggerSource)
            put("status", "active")
        }

        executeWithRetry("recordSession") {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer ${getAnonKey()}")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 409) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        Exception(
                            "Record session failed: HTTP ${response.code}: " +
                                response.body?.string()
                        )
                    )
                }
            }
        }
    }

    /**
     * Logs a GPS coordinate into the sos_locations table.
     */
    suspend fun recordLocation(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float?
    ): Result<Unit> = withContext(Dispatchers.IO) {

        val url = "${getBaseUrl()}/rest/v1/sos_locations"

        val json = JSONObject().apply {
            put("session_id", sessionId)
            put("latitude", latitude)
            put("longitude", longitude)
            if (accuracy != null) put("accuracy", accuracy)
        }

        executeWithRetry("recordLocation") {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer ${getAnonKey()}")
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        Exception(
                            "Record location failed: HTTP ${response.code}: " +
                                response.body?.string()
                        )
                    )
                }
            }
        }
    }

    /**
     * Uploads an audio chunk through the Cloak Edge Function.
     * A valid session token must be registered first.
     */
    suspend fun uploadAudioChunk(
        sessionId: String,
        chunkIndex: Int,
        audioFile: File,
        accessToken: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {

        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    "Audio file is empty or missing."
                )
            )
        }

        val url =
            "${getBaseUrl()}/functions/v1/cloak-sos-access/upload"

        executeWithRetry("uploadAudioChunk[$chunkIndex]") {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("session_id", sessionId)
                .addFormDataPart("token", accessToken)
                .addFormDataPart(
                    "file",
                    "chunk_${chunkIndex}.m4a",
                    audioFile.asRequestBody(audioMediaType)
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getAnonKey())
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseText =
                        response.body?.string().orEmpty()
                    val json = JSONObject(responseText)
                    val path = json.optString("path")

                    if (path.isBlank()) {
                        Result.failure(
                            Exception("Upload response did not contain a file path.")
                        )
                    } else {
                        Result.success(path)
                    }
                } else {
                    Result.failure(
                        Exception(
                            "Audio upload failed: HTTP ${response.code}: " +
                                response.body?.string()
                        )
                    )
                }
            }
        }
    }

    private suspend fun <T> executeWithRetry(
        operationName: String,
        block: () -> Result<T>
    ): Result<T> {
        var currentDelay = INITIAL_BACKOFF_MS
        var lastError: Throwable =
            Exception("Operation $operationName failed")

        for (attempt in 1..MAX_RETRIES) {
            try {
                val result = block()

                if (result.isSuccess) {
                    return result
                }

                result.exceptionOrNull()?.let {
                    lastError = it
                }

                Log.w(
                    TAG,
                    "$operationName attempt $attempt failed: ${lastError.message}"
                )
            } catch (e: Exception) {
                lastError = e
                Log.w(
                    TAG,
                    "$operationName attempt $attempt failed: ${e.message}"
                )
            }

            if (attempt < MAX_RETRIES) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }

        return Result.failure(lastError)
    }
}
