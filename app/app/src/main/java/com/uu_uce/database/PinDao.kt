package com.uu_uce.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.uu_uce.allpins.PinData

@Dao
interface PinDao {

    @Query("SELECT * from pins")
    fun getAllPins() : LiveData<List<PinData>>

    @Query("SELECT status from pins where pinId = :pid")
    suspend fun getStatus(pid: Int) : Int

    @Query("SELECT status from pins where pinId in (:pids)")
    suspend fun getStatuses(pids: List<Int>) : List<Int>

    @Query("UPDATE pins SET status = :newStatus where pinId = :pid")
    suspend fun setStatus(pid : Int, newStatus : Int)

    @Query("UPDATE pins SET status = :newStatus where pinId in (:pids)")
    suspend fun setStatuses(pids : List<Int>, newStatus : Int)

    @Insert
    suspend fun insert(pin: PinData)

    @Insert
    suspend fun insertAll(pins: List<PinData>)

    @Query("DELETE from pins")
    suspend fun deleteAllPins()

    @Transaction
    suspend fun updateData(pins: List<PinData>) {
        deleteAllPins()
        insertAll(pins)
    }
}