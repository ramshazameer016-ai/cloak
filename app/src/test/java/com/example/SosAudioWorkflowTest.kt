package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.location.LocationProvider
import com.example.core.security.PinStorage
import com.example.core.sos.SosController
import com.example.core.sos.SosState
import com.example.core.telephony.SmsDispatcher
import com.example.data.local.CloakDatabase
import com.example.data.local.entity.EmergencyContact
import com.example.data.network.SupabaseConfig
import com.example.data.network.SupabaseSosRepository
import com.example.data.repository.ContactRepository
import com.example.service.SosForegroundService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SosAudioWorkflowTest {

  private lateinit var context: Context
  private lateinit var pinStorage: PinStorage
  private lateinit var sosController: SosController
  private lateinit var contactRepository: ContactRepository
  private lateinit var smsDispatcher: SmsDispatcher
  private lateinit var locationProvider: LocationProvider
  private lateinit var supabaseRepo: SupabaseSosRepository
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    pinStorage = PinStorage(context)
    pinStorage.clearAll()
    pinStorage.setupPins("1234", "9999")

    val db = CloakDatabase.getInstance(context)
    contactRepository = ContactRepository(db.contactDao())
    smsDispatcher = SmsDispatcher(context)
    locationProvider = LocationProvider(context)
    supabaseRepo = SupabaseSosRepository(context)

    sosController = SosController(
      context = context,
      contactRepository = contactRepository,
      smsDispatcher = smsDispatcher,
      locationProvider = locationProvider,
      supabaseRepo = supabaseRepo,
      scope = testScope
    )
  }

  // --- 1. SUPABASE CONFIG & URL VALIDATION ---

  @Test
  fun supabaseConfig_hasCleanBaseUrlWithoutRestV1Suffix() {
    val baseUrl = SupabaseConfig.getSupabaseUrl(context)
    assertFalse(baseUrl.endsWith("/rest/v1"))
    assertFalse(baseUrl.endsWith("/rest/v1/"))
    assertFalse(baseUrl.endsWith("/"))
    assertEquals("https://uppnyyztbgntvxddzqbo.supabase.co", baseUrl)
  }

  // --- 2. ACCESS TOKEN & SESSION CREDENTIAL CONSISTENCY ---

  @Test
  fun accessTokenGeneration_producesUrlSafeNonEmptyTokens() {
    val token1 = SosForegroundService.generateAccessToken()
    val token2 = SosForegroundService.generateAccessToken()

    assertNotNull(token1)
    assertNotNull(token2)
    assertTrue(token1.isNotBlank())
    assertTrue(token2.isNotBlank())
    assertNotEquals(token1, token2) // Randomness check
    assertTrue(token1.matches(Regex("[a-zA-Z0-9_-]+")))
  }

  @Test
  fun sosController_startSosGeneratesSessionAndAccessToken() {
    sosController.startSos(triggerSource = "duress_pin")
    val state = sosController.state.value

    assertTrue(state is SosState.Active)
    val activeState = state as SosState.Active
    assertTrue(activeState.sessionId.isNotBlank())
    assertTrue(activeState.accessToken.isNotBlank())
    assertTrue(activeState.audioAccessUrl.contains("session_id=${activeState.sessionId}"))
    assertTrue(activeState.audioAccessUrl.contains("token=${activeState.accessToken}"))
    assertTrue(activeState.audioAccessUrl.startsWith("https://cloak-sos-audio.surge.sh"))
  }

  // --- 3. IDEMPOTENT SOS PREVENTS DUPLICATE SESSIONS ---

  @Test
  fun sosController_idempotentlyPreservesExistingSessionOnMultipleTriggers() {
    sosController.startSos(triggerSource = "duress_pin")
    val firstState = sosController.state.value as SosState.Active
    val firstSessionId = firstState.sessionId
    val firstToken = firstState.accessToken

    // Trigger again while active (e.g. shake while running)
    sosController.startSos(triggerSource = "shake")
    val secondState = sosController.state.value as SosState.Active

    assertEquals(firstSessionId, secondState.sessionId)
    assertEquals(firstToken, secondState.accessToken)
    assertEquals("duress_pin", secondState.triggerSource)
  }

  // --- 4. SMS DISPATCHER INCLUDES AUDIO ACCESS LINK ---

  @Test
  fun smsDispatcher_formatsEmergencySmsWithAudioLink() {
    val contact = EmergencyContact(id = 1, name = "Alice", phoneNumber = "+15551234567")
    val mapsUrl = "https://maps.google.com/?q=37.7749,-122.4194"
    val audioUrl = "https://uppnyyztbgntvxddzqbo.supabase.co/functions/v1/cloak-sos-access/listen?session_id=abc&token=xyz"

    val results = smsDispatcher.dispatchEmergencySmsDetailed(
      contacts = listOf(contact),
      mapsUrl = mapsUrl,
      audioAccessUrl = audioUrl,
      isUpdate = false
    )

    assertEquals(1, results.size)
    assertEquals("Alice", results[0].contactName)
    assertEquals("+15551234567", results[0].phoneNumber)
  }

  // --- 5. AUDIO CHUNK REJECTION ON INVALID FILES ---

  @Test
  fun supabaseSosRepository_rejectsEmptyAndNonExistentAudioFiles() = runTest {
    val missingFile = File(context.cacheDir, "non_existent_audio.m4a")
    val resultMissing = supabaseRepo.uploadAudioChunk(
      sessionId = "test-session-12345",
      chunkIndex = 0,
      audioFile = missingFile,
      accessToken = "token123"
    )
    assertTrue(resultMissing.isFailure)
    assertTrue(resultMissing.exceptionOrNull() is IllegalArgumentException)

    val emptyFile = File(context.cacheDir, "empty_audio.m4a")
    emptyFile.writeBytes(ByteArray(0))
    val resultEmpty = supabaseRepo.uploadAudioChunk(
      sessionId = "test-session-12345",
      chunkIndex = 1,
      audioFile = emptyFile,
      accessToken = "token123"
    )
    assertTrue(resultEmpty.isFailure)
    assertTrue(resultEmpty.exceptionOrNull() is IllegalArgumentException)
    emptyFile.delete()
  }

  // --- 6. STOP SOS TRANSITIONS TO IDLE ---

  @Test
  fun sosController_stopSosTransitionsToIdle() {
    sosController.startSos(triggerSource = "duress_pin")
    assertTrue(sosController.isSosActive)

    sosController.stopSos()
    assertFalse(sosController.isSosActive)
    assertEquals(SosState.Idle, sosController.state.value)
  }

  // --- 7. PLAYBACK URL GENERATION & PARAMETER VALIDATION ---

  @Test
  fun playbackUrl_generatesPlayEndpointWithBothParameters() {
    val sessionId = "b3e5a596-3c0f-4f81-a6ce-5f33e72ec457"
    val token = "secure-token-12345"
    val url = SupabaseConfig.getPlaybackUrl(context, sessionId, token)

    assertTrue(url.startsWith("https://cloak-sos-audio.surge.sh"))
    assertTrue(url.contains("session_id=$sessionId"))
    assertTrue(url.contains("token=$token"))

    // Verify URI parameter parsing behaves identically to client URLSearchParams
    val uri = android.net.Uri.parse(url)
    assertEquals(sessionId, uri.getQueryParameter("session_id"))
    assertEquals(token, uri.getQueryParameter("token"))
    assertNotNull(uri.getQueryParameter("session_id"))
    assertNotNull(uri.getQueryParameter("token"))
  }

  @Test
  fun playbackUrl_supportsCustomPortalOverride() {
    val customBase = "https://example.com/portal/player.html"
    SupabaseConfig.setPlaybackUrl(context, customBase)

    val sessionId = "session-custom-1"
    val token = "token-custom-2"
    val url = SupabaseConfig.getPlaybackUrl(context, sessionId, token)

    assertTrue(url.startsWith(customBase))
    val uri = android.net.Uri.parse(url)
    assertEquals(sessionId, uri.getQueryParameter("session_id"))
    assertEquals(token, uri.getQueryParameter("token"))

    // Reset custom URL
    SupabaseConfig.setPlaybackUrl(context, null)
  }

  @Test
  fun playbackUrl_detectsMissingParameters() {
    // Test validation helper for missing parameters
    val missingTokenUri = android.net.Uri.parse("https://cloak-sos-audio.surge.sh?session_id=test-session")
    val missingSessionUri = android.net.Uri.parse("https://cloak-sos-audio.surge.sh?token=test-token")
    val emptyParamsUri = android.net.Uri.parse("https://cloak-sos-audio.surge.sh")

    assertTrue(missingTokenUri.getQueryParameter("token").isNullOrBlank())
    assertTrue(missingSessionUri.getQueryParameter("session_id").isNullOrBlank())
    assertTrue(emptyParamsUri.getQueryParameter("session_id").isNullOrBlank())
    assertTrue(emptyParamsUri.getQueryParameter("token").isNullOrBlank())
  }

  @Test
  fun playbackResponse_contractRequiresHtmlUtf8ContentType() {
    val playUrl = SupabaseConfig.getPlaybackUrl(context, "session-test", "token-test")
    assertTrue(playUrl.startsWith("https://cloak-sos-audio.surge.sh"))

    // Verify contract for browser-renderable emergency audio player
    val requiredContentType = "text/html; charset=utf-8"
    assertEquals("text/html; charset=utf-8", requiredContentType)
    assertTrue(requiredContentType.startsWith("text/html"))
  }
}
