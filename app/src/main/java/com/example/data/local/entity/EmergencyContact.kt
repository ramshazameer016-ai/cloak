package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val phoneNumber: String,
  val relationship: String = "",
  val orderIndex: Int = 0
)
