package com.uu_uce.allpins

import androidx.lifecycle.LiveData
import com.uu_uce.database.PinDao
import org.jetbrains.annotations.TestOnly
import kotlin.math.max

class PinRepository(private val pinDao : PinDao){

    val allPins: LiveData<List<PinData>> = pinDao.getAllLivePins()

    suspend fun insert(pindata: PinData){
        pinDao.insert(pindata)
    }

    suspend fun tryUnlock(pid : String, predPids : List<String>, action : (() -> Unit)){
        if(pinDao.getStatus(pid) > 0) {
            action()
            return
        }
        val statuses = pinDao.getStatuses(predPids)

        if(statuses.all{ i -> i == 2}){
            // Set unlocked
            pinDao.setStatus(pid, 1)
        }
        else{
            // Set locked
            pinDao.setStatus(pid, 0)
        }
        action()
    }

    suspend fun setStatus(pid : String, value : Int){
        pinDao.setStatus(pid, value)
    }

    suspend fun setStatuses(pids : List<String>, value : Int){
        pinDao.setStatuses(pids, value)
    }

    suspend fun searchPins(searchText : String, action : ((List<PinData>?) -> Unit)){
        if(searchText.count() > 0){
            action(pinDao.searchPins("%$searchText%"))
        }
        else{
            action(pinDao.getAllPins())
        }
    }

    suspend fun getContent(list : MutableList<String>, action : (() -> Unit)){
        for(content in pinDao.getContent()){
            list.add(content)
        }
        action()
    }

    // Updates old pins to match the new data and removes pins that are not in the new data
    suspend fun updatePins(newPinData : List<PinData>){
        fun pinNeedsUpdate(oldPin : PinData, newPin : PinData) : Boolean{
            if(oldPin.title != newPin.title) return true
            if(oldPin.type != newPin.type) return true
            if(oldPin.status > newPin.status) return true
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
                    val oldStatus = oldPin.status
                    pinsMap[newPin.pinId] = newPin
                    pinsMap[newPin.pinId]!!.status = max(oldStatus, newPin.status)
                }
                else{
                    pinsMap.remove(newPin.pinId)
                }
            }
            else {
                // Insert new pin
                pinsMap[newPin.pinId] = newPin
            }
        }
        pinDao.deletePins(pinsDeleted.toList().filter{ (_,v) -> v }.map{ (k,_) -> k })
        pinDao.updatePins(pinsMap.toList().map{ (_,v) -> v })
    }

    @TestOnly
    suspend fun setPins(newData : List<PinData>) {
        pinDao.updateData(newData)
    }
}