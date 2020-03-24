package com.uu_uce.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PinDao {

    @Query("SELECT * from pins")
    fun getAllPins() : LiveData<List<PinData>>

    @Query("SELECT status from pins where pinId = :pid")
    suspend fun getStatus(pid: Int) : Int

    @Query("UPDATE pins SET status = :newStatus where pinId = :pid")
    suspend fun setStatus(pid : Int, newStatus : Int)

    @Insert
    suspend fun insert(pin: PinData)


    @Query("DELETE from pins")
    suspend fun deleteAllPins()


}