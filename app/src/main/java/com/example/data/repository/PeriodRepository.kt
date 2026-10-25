package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.dao.PeriodDao
import com.example.data.local.entity.PeriodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PeriodRepository(
  private val periodDao: PeriodDao,
  context: Context
) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("petal_cycle_settings", Context.MODE_PRIVATE)

  val allEntriesFlow: Flow<List<PeriodEntry>> = periodDao.getAllEntriesFlow()

  companion object {
    private const val KEY_CYCLE_LENGTH = "average_cycle_length"
    private const val KEY_PERIOD_DURATION = "period_duration_days"
    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_DURATION = 5
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  }

  fun getAverageCycleLength(): Int {
    return prefs.getInt(KEY_CYCLE_LENGTH, DEFAULT_CYCLE_LENGTH)
  }

  fun setAverageCycleLength(days: Int) {
    prefs.edit().putInt(KEY_CYCLE_LENGTH, days.coerceIn(21, 40)).apply()
  }

  fun getPeriodDuration(): Int {
    return prefs.getInt(KEY_PERIOD_DURATION, DEFAULT_PERIOD_DURATION)
  }

  fun setPeriodDuration(days: Int) {
    prefs.edit().putInt(KEY_PERIOD_DURATION, days.coerceIn(3, 10)).apply()
  }

  suspend fun getEntry(date: LocalDate): PeriodEntry? {
    return periodDao.getEntryByDate(date.format(DATE_FORMATTER))
  }

  suspend fun saveEntry(entry: PeriodEntry) {
    periodDao.insertOrUpdate(entry)
  }

  suspend fun togglePeriodDay(date: LocalDate): Boolean {
    val dateStr = date.format(DATE_FORMATTER)
    val existing = periodDao.getEntryByDate(dateStr)
    val newIsPeriod = !(existing?.isPeriodDay ?: false)
    val updated = existing?.copy(isPeriodDay = newIsPeriod) ?: PeriodEntry(
      dateString = dateStr,
      isPeriodDay = newIsPeriod,
      flowIntensity = if (newIsPeriod) "Medium" else ""
    )
    periodDao.insertOrUpdate(updated)
    return newIsPeriod
  }

  /**
   * Predicts the next cycle window based on the most recent logged period date
   * and average cycle length.
   */
  suspend fun predictNextPeriod(): Pair<LocalDate, LocalDate>? {
    val periodDays = periodDao.getPeriodDays()
    if (periodDays.isEmpty()) return null

    val dates = periodDays.mapNotNull {
      try {
        LocalDate.parse(it.dateString, DATE_FORMATTER)
      } catch (_: Exception) {
        null
      }
    }.sortedDescending()

    val lastPeriodStart = dates.firstOrNull() ?: return null
    val cycleLength = getAverageCycleLength()
    val duration = getPeriodDuration()

    val predictedStart = lastPeriodStart.plusDays(cycleLength.toLong())
    val predictedEnd = predictedStart.plusDays((duration - 1).toLong())
    return Pair(predictedStart, predictedEnd)
  }
}
