package com.uu_uce.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.uu_uce.allpins.PinData

@Dao
interface PinDao {

    @Query("SELECT * from pins ORDER BY location DESC")
    fun getAllPins() : LiveData<List<PinData>>

    @Query("SELECT content from pins")
    suspend fun getContent() : List<String>

    @Query("SELECT status from pins where pinId = :pid")
    suspend fun getStatus(pid: String) : Int

    @Query("SELECT status from pins where pinId in (:pids)")
    suspend fun getStatuses(pids: List<String>) : List<Int>

    @Query("SELECT * from pins where LOWER(title) LIKE LOWER(:search)")
    suspend fun searchPins(search : String) : List<PinData>

    @Query("UPDATE pins SET status = :newStatus where pinId = :pid")
    suspend fun setStatus(pid : String, newStatus : Int)

    @Query("UPDATE pins SET status = :newStatus where pinId in (:pids)")
    suspend fun setStatuses(pids : List<String>, newStatus : Int)

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