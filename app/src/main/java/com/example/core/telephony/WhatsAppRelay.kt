package com.example.core.telephony

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppRelay {

  /**
   * Opens WhatsApp with prefilled message for the emergency contact.
   * User must tap 'Send' inside WhatsApp as Android third-party apps cannot
   * send WhatsApp messages silently in the background.
   */
  fun openWhatsAppEmergencyChat(
    context: Context,
    phoneNumber: String,
    mapsUrl: String?
  ): Boolean {
    return try {
      val cleanPhone = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
      val message = if (mapsUrl != null) {
        "EMERGENCY ALERT: I need immediate help. My current location: $mapsUrl"
      } else {
        "EMERGENCY ALERT: I need immediate help. Location is currently acquiring."
      }

      val encodedMsg = URLEncoder.encode(message, "UTF-8")
      val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }
}
