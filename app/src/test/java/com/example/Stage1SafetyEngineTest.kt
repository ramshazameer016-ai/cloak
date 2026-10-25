package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.location.LocationProvider
import com.example.core.security.CryptoManager
import com.example.core.security.PinStorage
import com.example.core.security.PinVerificationResult
import com.example.core.sensor.ShakeDetector
import com.example.core.sos.SosController
import com.example.core.sos.SosState
import com.example.core.telephony.SmsDispatcher
import com.example.data.local.CloakDatabase
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class Stage1SafetyEngineTest {

  private lateinit var context: Context
  private lateinit var pinStorage: PinStorage
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    pinStorage = PinStorage(context)
    pinStorage.clearAll()
  }

  // --- 1. CRYPTO & PIN DERIVATION TESTS ---

  @Test
  fun cryptoManager_hashesPinWithRandomSaltAndVerifiesCorrectly() {
    val pin = "1234"
    val hashResult1 = CryptoManager.hashPin(pin)
    val hashResult2 = CryptoManager.hashPin(pin)

    // Salt randomness: identical PINs have distinct salts and hashes
    assertNotEquals(hashResult1.saltHex, hashResult2.saltHex)
    assertNotEquals(hashResult1.hashHex, hashResult2.hashHex)

    // Verification succeeds with correct PIN and stored salt/hash
    assertTrue(CryptoManager.verifyPin(pin, hashResult1.saltHex, hashResult1.hashHex))
    assertTrue(CryptoManager.verifyPin(pin, hashResult2.saltHex, hashResult2.hashHex))

    // Verification fails with wrong PIN
    assertFalse(CryptoManager.verifyPin("9999", hashResult1.saltHex, hashResult1.hashHex))
    assertFalse(CryptoManager.verifyPin("0000", hashResult1.saltHex, hashResult1.hashHex))
  }

  // --- 2. PIN STORAGE & DURESS SEPARATION TESTS ---

  @Test
  fun pinStorage_rejectsInvalidLengthOrIdenticalPins() {
    // Rejects non-4-digit PINs
    val shortResult = pinStorage.setupPins("12", "9999")
    assertTrue(shortResult.isFailure)

    // Rejects non-numeric characters
    val alphaResult = pinStorage.setupPins("123a", "9999")
    assertTrue(alphaResult.isFailure)

    // Rejects identical Normal and Duress PINs (critical security rule)
    val identicalResult = pinStorage.setupPins("1234", "1234")
    assertTrue(identicalResult.isFailure)

    assertFalse(pinStorage.isSetupComplete())
  }

  @Test
  fun pinStorage_distinguishesNormalAndDuressCredentials() {
    val setupResult = pinStorage.setupPins("1234", "9999")
    assertTrue(setupResult.isSuccess)
    assertTrue(pinStorage.isSetupComplete())

    // Normal PIN verification
    val normalResult = pinStorage.verify("1234")
    assertEquals(PinVerificationResult.NormalPinSuccess, normalResult)

    // Duress PIN verification
    val duressResult = pinStorage.verify("9999")
    assertEquals(PinVerificationResult.DuressPinSuccess, duressResult)

    // Unknown PIN verification
    val invalidResult = pinStorage.verify("5555")
    assertEquals(PinVerificationResult.InvalidPin, invalidResult)
  }

  // --- 3. SHAKE DETECTOR ALGORITHM TESTS ---

  @Test
  fun shakeDetector_identifiesThreeSpikesWithProperSpacing() {
    var triggered = false
    val detector = ShakeDetector(
      context = context,
      threshold = 18.0f,
      minSpikeIntervalMs = 250L,
      windowDurationMs = 1500L,
      requiredSpikes = 3,
      onShakeTriggered = { triggered = true }
    )

    val baseTime = 1000L

    // Spike 1 at t = 1000ms
    val s1 = detector.processAccelerationSample(20.0f, baseTime)
    assertFalse(s1)

    // Rapid event at t = 1100ms (< 250ms spacing) -> must be ignored as single movement
    val sIgnore = detector.processAccelerationSample(22.0f, baseTime + 100L)
    assertFalse(sIgnore)

    // Spike 2 at t = 1350ms (350ms >= 250ms spacing) -> recorded
    val s2 = detector.processAccelerationSample(21.0f, baseTime + 350L)
    assertFalse(s2)

    // Spike 3 at t = 1700ms (within 1500ms window from t=1000ms) -> triggers SOS
    val s3 = detector.processAccelerationSample(24.0f, baseTime + 700L)
    assertTrue(s3)
  }

  @Test
  fun shakeDetector_ignoresNormalWalkingMovementBelowThreshold() {
    val detector = ShakeDetector(
      context = context,
      threshold = 18.0f,
      minSpikeIntervalMs = 250L,
      windowDurationMs = 1500L,
      requiredSpikes = 3,
      onShakeTriggered = { }
    )

    val baseTime = 2000L
    // Normal walking generates low linear acceleration (e.g. 5 to 12 m/s^2)
    assertFalse(detector.processAccelerationSample(8.0f, baseTime))
    assertFalse(detector.processAccelerationSample(10.5f, baseTime + 300L))
    assertFalse(detector.processAccelerationSample(9.2f, baseTime + 600L))
  }

  // --- 4. SOS CONTROLLER & IDEMPOTENCY TESTS ---

  @Test
  fun sosController_idempotentDuressTriggerAndNormalStop() = runTest {
    val contactRepo = ContactRepository(CloakDatabase.getInstance(context).contactDao())
    val smsDispatcher = SmsDispatcher(context)
    val locationProvider = LocationProvider(context)

    val controller = SosController(
      context = context,
      contactRepository = contactRepo,
      smsDispatcher = smsDispatcher,
      locationProvider = locationProvider,
      scope = testScope
    )

    assertEquals(SosState.Idle, controller.state.value)
    assertFalse(controller.isSosActive)

    // Trigger SOS via duress PIN
    controller.startSos(triggerSource = "duress_pin")

    val stateAfterTrigger = controller.state.value
    assertTrue(stateAfterTrigger is SosState.Active)
    assertTrue(controller.isSosActive)

    val activeState = stateAfterTrigger as SosState.Active
    val initialSessionId = activeState.sessionId
    assertEquals("duress_pin", activeState.triggerSource)

    // IDEMPOTENCY TEST: Repeated trigger (e.g. repeated shake or duress input)
    // Must preserve the existing active session ID and not create duplicate sessions
    controller.startSos(triggerSource = "shake")
    val stateAfterSecondTrigger = controller.state.value as SosState.Active
    assertEquals(initialSessionId, stateAfterSecondTrigger.sessionId)

    // STOP TEST: Normal PIN entry invokes stopSos()
    controller.stopSos()
    assertEquals(SosState.Idle, controller.state.value)
    assertFalse(controller.isSosActive)
  }
}
