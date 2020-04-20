package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData

class FieldbookRepository(private val fieldbookDao: FieldbookDao) {

    val allFielbookEntries: LiveData<MutableList<FieldbookEntry>> = fieldbookDao.getAll()

    suspend fun insert(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.insert(fieldbookEntry)
    }

    suspend fun delete(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.delete(fieldbookEntry)
    }

    suspend fun deleteAll(){
        fieldbookDao.deleteAll()
    }
}