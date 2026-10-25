package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PeriodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
  @Query("SELECT * FROM period_entries ORDER BY dateString DESC")
  fun getAllEntriesFlow(): Flow<List<PeriodEntry>>

  @Query("SELECT * FROM period_entries WHERE dateString = :dateString LIMIT 1")
  suspend fun getEntryByDate(dateString: String): PeriodEntry?

  @Query("SELECT * FROM period_entries WHERE isPeriodDay = 1 ORDER BY dateString DESC")
  suspend fun getPeriodDays(): List<PeriodEntry>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(entry: PeriodEntry)

  @Query("DELETE FROM period_entries WHERE dateString = :dateString")
  suspend fun deleteEntry(dateString: String)
}
