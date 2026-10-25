package com.example.core.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.local.entity.EmergencyContact

sealed class SmsDispatchResult {
  data class Success(val sentCount: Int, val contacts: List<String>) : SmsDispatchResult()
  data object PermissionDenied : SmsDispatchResult()
  data object NoContactsConfigured : SmsDispatchResult()
  data class Failure(val message: String) : SmsDispatchResult()
}

data class DetailedContactDispatch(
  val contactName: String,
  val phoneNumber: String,
  val status: String
)

class SmsDispatcher(private val context: Context) {

  fun hasSmsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
  }

  /**
   * Dispatches native emergency SMS to up to 3 emergency contacts using SmsManager.
   */
  fun dispatchEmergencySms(
    contacts: List<EmergencyContact>,
    mapsUrl: String?,
    audioAccessUrl: String? = null,
    isUpdate: Boolean = false
  ): SmsDispatchResult {
    if (contacts.isEmpty()) {
      return SmsDispatchResult.NoContactsConfigured
    }

    if (!hasSmsPermission()) {
      return SmsDispatchResult.PermissionDenied
    }

    val message = buildMessage(mapsUrl, audioAccessUrl, isUpdate)
    val smsManager: SmsManager = getSmsManager()

    var sentCount = 0
    val sentTo = mutableListOf<String>()

    for (contact in contacts.take(3)) {
      try {
        val parts = smsManager.divideMessage(message)
        if (parts.size > 1) {
          smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
        } else {
          smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
        }
        sentCount++
        sentTo.add(contact.name.ifBlank { contact.phoneNumber })
      } catch (e: Exception) {
        // Individual contact dispatch error (e.g. invalid carrier format, no SIM)
      }
    }

    return if (sentCount > 0) {
      SmsDispatchResult.Success(sentCount, sentTo)
    } else {
      SmsDispatchResult.Failure("Could not send SMS. Verify device SIM card and mobile carrier signal.")
    }
  }

  fun dispatchEmergencySmsDetailed(
    contacts: List<EmergencyContact>,
    mapsUrl: String?,
    audioAccessUrl: String? = null,
    isUpdate: Boolean = false
  ): List<DetailedContactDispatch> {
    if (contacts.isEmpty()) return emptyList()

    if (!hasSmsPermission()) {
      return contacts.take(3).map {
        DetailedContactDispatch(
          contactName = it.name.ifBlank { it.phoneNumber },
          phoneNumber = it.phoneNumber,
          status = "Waiting for permission"
        )
      }
    }

    val message = buildMessage(mapsUrl, audioAccessUrl, isUpdate)
    val smsManager: SmsManager = getSmsManager()
    val results = mutableListOf<DetailedContactDispatch>()

    for (contact in contacts.take(3)) {
      val displayName = contact.name.ifBlank { contact.phoneNumber }
      try {
        val parts = smsManager.divideMessage(message)
        if (parts.size > 1) {
          smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
        } else {
          smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
        }
        results.add(DetailedContactDispatch(displayName, contact.phoneNumber, "SMS submitted"))
      } catch (e: Exception) {
        results.add(DetailedContactDispatch(displayName, contact.phoneNumber, "Failed: ${e.message ?: "Carrier error"}"))
      }
    }
    return results
  }

  private fun buildMessage(mapsUrl: String?, audioAccessUrl: String? = null, isUpdate: Boolean = false): String {
    val prefix = if (isUpdate) "[EMERGENCY UPDATE]" else "[EMERGENCY ALERT]"
    val locationPart = if (mapsUrl != null) {
      "My location: $mapsUrl"
    } else {
      "Location: acquiring GPS lock"
    }
    val audioPart = if (!audioAccessUrl.isNullOrBlank()) {
      " | Audio: $audioAccessUrl"
    } else ""
    return "$prefix I need immediate help. $locationPart$audioPart"
  }

  @Suppress("DEPRECATION")
  private fun getSmsManager(): SmsManager {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      context.getSystemService(SmsManager::class.java)
    } else {
      SmsManager.getDefault()
    }
  }

  /**
   * Fallback composer intent if direct SMS permission is restricted.
   */
  fun openSmsComposerIntent(phoneNumber: String, text: String): Intent {
    return Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("smsto:$phoneNumber")
      putExtra("sms_body", text)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }
}
