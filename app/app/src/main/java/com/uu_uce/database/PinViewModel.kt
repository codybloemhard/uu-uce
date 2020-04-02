package com.uu_uce.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PinViewModel(application: Application) : AndroidViewModel(application) {

    private val pinRepository: PinRepository

    val allPinData: LiveData<List<PinData>>

    init {
        val pinDao = UceRoomDatabase.getDatabase(application, viewModelScope).pinDao()
        pinRepository = PinRepository(pinDao)
        allPinData = pinRepository.allPins
    }

    fun insert(pin: PinData) = viewModelScope.launch {
        pinRepository.insert(pin)
    }

    fun tryUnlock(pid : Int, predPids : List<Int>, action : (() -> Unit)) = viewModelScope.launch {
        pinRepository.tryUnlock(pid, predPids, action)
    }

    fun completePin(pid : Int, followPids : List<Int>) = viewModelScope.launch {
        pinRepository.setStatus(pid, 2)
        if(followPids[0] != -1)
            pinRepository.setStatuses(followPids, -1)
    }
}