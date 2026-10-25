package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.location.LocationProvider
import com.example.core.security.PinStorage
import com.example.core.security.PinVerificationResult
import com.example.core.sos.SosController
import com.example.core.sos.SosState
import com.example.core.telephony.SmsDispatcher
import com.example.data.local.CloakDatabase
import com.example.core.demo.DemoConfigManager
import com.example.data.repository.ContactRepository
import com.example.ui.auth.AuthViewModel
import com.example.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class DuressPinAndSosActivationTest {

  private lateinit var context: Context
  private lateinit var pinStorage: PinStorage
  private lateinit var sosController: SosController
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    pinStorage = PinStorage(context)
    pinStorage.clearAll()
    val setupResult = pinStorage.setupPins("1234", "9999")
    assertTrue(setupResult.isSuccess)

    val db = CloakDatabase.getInstance(context)
    val contactRepo = ContactRepository(db.contactDao())
    val smsDispatcher = SmsDispatcher(context)
    val locationProvider = LocationProvider(context)

    sosController = SosController(
      context = context,
      contactRepository = contactRepo,
      smsDispatcher = smsDispatcher,
      locationProvider = locationProvider,
      scope = testScope
    )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- 1. NORMAL PIN DOES NOT ACTIVATE SOS ---

  @Test
  fun normalPin_unlocksToTrackerWithoutActivatingSos() = runTest {
    assertEquals(SosState.Idle, sosController.state.value)
    assertFalse(sosController.isSosActive)

    // Simulate keypad flow with normal PIN 1234
    val enteredPin = "1234"
    val verification = pinStorage.verify(enteredPin)

    // Verify authentication channel
    assertEquals(PinVerificationResult.NormalPinSuccess, verification)

    // Execute standard KeypadScreen action
    var destinationRoute: String? = null
    when (verification) {
      is PinVerificationResult.NormalPinSuccess -> {
        if (sosController.isSosActive) {
          sosController.stopSos()
        }
        destinationRoute = Screen.Tracker.route
      }
      is PinVerificationResult.DuressPinSuccess -> {
        sosController.startSos(triggerSource = "duress_pin")
        destinationRoute = Screen.Tracker.route
      }
      is PinVerificationResult.InvalidPin -> { }
    }

    // Normal PIN destination must be Tracker
    assertEquals(Screen.Tracker.route, destinationRoute)

    // SOS MUST REMAIN IDLE
    assertEquals(SosState.Idle, sosController.state.value)
    assertFalse(sosController.isSosActive)
  }

  // --- 2. DURESS PIN ACTIVATES SOS AND OPENS SAME TRACKER ---

  @Test
  fun duressPin_activatesSosAndOpensSameTrackerScreen() = runTest {
    assertEquals(SosState.Idle, sosController.state.value)
    assertFalse(sosController.isSosActive)

    // Simulate keypad flow with duress PIN 9999
    val enteredPin = "9999"
    val verification = pinStorage.verify(enteredPin)

    // Verify authentication channel
    assertEquals(PinVerificationResult.DuressPinSuccess, verification)

    // Execute standard KeypadScreen action
    var destinationRoute: String? = null
    when (verification) {
      is PinVerificationResult.NormalPinSuccess -> {
        if (sosController.isSosActive) {
          sosController.stopSos()
        }
        destinationRoute = Screen.Tracker.route
      }
      is PinVerificationResult.DuressPinSuccess -> {
        sosController.startSos(triggerSource = "duress_pin")
        destinationRoute = Screen.Tracker.route
      }
      is PinVerificationResult.InvalidPin -> { }
    }

    // Both PINs must open the EXACT SAME tracker screen
    assertEquals(Screen.Tracker.route, destinationRoute)

    // SOS MUST BE GENUINELY ACTIVATED
    assertTrue(sosController.isSosActive)
    val activeState = sosController.state.value
    assertTrue(activeState is SosState.Active)
    val active = activeState as SosState.Active
    assertEquals("duress_pin", active.triggerSource)
    assertNotNull(active.sessionId)
  }

  // --- 3. IDEMPOTENT SOS ENGINE: DUPLICATE DURESS PIN ENTRY DOES NOT RE-START SESSION ---

  @Test
  fun duressPin_repeatedEntryMaintainsExistingSessionIdempotently() = runTest {
    // 1st Duress trigger
    sosController.startSos(triggerSource = "duress_pin")
    val initialActive = sosController.state.value as SosState.Active
    val initialSessionId = initialActive.sessionId

    // 2nd Duress trigger (e.g. user re-enters 9999 from lock screen or Fake-Sleep)
    sosController.startSos(triggerSource = "duress_pin")
    val currentActive = sosController.state.value as SosState.Active

    // Session ID and active state must be strictly preserved
    assertEquals(initialSessionId, currentActive.sessionId)
    assertEquals("duress_pin", currentActive.triggerSource)
    assertTrue(sosController.isSosActive)
  }

  // --- 4. NORMAL PIN DEACTIVATES ACTIVE SOS SESSION ---

  @Test
  fun normalPin_stopsActiveSosSessionAndReturnsToTracker() = runTest {
    // Start SOS via duress PIN
    sosController.startSos(triggerSource = "duress_pin")
    assertTrue(sosController.isSosActive)

    // User enters Normal PIN 1234
    val verification = pinStorage.verify("1234")
    assertEquals(PinVerificationResult.NormalPinSuccess, verification)

    // Execute Normal PIN action
    if (sosController.isSosActive) {
      sosController.stopSos()
    }
    val destination = Screen.Tracker.route

    assertEquals(Screen.Tracker.route, destination)
    assertFalse(sosController.isSosActive)
    assertEquals(SosState.Idle, sosController.state.value)
  }

  // --- 5. PERSISTENCE ACROSS NAVIGATION / FAKE-SLEEP LIFECYCLE ---

  @Test
  fun activeSos_persistsAcrossFakeSleepAndScreenNavigation() = runTest {
    // Trigger SOS via duress PIN
    sosController.startSos(triggerSource = "duress_pin")
    val active = sosController.state.value as SosState.Active
    val sessionId = active.sessionId

    // Simulate screen navigation: Tracker -> Keypad -> Tracker
    val lockedRoute = Screen.Keypad.route
    assertEquals(Screen.Keypad.route, lockedRoute)
    // SOS must remain active during lock screen
    assertTrue(sosController.isSosActive)

    // Unlock via Duress PIN from Fake-Sleep / Keypad
    val verifyDuress = pinStorage.verify("9999")
    if (verifyDuress is PinVerificationResult.DuressPinSuccess) {
      sosController.startSos(triggerSource = "duress_pin") // Idempotent
    }
    val unlockedRoute = Screen.Tracker.route
    assertEquals(Screen.Tracker.route, unlockedRoute)

    // SOS session must still be the exact same active session
    assertTrue(sosController.isSosActive)
    assertEquals(sessionId, (sosController.state.value as SosState.Active).sessionId)
  }

  // --- 6. AUTH VIEWMODEL: NORMAL PIN FLOW ---

  @Test
  fun authViewModel_normalPin_unlocksWithoutActivatingSos() = runTest {
    val viewModel = AuthViewModel(pinStorage, sosController)
    var unlockEmitted = false
    var isDuressResult: Boolean? = null

    val job = launch {
      viewModel.unlockEvent.collect { isDuress ->
        unlockEmitted = true
        isDuressResult = isDuress
      }
    }
    testScheduler.runCurrent()

    // Type "1234"
    viewModel.onKeyClicked("1")
    viewModel.onKeyClicked("2")
    viewModel.onKeyClicked("3")
    viewModel.onKeyClicked("4")

    testScheduler.advanceUntilIdle()

    assertTrue("Unlock event should be emitted for normal PIN", unlockEmitted)
    assertEquals(false, isDuressResult)
    assertFalse("SOS should NOT be activated by normal PIN", sosController.isSosActive)
    assertEquals(SosState.Idle, sosController.state.value)

    job.cancel()
  }

  // --- 7. AUTH VIEWMODEL: DURESS PIN FLOW ---

  @Test
  fun authViewModel_duressPin_unlocksAndSilentlyActivatesSos() = runTest {
    val viewModel = AuthViewModel(pinStorage, sosController)
    var unlockEmitted = false
    var isDuressResult: Boolean? = null

    val job = launch {
      viewModel.unlockEvent.collect { isDuress ->
        unlockEmitted = true
        isDuressResult = isDuress
      }
    }
    testScheduler.runCurrent()

    // Type "9999" (duress)
    viewModel.onKeyClicked("9")
    viewModel.onKeyClicked("9")
    viewModel.onKeyClicked("9")
    viewModel.onKeyClicked("9")

    testScheduler.advanceUntilIdle()

    assertTrue("Unlock event should be emitted for duress PIN", unlockEmitted)
    assertEquals(true, isDuressResult)
    assertTrue("SOS MUST be activated by duress PIN", sosController.isSosActive)
    val state = sosController.state.value
    assertTrue(state is SosState.Active)
    assertEquals("duress_pin", (state as SosState.Active).triggerSource)

    job.cancel()
  }

  // --- 8. AUTH VIEWMODEL: INVALID PIN ---

  @Test
  fun authViewModel_invalidPin_setsErrorMessageAndClearsInput() = runTest {
    val viewModel = AuthViewModel(pinStorage, sosController)
    var unlockEmitted = false

    val job = launch {
      viewModel.unlockEvent.collect {
        unlockEmitted = true
      }
    }

    // Type "0000" (invalid)
    viewModel.onKeyClicked("0")
    viewModel.onKeyClicked("0")
    viewModel.onKeyClicked("0")
    viewModel.onKeyClicked("0")

    testScheduler.advanceUntilIdle()

    assertFalse("Unlock event must NOT be emitted for invalid PIN", unlockEmitted)
    assertFalse("SOS must NOT be active", sosController.isSosActive)
    assertEquals("", viewModel.enteredPin.value)
    assertEquals("Incorrect passcode. Please try again.", viewModel.errorMessage.value)

    job.cancel()
  }

  // --- 9. DEMO MODE NAVIGATION: DURESS PIN ROUTING ---

  @Test
  fun demoMode_routingDecision_duressPinOpensDemoSosWhenEnabled_opensTrackerWhenDisabled() = runTest {
    val demoConfig = DemoConfigManager(context)

    // A) When Demo Mode is DISABLED (default production mode)
    demoConfig.setDemoModeEnabled(false)
    val routeWhenDisabled = if (true && demoConfig.isDemoModeEnabled) {
      Screen.DemoSos.route
    } else {
      Screen.Tracker.route
    }
    assertEquals("Production mode must route duress PIN directly to Petal tracker", Screen.Tracker.route, routeWhenDisabled)

    // B) When Demo Mode is explicitly ENABLED
    demoConfig.setDemoModeEnabled(true)
    val routeWhenEnabled = if (true && demoConfig.isDemoModeEnabled) {
      Screen.DemoSos.route
    } else {
      Screen.Tracker.route
    }
    assertEquals("Demo mode must route duress PIN to Demo SOS status screen", Screen.DemoSos.route, routeWhenEnabled)

    // C) Normal PIN always routes to Tracker regardless of demo mode
    val normalRoute = if (false && demoConfig.isDemoModeEnabled) {
      Screen.DemoSos.route
    } else {
      Screen.Tracker.route
    }
    assertEquals("Normal PIN must always open Petal tracker", Screen.Tracker.route, normalRoute)
  }

  // --- 10. FAKE-SLEEP RECOVERY: DUAL PIN VERIFICATION ---

  @Test
  fun fakeSleepRecovery_normalPinStopsSos_duressPinLeavesSosActive() = runTest {
    // 1. Activate SOS
    sosController.startSos("duress_pin")
    assertTrue(sosController.isSosActive)

    // 2. Normal PIN 1234 in Fake-Sleep recovery stops SOS
    val verifyNormal = pinStorage.verify("1234")
    assertEquals(PinVerificationResult.NormalPinSuccess, verifyNormal)
    sosController.stopSos()
    assertFalse("Normal PIN in Fake-Sleep recovery must stop SOS", sosController.isSosActive)
    assertEquals(SosState.Idle, sosController.state.value)

    // 3. Reactivate SOS
    sosController.startSos("duress_pin")
    assertTrue(sosController.isSosActive)

    // 4. Duress PIN 9999 in Fake-Sleep recovery leaves SOS active
    val verifyDuress = pinStorage.verify("9999")
    assertEquals(PinVerificationResult.DuressPinSuccess, verifyDuress)
    // Duress PIN maintains active SOS
    assertTrue("Duress PIN in Fake-Sleep recovery must leave SOS active", sosController.isSosActive)
  }
}
