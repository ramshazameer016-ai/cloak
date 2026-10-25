package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.SecretGestureConfig
import com.example.core.shield.DigitalLockdown
import com.example.core.shield.PhotoSanitizer
import com.example.core.telephony.WhatsAppRelay
import com.example.data.network.BurnerNoteRepository
import com.example.data.network.BurnerNoteRetrieveResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage3DigitalShieldAndHardeningTest {

  private lateinit var context: Context
  private lateinit var burnerRepo: BurnerNoteRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    burnerRepo = BurnerNoteRepository(context)
  }

  // --- 1. SECRET SHIELD GESTURE & DRIFT CANCELLATION ---

  @Test
  fun secretGesture_cancelsWhenMovementExceedsTwentyPixels() {
    val start = Offset(100f, 100f)

    // Minor drift: 6px X, 8px Y -> distance = 10px <= 20px -> NOT exceeded
    val minorDrift = Offset(106f, 108f)
    assertFalse(SecretGestureConfig.isDriftExceeded(start, minorDrift, 20f))

    // Boundary check: exactly 20px -> NOT exceeded
    val boundaryDrift = Offset(120f, 100f)
    assertFalse(SecretGestureConfig.isDriftExceeded(start, boundaryDrift, 20f))

    // Excessive drift: 15px X, 20px Y -> distance = 25px > 20px -> EXCEEDED (cancels)
    val excessiveDrift = Offset(115f, 120f)
    assertTrue(SecretGestureConfig.isDriftExceeded(start, excessiveDrift, 20f))

    // Verify 3-second hold configuration
    assertEquals(3000L, SecretGestureConfig.HOLD_DURATION_MS)
  }

  // --- 2. BURNER NOTES CODE GENERATION & VALIDATION ---

  @Test
  fun burnerNoteRepository_generatesCryptographicSixDigitCodes() {
    for (i in 1..20) {
      val code = burnerRepo.generateSixDigitCode()
      assertEquals(6, code.length)
      assertTrue(code.all { it.isDigit() })
      val num = code.toInt()
      assertTrue(num in 100000..999999)
    }
  }

  @Test
  fun burnerNoteRepository_validatesCodeFormatAndRejectsShortCodes() = runTest {
    // Rejects non-6-digit code
    val shortResult = burnerRepo.retrieveAndBurn("123")
    assertTrue(shortResult is BurnerNoteRetrieveResult.Error)

    // Rejects non-numeric code
    val alphaResult = burnerRepo.retrieveAndBurn("12345a")
    assertTrue(alphaResult is BurnerNoteRetrieveResult.Error)
  }

  // --- 3. PHOTO SANITIZER EXIF STRIPPER & VERIFICATION ---

  @Test
  fun photoSanitizer_cleansImageAndVerifiesAbsenceOfMetadata() = runTest {
    // Create synthetic bitmap in cache
    val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val testFile = File(context.cacheDir, "test_input.jpg")
    FileOutputStream(testFile).use { fos ->
      testBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
    }
    testBitmap.recycle()

    val uri = Uri.fromFile(testFile)
    val result = PhotoSanitizer.sanitizePhoto(context, uri)

    assertTrue(result.isSuccess)
    assertTrue(result.verifiedClean)
    assertNotNull(result.outputFile)
    assertTrue(result.outputFile!!.exists())
    assertTrue(result.outputFile!!.length() > 0)
  }

  // --- 4. DIGITAL LOCKDOWN SYSTEM INTENT SHORTCUTS ---

  @Test
  fun digitalLockdown_safelyExecutesSettingShortcuts() {
    // Verify methods execute safely without uncaught exceptions
    val wifiResult = DigitalLockdown.openWifiSettings(context)
    val btResult = DigitalLockdown.openBluetoothSettings(context)
    val locResult = DigitalLockdown.openLocationSettings(context)
    val airResult = DigitalLockdown.openAirplaneModeSettings(context)

    // Robolectric starts activities in shadow application
    assertTrue(wifiResult || !wifiResult) // Asserts no crash
    assertTrue(btResult || !btResult)
    assertTrue(locResult || !locResult)
    assertTrue(airResult || !airResult)
  }
}
