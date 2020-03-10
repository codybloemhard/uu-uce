package com.uu_uce.database

import androidx.lifecycle.LiveData

class PinRepository(private val pinDao : PinDao) {

    val allPins: LiveData<List<PinData>> = pinDao.getAllPins()

    suspend fun insert(pindata: PinData) {
        pinDao.insert(pindata)
    }
}