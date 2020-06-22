package com.uu_uce.allpins

import androidx.lifecycle.LiveData
import com.uu_uce.database.PinDao
import org.jetbrains.annotations.TestOnly
import kotlin.math.max

/**
 * A communication layer between the ViewModel and the database
 *
 * @property[pinDao] the data access object for the pin table
 * @constructor creates the PinRepository
 */
class PinRepository(private val pinDao : PinDao){

    val allPins: LiveData<List<PinData>> = pinDao.getAllLivePins()

    /**
     * Insert a pinData object into the database
     * @param pindata the PinData object to be inserted
     */
    suspend fun insert(pindata: PinData) {
        pinDao.insert(pindata)
    }

    /**
     * Unlocks a pin if all its predecessors are completed and executes an action upon unlocking.
     * @param pid the id of the pin which should be unlocked.
     * @param predPids the id's of the predecessors of the pin that is to be unlocked.
     * @param action the action to be executed if the pin was successfully unlocked.
     */
    suspend fun tryUnlock(pid: String, predPids: List<String>, action: (() -> Unit)) {
        if (pinDao.getStatus(pid) > 0) {
            action()
            return
        }
        val statuses = pinDao.getStatuses(predPids)

        if (statuses.all { i -> i == 2 }) {
            // Set unlocked
            pinDao.setStatus(pid, 1)
        } else {
            // Set locked
            pinDao.setStatus(pid, 0)
        }
        action()
    }

    /**
     * Sets the status of a pin in the database.
     * @param[pid] the id of the pin whose status is to be set.
     * @param value the value which the pin's status will be set to.
     */
    suspend fun setStatus(pid: String, value: Int) {
        pinDao.setStatus(pid, value)
    }

    /**
     * Sets the status of multiple pins to a single value at the same time.
     * @param[pids] the ids of the pins whose status is to be set.
     * @param[value] the value which the pins' statuses will be set to.
     */
    suspend fun setStatuses(pids: List<String>, value: Int) {
        pinDao.setStatuses(pids, value)
    }

    /**
     * Find pins whose titles match the queried string.
     * @param[searchText] the string that was queried.
     * @param[action] the action to be executed with the PinData that matched the query.
     */
    suspend fun searchPins(searchText: String, action: ((List<PinData>?) -> Unit)) {
        if (searchText.count() > 0) {
            action(pinDao.searchPins("%$searchText%"))
        } else {
            action(pinDao.getAllPins())
        }
    }

    /**
     * The
     */
    suspend fun getPins(pinIds: List<String>, action: (List<PinData>) -> Unit) {
        action(pinDao.getPins(pinIds))
    }

    suspend fun getContent(list: MutableList<String>, action: (() -> Unit)) {
        for (content in pinDao.getContent()) {
            list.add(content)
        }
        action()
    }

    // Updates old pins to match the new data and removes pins that are not in the new data
    suspend fun updatePins(newPinData : List<PinData>, onCompleteAction : (() -> Unit)){
        fun pinNeedsUpdate(oldPin : PinData, newPin : PinData) : Boolean{
            if(oldPin.title != newPin.title) return true
            if(oldPin.type != newPin.type) return true
            if(oldPin.status < newPin.status) return true
            if(oldPin.startStatus != newPin.startStatus) return true
            if(oldPin.difficulty != newPin.difficulty) return true
            if(oldPin.location != newPin.location) return true
            if(oldPin.predecessorIds != newPin.predecessorIds) return true
            if(oldPin.followIds != newPin.followIds) return true
            if(oldPin.content != newPin.content) return true
            return false
        }

        val pins = pinDao.getAllPins()
        if (pins.count() == 0) {
            // Insert pins in database
            for (pin in newPinData) {
                pinDao.insert(pin)
            }
            return
        }

        val pinsMap = mutableMapOf<String, PinData>()
        val pinsDeleted = mutableMapOf<String, Boolean>()
        val newPins = mutableListOf<PinData>()
        for (pin in pins) {
            // Insert old pins
            pinsMap[pin.pinId] = pin
            pinsDeleted[pin.pinId] = true
        }
        for (newPin in newPinData) {
            if (pinsMap.containsKey(newPin.pinId)) {
                // Update existing pin
                val oldPin = pinsMap[newPin.pinId]
                pinsDeleted[newPin.pinId] = false

                if(pinNeedsUpdate(oldPin!!, newPin)){
                    val oldStatus = if(oldPin.startStatus == oldPin.status){
                        newPin.startStatus
                    }
                    else{
                        max(oldPin.status, newPin.status)
                    }

                    pinsMap[newPin.pinId] = newPin
                    pinsMap[newPin.pinId]!!.status = oldStatus
                }
                else{
                    pinsMap.remove(newPin.pinId)
                }
            }
            else {
                // Insert new pin
                newPins.add(newPin)
            }
        }
        pinDao.deletePins(pinsDeleted.toList().filter{ (_,v) -> v }.map{ (k,_) -> k })
        pinDao.updatePins(pinsMap.toList().map{ (_,v) -> v })
        pinDao.insertAll(newPins)
        onCompleteAction()
    }

    suspend fun reloadPins(action : ((List<PinData>) -> (Unit))){
        action(pinDao.getAllPins())
    }

    @TestOnly
    suspend fun setPins(newData : List<PinData>) {
        pinDao.updateData(newData)
    }
}