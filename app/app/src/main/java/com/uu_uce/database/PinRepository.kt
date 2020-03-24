package com.uu_uce.database

import androidx.lifecycle.LiveData
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger

class PinRepository(private val pinDao : PinDao){

    val allPins: LiveData<List<PinData>> = pinDao.getAllPins()

    suspend fun insert(pindata: PinData){
        pinDao.insert(pindata)
    }

    suspend fun getStatus(pid : Int, predPids : List<Int>, action : (() -> Unit)){
        val status = predPids.map{ prePid -> pinDao.getStatus(prePid) == 2}

        if(status.all{b -> b}){
            pinDao.setStatus(pid, 1)
        }
        action()
    }

    suspend fun setStatus(pid : Int, value : Int){
        pinDao.setStatus(pid, value)
    }
}