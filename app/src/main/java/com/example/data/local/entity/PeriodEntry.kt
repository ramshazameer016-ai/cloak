package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_entries")
data class PeriodEntry(
  @PrimaryKey
  val dateString: String, // Format: YYYY-MM-DD
  val isPeriodDay: Boolean = false,
  val flowIntensity: String = "", // "None", "Light", "Medium", "Heavy"
  val symptoms: String = "", // Comma-separated list of symptoms
  val mood: String = "", // e.g. "Calm", "Happy", "Crampy"
  val notes: String = ""
)
