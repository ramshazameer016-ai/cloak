package com.example

import android.app.Application
import com.example.core.demo.DemoConfigManager
import com.example.core.location.LocationProvider
import com.example.core.security.PinStorage
import com.example.core.sensor.ShakeDetector
import com.example.core.sos.SosController
import com.example.core.telephony.SmsDispatcher
import com.example.data.local.CloakDatabase
import com.example.data.repository.ContactRepository
import com.example.data.repository.PeriodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CloakApplication : Application() {

  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  val database by lazy { CloakDatabase.getInstance(this) }
  val contactRepository by lazy { ContactRepository(database.contactDao()) }
  val periodRepository by lazy { PeriodRepository(database.periodDao(), this) }
  val pinStorage by lazy { PinStorage(this) }
  val locationProvider by lazy { LocationProvider(this) }
  val smsDispatcher by lazy { SmsDispatcher(this) }
  val demoConfigManager by lazy { DemoConfigManager(this) }

  val sosController by lazy {
    SosController(
      context = this,
      contactRepository = contactRepository,
      smsDispatcher = smsDispatcher,
      locationProvider = locationProvider,
      scope = applicationScope
    )
  }

  val shakeDetector by lazy {
    ShakeDetector(
      context = this,
      onShakeTriggered = {
        sosController.startSos(triggerSource = "shake")
      }
    )
  }

  override fun onCreate() {
    super.onCreate()
    // Start foreground shake monitoring
    shakeDetector.start()
  }
}
