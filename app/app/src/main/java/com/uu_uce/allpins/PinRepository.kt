package com.uu_uce.allpins

import androidx.lifecycle.LiveData
import com.uu_uce.database.PinDao
import org.jetbrains.annotations.TestOnly

class PinRepository(private val pinDao : PinDao){

    val allPins: LiveData<List<PinData>> = pinDao.getAllPins()

    suspend fun insert(pindata: PinData){
        pinDao.insert(pindata)
    }

    suspend fun tryUnlock(pid : Int, predPids : List<Int>, action : (() -> Unit)){
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

    suspend fun setStatus(pid : Int, value : Int){
        pinDao.setStatus(pid, value)
    }

    suspend fun setStatuses(pids : List<Int>, value : Int){
        pinDao.setStatuses(pids, value)
    }

    suspend fun searchPins(searchText : String, action : ((List<PinData>?) -> Unit)){
        if(searchText.count() > 0){
            action(pinDao.searchPins("%$searchText%"))
        }
        else{
            action(allPins.value)
        }
    }

    @TestOnly
    suspend fun setPins(newData : List<PinData>) {
        pinDao.updateData(newData)
    }
}