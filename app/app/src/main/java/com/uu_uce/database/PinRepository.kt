package com.uu_uce.database

import androidx.lifecycle.LiveData

class PinRepository(private val pinDao : PinDao){

    val allPins: LiveData<List<PinData>> = pinDao.getAllPins()
    val allUnlockedPins: LiveData<List<PinData>> = pinDao.getAllUnlockedPins()

    suspend fun insert(pindata: PinData){
        pinDao.insert(pindata)
    }

    suspend fun tryUnlock(pid : Int, predPids : List<Int>, action : (() -> Unit)){
        if(pinDao.getStatus(pid) > 0) return
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

    suspend fun createArrays(action : ((Int) -> Unit)){
        val pinCount = pinDao.countPins()
        action(pinCount)
    }
}