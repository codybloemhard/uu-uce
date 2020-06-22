package com.uu_uce.allpins

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.uu_uce.database.UceRoomDatabase
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly

/**
 * Manages and stores data from the database
 * and makes sure it survives through the entire LifeCycle
 *
 * @param[application] the context for the ViewModel
 * @constructor makes a ViewModel
 */
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

    /**
     * Insert a pinData object into the database
     * @param[pin] the PinData object to be inserted
     */
    fun insert(pin: PinData) = viewModelScope.launch {
        pinRepository.insert(pin)
    }

    /**
     * Unlocks a pin if all its predecessors are completed and executes an action upon unlocking.
     * @param[pid] the id of the pin which should be unlocked.
     * @param[predPids] the id's of the predecessors of the pin that is to be unlocked.
     * @param[action] the action to be executed if the pin was successfully unlocked.
     */
    fun tryUnlock(pid : String, predPids : List<String>, action : (() -> Unit)) = viewModelScope.launch {
        pinRepository.tryUnlock(pid, predPids, action)
    }

    /**
     * Sets the status of a specified pin to completed(2) and informs all following pins that they need to be reloaded.
     * @param[pid] the pin that is to be completed.
     * @param[followPids] the ids of all following pins of the pin that is to be completed.
     */
    fun completePin(pid : String, followPids : List<String>) = viewModelScope.launch {
        pinRepository.setStatus(pid, 2)
        if(followPids[0] != "") {
            pinRepository.setStatuses(followPids, -1)
        }
    }

    /**
     * Find pins whose titles match the queried string.
     * @param[searchText] the string that was queried.
     * @param[action] the action to be executed with the PinData that matched the query.
     */
    fun searchPins(searchText : String, action : ((List<PinData>?) -> Unit)) = viewModelScope.launch {
        pinRepository.searchPins(searchText, action)
    }

    /**
     * Adds the content of each pin to a list.
     * @param[list] the list to which all content will be added.
     * @param[action] the action to be executed when the list of content is completed.
     */
    fun getContent(list : MutableList<String>, action : (() -> Unit))= viewModelScope.launch {
        pinRepository.getContent(list, action)
    }

    /**
     * Updates old pins to match the new data and removes pins that are not in the new data.
     * @param[pinList] the new data to which the database needs to be updated.
     * @param[onCompleteAction] the action to be executed when the database is updated.
     */
    fun updatePins(pinList : List<PinData>?, onCompleteAction : (() -> Unit)) = viewModelScope.launch {
        if(pinList != null){
            pinRepository.updatePins(pinList, onCompleteAction)
        }
    }

    /**
     * Executes funtion with all pins in the database, used for updating pins in memory
     * @param[action] action to be executed with all PinData in the database.
     */
    fun reloadPins(action : ((List<PinData>) -> (Unit))) = viewModelScope.launch {
        pinRepository.reloadPins(action)
    }

    /**
     * Executes a function with the PinData of specified pins as input.
     * @param[pinIds] the ids of the pins that the function should take as input.
     * @param[action] the function which takes a list of PinData as input.
     */
    fun getPins(pinIds : List<String>, action : ((List<PinData>) -> (Unit))) = viewModelScope.launch {
        pinRepository.getPins(pinIds, action)
    }

    @TestOnly
    /**
     * Sets the database for testing purposes
     * @param[newData] the data that should be in the database during testing.
     */
    fun setPins(newData : List<PinData>) = viewModelScope.launch {
        pinRepository.setPins(newData)
    }
}