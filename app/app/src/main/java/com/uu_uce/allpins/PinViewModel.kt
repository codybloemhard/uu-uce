package com.uu_uce.allpins

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.uu_uce.database.UceRoomDatabase
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly

class PinViewModel(application: Application) : AndroidViewModel(application) {

    private val pinRepository: PinRepository

    val allPinData: LiveData<List<PinData>>

    init {
        val pinDao = UceRoomDatabase.getDatabase(
            application,
            viewModelScope
        ).pinDao()
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

    fun searchPins(searchText : String, action : ((List<PinData>?) -> Unit)) = viewModelScope.launch {
        pinRepository.searchPins(searchText, action)
    }

    fun getContent(list : MutableList<String>, action : (() -> Unit))= viewModelScope.launch {
        pinRepository.getContent(list, action)
    }

    @TestOnly
    fun setPins(newData : List<PinData>) = viewModelScope.launch {
        pinRepository.setPins(newData)
    }
}