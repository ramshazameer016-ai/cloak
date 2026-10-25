package com.example.data.network

import android.content.Context
import android.content.SharedPreferences

object SupabaseConfig {
  // Public/anon credentials configured for Cloak project
  // Can be overridden via local settings or environment variables
  const val DEFAULT_SUPABASE_URL = "https://uppnyyztbgntvxddzqbo.supabase.co"
  const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_KL_66tjyRQ1P2bhvStUTaA_A6tY-54D"
  const val DEFAULT_PLAYBACK_URL = "https://cloak-sos-audio.surge.sh"

  private const val PREFS_NAME = "supabase_config_prefs"
  private const val KEY_URL = "supabase_url"
  private const val KEY_KEY = "supabase_anon_key"
  private const val KEY_PLAYBACK_URL = "playback_url"

  fun getPlaybackUrl(context: Context, sessionId: String, token: String): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val custom = prefs.getString(KEY_PLAYBACK_URL, DEFAULT_PLAYBACK_URL)
    val base = if (!custom.isNullOrBlank()) custom else DEFAULT_PLAYBACK_URL
    return if (base.contains("?")) {
      "$base&session_id=$sessionId&token=$token"
    } else {
      "$base?session_id=$sessionId&token=$token"
    }
  }

  fun setPlaybackUrl(context: Context, url: String?) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_PLAYBACK_URL, url?.trim())
      .apply()
  }

  fun getSupabaseUrl(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_URL, DEFAULT_SUPABASE_URL) ?: DEFAULT_SUPABASE_URL
    return raw.trim().removeSuffix("/").removeSuffix("/rest/v1")
  }

  fun getSupabaseAnonKey(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_KEY, DEFAULT_SUPABASE_ANON_KEY) ?: DEFAULT_SUPABASE_ANON_KEY
  }

  fun setCredentials(context: Context, url: String, anonKey: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_URL, url.trim().removeSuffix("/"))
      .putString(KEY_KEY, anonKey.trim())
      .apply()
  }
}
