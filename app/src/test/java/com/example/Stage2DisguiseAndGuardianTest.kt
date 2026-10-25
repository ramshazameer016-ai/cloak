package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.location.LocationProvider
import com.example.core.telephony.SmsDispatchResult
import com.example.core.telephony.SmsDispatcher
import com.example.data.local.CloakDatabase
import com.example.data.local.entity.EmergencyContact
import com.example.data.local.entity.PeriodEntry
import com.example.data.network.SupabaseSosRepository
import com.example.data.repository.ContactRepository
import com.example.data.repository.PeriodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage2DisguiseAndGuardianTest {

  private lateinit var context: Context
  private lateinit var database: CloakDatabase
  private lateinit var periodRepo: PeriodRepository
  private lateinit var contactRepo: ContactRepository
  private lateinit var smsDispatcher: SmsDispatcher
  private lateinit var locationProvider: LocationProvider
  private lateinit var supabaseRepo: SupabaseSosRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    database = CloakDatabase.getInstance(context)
    periodRepo = PeriodRepository(database.periodDao(), context)
    contactRepo = ContactRepository(database.contactDao())
    smsDispatcher = SmsDispatcher(context)
    locationProvider = LocationProvider(context)
    supabaseRepo = SupabaseSosRepository(context)
  }

  // --- 1. PERIOD TRACKER & CYCLE PREDICTION TESTS ---

  @Test
  fun periodRepository_persistsEntriesAndPredictsCycleAccurately() = runTest {
    // 1. Initial prediction with no logged periods is null
    val initialPrediction = periodRepo.predictNextPeriod()
    assertNull(initialPrediction)

    // 2. Set average cycle length to 28 days, period duration to 5 days
    periodRepo.setAverageCycleLength(28)
    periodRepo.setPeriodDuration(5)
    assertEquals(28, periodRepo.getAverageCycleLength())
    assertEquals(5, periodRepo.getPeriodDuration())

    // 3. Log a period start on 2026-10-01
    val baseDate = LocalDate.of(2026, 10, 1)
    val entry = PeriodEntry(
      dateString = baseDate.format(PeriodRepository.DATE_FORMATTER),
      isPeriodDay = true,
      flowIntensity = "Heavy",
      symptoms = "Cramps,Bloating",
      mood = "Sensitive",
      notes = "Day 1 of cycle"
    )
    periodRepo.saveEntry(entry)

    val retrieved = periodRepo.getEntry(baseDate)
    assertNotNull(retrieved)
    assertTrue(retrieved!!.isPeriodDay)
    assertEquals("Heavy", retrieved.flowIntensity)
    assertTrue(retrieved.symptoms.contains("Cramps"))

    // 4. Predict next period: 2026-10-01 + 28 days = 2026-10-29 to 2026-11-02
    val prediction = periodRepo.predictNextPeriod()
    assertNotNull(prediction)
    val (expectedStart, expectedEnd) = prediction!!
    assertEquals(LocalDate.of(2026, 10, 29), expectedStart)
    assertEquals(LocalDate.of(2026, 11, 2), expectedEnd)
  }

  // --- 2. EMERGENCY CONTACT BOUNDS & VALIDATION TESTS ---

  @Test
  fun contactRepository_enforcesMaximumThreeContacts() = runTest {
    contactRepo.clearAll()

    val c1 = contactRepo.addContact("Sister", "+1234567890", "Family")
    assertTrue(c1.isSuccess)

    val c2 = contactRepo.addContact("Friend", "+1234567891", "Friend")
    assertTrue(c2.isSuccess)

    val c3 = contactRepo.addContact("Mom", "+1234567892", "Parent")
    assertTrue(c3.isSuccess)

    // 4th contact must fail (maximum 3 allowed by specification)
    val c4 = contactRepo.addContact("Neighbor", "+1234567893", "Other")
    assertTrue(c4.isFailure)
    assertEquals("Maximum of 3 emergency contacts reached.", c4.exceptionOrNull()?.message)

    val all = contactRepo.getContactsSync()
    assertEquals(3, all.size)
  }

  // --- 3. LIVE LOCATION & MAPS URL TESTS ---

  @Test
  fun locationProvider_formatsGoogleMapsUrlWithoutApiKeyRequirement() {
    val lat = 37.7749
    val lng = -122.4194
    val url = locationProvider.formatGoogleMapsUrl(lat, lng)

    // Verify exact pattern required: https://maps.google.com/?q=LATITUDE,LONGITUDE
    assertEquals("https://maps.google.com/?q=37.7749,-122.4194", url)
    assertTrue(url.startsWith("https://maps.google.com/?q="))
  }

  @Test
  fun smsDispatcher_handlesNoContactsGracefully() {
    val result = smsDispatcher.dispatchEmergencySms(
      contacts = emptyList(),
      mapsUrl = "https://maps.google.com/?q=0.0,0.0"
    )
    assertEquals(SmsDispatchResult.NoContactsConfigured, result)
  }

  // --- 4. SUPABASE AUDIO CHUNK VALIDATION ---

  @Test
  fun supabaseSosRepository_rejectsEmptyOrNonExistentAudioFiles() = runTest {
    val nonExistentFile = File(context.cacheDir, "ghost_chunk.m4a")
    val result = supabaseRepo.uploadAudioChunk(
      sessionId = "test-session-123",
      chunkIndex = 0,
      audioFile = nonExistentFile
    )
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }
}
