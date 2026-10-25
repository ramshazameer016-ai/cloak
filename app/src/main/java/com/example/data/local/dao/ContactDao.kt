package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
  @Query("SELECT * FROM emergency_contacts ORDER BY orderIndex ASC, id ASC LIMIT 3")
  fun getAllContactsFlow(): Flow<List<EmergencyContact>>

  @Query("SELECT * FROM emergency_contacts ORDER BY orderIndex ASC, id ASC LIMIT 3")
  suspend fun getAllContactsSync(): List<EmergencyContact>

  @Query("SELECT COUNT(*) FROM emergency_contacts")
  suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContact(contact: EmergencyContact): Long

  @Update
  suspend fun updateContact(contact: EmergencyContact)

  @Delete
  suspend fun deleteContact(contact: EmergencyContact)

  @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
  suspend fun deleteById(contactId: Long)

  @Query("DELETE FROM emergency_contacts")
  suspend fun clearAll()
}
