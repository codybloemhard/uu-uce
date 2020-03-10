package com.uu_uce.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PinRepository

    val allPinData: LiveData<List<PinData>>

    init {
        val pinDao = UceRoomDatabase.getDatabase(application, viewModelScope).pinDao()
        repository = PinRepository(pinDao)
        allPinData = repository.allPins
    }

    fun insert(pin: PinData) = viewModelScope.launch {
        repository.insert(pin)
    }
}