package com.uu_uce.fieldbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.uu_uce.pinDatabase.UceRoomDatabase
import kotlinx.coroutines.launch

class FieldbookViewModel(application: Application): AndroidViewModel(application) {

    private val fieldbookRepository: FieldbookRepository

    val allFieldbookEntries: LiveData<MutableList<FieldbookEntry>>

    init {
        val fieldbookDao = UceRoomDatabase.getDatabase(application, viewModelScope).fieldbookDao()
        fieldbookRepository = FieldbookRepository(fieldbookDao)
        allFieldbookEntries = fieldbookRepository.allFielbookEntries
    }

    fun insert (fieldbookEntry: FieldbookEntry) = viewModelScope.launch {
        fieldbookRepository.insert(fieldbookEntry)
    }

    fun delete (fieldbookEntry: FieldbookEntry) = viewModelScope.launch {
        fieldbookRepository.delete(fieldbookEntry)
    }
}