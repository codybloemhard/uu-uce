package com.uu_uce.fieldbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.uu_uce.database.UceRoomDatabase
import kotlinx.coroutines.launch

class FieldbookViewModel(application: Application): AndroidViewModel(application) {

    private val fieldbookRepository: FieldbookRepository

    val allFieldbookEntries: LiveData<List<FieldbookEntry>>

    init {
        val fieldbookDao = UceRoomDatabase.getDatabase(application, viewModelScope).fieldbookDao()
        fieldbookRepository = FieldbookRepository(fieldbookDao)
        allFieldbookEntries = fieldbookRepository.allEntries
    }

    fun getContent (entryId: Int, action : ((FieldbookEntry) -> Unit)) = viewModelScope.launch {
        fieldbookRepository.getContent(entryId, action)
    }

    fun insert (fieldbookEntry: FieldbookEntry) = viewModelScope.launch {
        fieldbookRepository.insert(fieldbookEntry)
    }

    fun delete (fieldbookEntry: FieldbookEntry) = viewModelScope.launch {
        fieldbookRepository.delete(fieldbookEntry)
    }

    fun deleteAll() = viewModelScope.launch {
        fieldbookRepository.deleteAll()
    }

    fun search(searchText : String, action : ((List<FieldbookEntry>?) -> Unit)) = viewModelScope.launch {
        fieldbookRepository.search(searchText, action)
    }

    fun update(title: String, content: String, entryId: Int) = viewModelScope.launch {
        fieldbookRepository.update(title, content, entryId)
    }
}