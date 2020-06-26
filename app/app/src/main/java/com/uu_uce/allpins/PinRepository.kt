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
     * @param[pindata] the PinData object to be inserted
     */
    suspend fun insert(pindata: PinData) {
        pinDao.insert(pindata)
    }

    /**
     * Unlocks a pin if all its predecessors are completed and executes an action upon unlocking.
     * @param[pid] the id of the pin which should be unlocked.
     * @param[predPids] the id's of the predecessors of the pin that is to be unlocked.
     * @param[action] the action to be executed if the pin was successfully unlocked.
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
     * @param[value] the value which the pin's status will be set to.
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
     * Executes a function with the PinData of specified pins as input.
     * @param[pinIds] the ids of the pins that the function should take as input.
     * @param[action] the function which takes a list of PinData as input.
     */
    suspend fun getPins(pinIds: List<String>, action: (List<PinData>) -> Unit) {
        action(pinDao.getPins(pinIds))
    }

    /**
     * Adds the content of each pin to a list.
     * @param[list] the list to which all content will be added.
     * @param[action] the action to be executed when the list of content is completed.
     */
    suspend fun getContent(list: MutableList<String>, action: (() -> Unit)) {
        for (content in pinDao.getContent()) {
            list.add(content)
        }
        action()
    }

    /**
     * Updates old pins to match the new data and removes pins that are not in the new data.
     * @param[newPinData] the new data to which the database needs to be updated.
     * @param[onCompleteAction] the action to be executed when the database is updated.
     */
    suspend fun updatePins(newPinData: List<PinData>, onCompleteAction: (() -> Unit)) {
        /**
         * Returns wheter or not the pin should be updated.
         * @param[oldPin] the PinData of a pin as it currently is in the database.
         * @param[newPin] the PinData as it is in the new data.
         * @retun true when the pin needs to be updated otherwise false.
         */
        fun pinNeedsUpdate(oldPin: PinData, newPin: PinData): Boolean {
            if (oldPin.title != newPin.title) return true
            if (oldPin.type != newPin.type) return true
            if (oldPin.status < newPin.status) return true
            if (oldPin.startStatus != newPin.startStatus) return true
            if (oldPin.difficulty != newPin.difficulty) return true
            if (oldPin.location != newPin.location) return true
            if (oldPin.predecessorIds != newPin.predecessorIds) return true
            if (oldPin.followIds != newPin.followIds) return true
            if (oldPin.content != newPin.content) return true
            return false
        }

        // Get current pins from database
        val pins = pinDao.getAllPins()
        if (pins.count() == 0) {
            // Insert pins into empty database
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
            // Assume all pins need to be deleted
            pinsDeleted[pin.pinId] = true
        }
        for (newPin in newPinData) {
            if (pinsMap.containsKey(newPin.pinId)) {
                // Update existing pin
                val oldPin = pinsMap[newPin.pinId]
                // Pin is still in database so does not need to be deleted
                pinsDeleted[newPin.pinId] = false

                if(pinNeedsUpdate(oldPin!!, newPin)) {
                    // Calculate new start status
                    val newStatus =
                        if (oldPin.startStatus == oldPin.status) {
                            newPin.startStatus
                        } else {
                            max(oldPin.status, newPin.status)
                        }

                    pinsMap[newPin.pinId] = newPin
                    pinsMap[newPin.pinId]!!.status = newStatus
                }
                else{
                    // Remove pin from list of pins that need update
                    pinsMap.remove(newPin.pinId)
                }
            } else {
                // Insert new pin
                newPins.add(newPin)
            }
        }
        // Delete all pins that were not in the new data
        pinDao.deletePins(pinsDeleted.toList().filter { (_, v) -> v }.map { (k, _) -> k })

        // Update pins that were changed
        pinDao.updatePins(pinsMap.toList().map { (_, v) -> v })

        // Insert new pins
        pinDao.insertAll(newPins)
        onCompleteAction()
    }

    /**
     * Executes funtion with all pins in the database, used for updating pins in memory
     * @param[action] action to be executed with all PinData in the database.
     */
    suspend fun reloadPins(action: ((List<PinData>) -> (Unit))) {
        action(pinDao.getAllPins())
    }

    @TestOnly
    /**
     * Sets the database for testing purposes
     * @param[newData] the data that should be in the database during testing.
     */
    suspend fun setPins(newData: List<PinData>) {
        pinDao.updateData(newData)
    }
}


