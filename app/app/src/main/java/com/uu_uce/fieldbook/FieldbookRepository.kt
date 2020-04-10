package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import com.uu_uce.database.FieldbookDao

class FieldbookRepository(private val fieldbookDao: FieldbookDao) {

    val allFielbookEntries: LiveData<List<FieldbookEntry>> = fieldbookDao.getAllFieldbookEntries()

    suspend fun insert(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.insert(fieldbookEntry)
    }

    suspend fun delete(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.delete(fieldbookEntry)
    }
}