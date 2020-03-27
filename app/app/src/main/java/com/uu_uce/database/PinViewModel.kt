package com.uu_uce.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PinRepository

    val allPinData: LiveData<List<PinData>>
    val allUnlockedPinData: LiveData<List<PinData>>

    init {
        val pinDao = UceRoomDatabase.getDatabase(application, viewModelScope).pinDao()
        repository = PinRepository(pinDao)
        allPinData = repository.allPins
        allUnlockedPinData = repository.allUnlockedPins
    }

    fun insert(pin: PinData) = viewModelScope.launch {
        repository.insert(pin)
    }

    fun tryUnlock(pid : Int, predPids : List<Int>, action : (() -> Unit)) = viewModelScope.launch {
        repository.tryUnlock(pid, predPids, action)
    }

    fun completePin(pid : Int, followPids : List<Int>) = viewModelScope.launch {
        repository.setStatus(pid, 2)
        if(followPids[0] != -1)
            repository.setStatuses(followPids, -1)
    }

    fun createArrays(action : ((Int) -> Unit)) = viewModelScope.launch {
        repository.createArrays(action)
    }
}