package com.example.data.repository

import com.example.data.local.dao.ContactDao
import com.example.data.local.entity.EmergencyContact
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {
  val contactsFlow: Flow<List<EmergencyContact>> = contactDao.getAllContactsFlow()

  suspend fun getContactsSync(): List<EmergencyContact> = contactDao.getAllContactsSync()

  suspend fun addContact(name: String, phoneNumber: String, relationship: String = ""): Result<Long> {
    val count = contactDao.getCount()
    if (count >= 3) {
      return Result.failure(IllegalStateException("Maximum of 3 emergency contacts reached."))
    }
    val cleanPhone = phoneNumber.trim().replace(" ", "").replace("-", "")
    if (cleanPhone.isBlank()) {
      return Result.failure(IllegalArgumentException("Phone number cannot be empty."))
    }
    val cleanName = name.trim().ifBlank { "Emergency Contact" }
    val newContact = EmergencyContact(
      name = cleanName,
      phoneNumber = cleanPhone,
      relationship = relationship,
      orderIndex = count
    )
    val id = contactDao.insertContact(newContact)
    return Result.success(id)
  }

  suspend fun deleteContact(id: Long) {
    contactDao.deleteById(id)
  }

  suspend fun clearAll() {
    contactDao.clearAll()
  }
}
