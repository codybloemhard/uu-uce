package com.uu_uce.database

import androidx.lifecycle.LiveData

class PinRepository(private val pinDao : PinDao){

    val allPins: LiveData<List<PinData>> = pinDao.getAllPins()

    suspend fun insert(pindata: PinData){
        pinDao.insert(pindata)
    }

    suspend fun tryUnlock(pid : Int, predPids : List<Int>, action : (() -> Unit)){
        if(pinDao.getStatus(pid) > 0) return
        val status = predPids.map{ prePid -> pinDao.getStatus(prePid) == 2}

        if(status.all{b -> b}){
            pinDao.setStatus(pid, 1)
            action()
        }
    }

    suspend fun setStatus(pid : Int, value : Int){
        pinDao.setStatus(pid, value)
    }
}