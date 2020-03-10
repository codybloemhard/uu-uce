package com.uu_uce.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PinDao {

    @Query("SELECT * from pins")
    fun getAllPins() : LiveData<List<PinData>>


    @Insert
    suspend fun insert(pin: PinData)


    @Query("DELETE from pins")
    suspend fun deleteAllPins()


}