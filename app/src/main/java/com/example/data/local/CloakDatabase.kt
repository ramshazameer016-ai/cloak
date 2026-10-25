package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.PeriodDao
import com.example.data.local.entity.EmergencyContact
import com.example.data.local.entity.PeriodEntry

@Database(
  entities = [EmergencyContact::class, PeriodEntry::class],
  version = 2,
  exportSchema = false
)
abstract class CloakDatabase : RoomDatabase() {
  abstract fun contactDao(): ContactDao
  abstract fun periodDao(): PeriodDao

  companion object {
    @Volatile
    private var INSTANCE: CloakDatabase? = null

    fun getInstance(context: Context): CloakDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          CloakDatabase::class.java,
          "cloak_local.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
        INSTANCE = instance
        instance
      }
    }
  }
}
