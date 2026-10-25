package com.example.core.shield

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SanitizationResult(
  val isSuccess: Boolean,
  val outputFile: File? = null,
  val verifiedClean: Boolean = false,
  val tagsRemoved: List<String> = emptyList(),
  val errorMessage: String? = null
)

object PhotoSanitizer {

  private val SENSITIVE_EXIF_TAGS = listOf(
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION
  )

  /**
   * Sanitizes a photo by decoding pixel data and re-compressing to a fresh file,
   * completely stripping all hardware, camera, GPS, and timestamp metadata.
   * Verifies the output file before reporting success.
   */
  suspend fun sanitizePhoto(context: Context, sourceUri: Uri): SanitizationResult = withContext(Dispatchers.IO) {
    try {
      // 1. Inspect original image to count existing sensitive tags
      val removedTags = mutableListOf<String>()
      context.contentResolver.openInputStream(sourceUri)?.use { input ->
        val originalExif = ExifInterface(input)
        for (tag in SENSITIVE_EXIF_TAGS) {
          if (originalExif.getAttribute(tag) != null) {
            removedTags.add(tag)
          }
        }
      }

      // 2. Decode bitmap from source URI
      val bitmap: Bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input)
      } ?: return@withContext SanitizationResult(
        isSuccess = false,
        errorMessage = "Unable to decode image file. Format may be unsupported or corrupt."
      )

      // 3. Save clean copy into app pictures directory
      val outDir = File(context.cacheDir, "sanitized_photos")
      if (!outDir.exists()) outDir.mkdirs()

      val cleanFile = File(outDir, "clean_${System.currentTimeMillis()}.jpg")
      FileOutputStream(cleanFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 94, fos)
        fos.flush()
      }
      bitmap.recycle()

      // 4. VERIFY: Inspect the new output file to ensure all metadata is stripped
      val cleanExif = ExifInterface(cleanFile.absolutePath)
      var hasResidualMetadata = false
      for (tag in SENSITIVE_EXIF_TAGS) {
        if (cleanExif.getAttribute(tag) != null) {
          hasResidualMetadata = true
          break
        }
      }

      val verified = !hasResidualMetadata
      SanitizationResult(
        isSuccess = true,
        outputFile = cleanFile,
        verifiedClean = verified,
        tagsRemoved = removedTags
      )
    } catch (e: Exception) {
      SanitizationResult(
        isSuccess = false,
        errorMessage = "Sanitization failed: ${e.message}"
      )
    }
  }
}
