package com.example.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

sealed class BurnerNoteRetrieveResult {
  data class Success(val noteContent: String) : BurnerNoteRetrieveResult()
  data object AlreadyDestroyedOrExpired : BurnerNoteRetrieveResult()
  data object RateLimited : BurnerNoteRetrieveResult()
  data class Error(val message: String) : BurnerNoteRetrieveResult()
}

class BurnerNoteRepository(private val context: Context) {

  companion object {
    private const val TAG = "BurnerNoteRepo"
    private const val MIN_REQUEST_INTERVAL_MS = 1500L
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val secureRandom = SecureRandom()
  private var lastAttemptTimestamp = 0L

  private fun getBaseUrl(): String = SupabaseConfig.getSupabaseUrl(context).trimEnd('/')
  private fun getAnonKey(): String = SupabaseConfig.getSupabaseAnonKey(context)

  /**
   * Generates a cryptographically strong 6-digit numerical retrieval code.
   */
  fun generateSixDigitCode(): String {
    val num = secureRandom.nextInt(900000) + 100000
    return num.toString()
  }

  /**
   * Stores a note with a 24-hour expiry in the burner_notes table.
   */
  suspend fun createBurnerNote(noteBody: String): Result<String> = withContext(Dispatchers.IO) {
    if (noteBody.isBlank()) {
      return@withContext Result.failure(IllegalArgumentException("Note body cannot be empty."))
    }

    val code = generateSixDigitCode()
    val url = "${getBaseUrl()}/rest/v1/burner_notes"

    val payload = JSONObject().apply {
      put("access_code", code)
      put("note_body", noteBody)
    }

    try {
      val request = Request.Builder()
        .url(url)
        .addHeader("apikey", getAnonKey())
        .addHeader("Authorization", "Bearer ${getAnonKey()}")
        .addHeader("Prefer", "return=minimal")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          Result.success(code)
        } else {
          Result.failure(Exception("Failed to create note: HTTP ${response.code}"))
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Atomically reads and deletes a note via the server-side RPC function read_and_burn.
   * Enforces client-side rate limiting to mitigate brute-force attempts.
   */
  suspend fun retrieveAndBurn(code: String): BurnerNoteRetrieveResult = withContext(Dispatchers.IO) {
    val cleanCode = code.trim()
    if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
      return@withContext BurnerNoteRetrieveResult.Error("Code must be exactly 6 numeric digits.")
    }

    val now = System.currentTimeMillis()
    if (now - lastAttemptTimestamp < MIN_REQUEST_INTERVAL_MS) {
      return@withContext BurnerNoteRetrieveResult.RateLimited
    }
    lastAttemptTimestamp = now

    val url = "${getBaseUrl()}/rest/v1/rpc/read_and_burn"
    val payload = JSONObject().apply {
      put("p_code", cleanCode)
    }

    try {
      val request = Request.Builder()
        .url(url)
        .addHeader("apikey", getAnonKey())
        .addHeader("Authorization", "Bearer ${getAnonKey()}")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext BurnerNoteRetrieveResult.Error("HTTP ${response.code}: ${response.message}")
        }

        val bodyStr = response.body?.string() ?: ""
        parseRpcResponse(bodyStr)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Network failure retrieving note: ${e.message}")
      BurnerNoteRetrieveResult.Error("Connection error: ${e.message}")
    }
  }

  private fun parseRpcResponse(bodyStr: String): BurnerNoteRetrieveResult {
    return try {
      val jsonArray = JSONArray(bodyStr)
      if (jsonArray.length() == 0) {
        return BurnerNoteRetrieveResult.AlreadyDestroyedOrExpired
      }
      val firstObj = jsonArray.getJSONObject(0)
      val isValid = firstObj.optBoolean("is_valid", false)
      val content = firstObj.optString("note_content", "")

      if (isValid && content.isNotBlank()) {
        BurnerNoteRetrieveResult.Success(content)
      } else {
        BurnerNoteRetrieveResult.AlreadyDestroyedOrExpired
      }
    } catch (_: Exception) {
      // In case the RPC returns a single JSONObject instead of an array
      try {
        val jsonObj = JSONObject(bodyStr)
        val isValid = jsonObj.optBoolean("is_valid", false)
        val content = jsonObj.optString("note_content", "")
        if (isValid && content.isNotBlank()) {
          BurnerNoteRetrieveResult.Success(content)
        } else {
          BurnerNoteRetrieveResult.AlreadyDestroyedOrExpired
        }
      } catch (_: Exception) {
        BurnerNoteRetrieveResult.AlreadyDestroyedOrExpired
      }
    }
  }
}
