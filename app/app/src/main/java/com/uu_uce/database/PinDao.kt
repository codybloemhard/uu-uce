package com.uu_uce.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.uu_uce.allpins.PinData
import com.uu_uce.views.pinsUpdated

@Dao
interface PinDao {

    @Query("SELECT * from pins")
    fun getAllLivePins() : LiveData<List<PinData>>

    @Query("SELECT * from pins")
    suspend fun getAllPins() : List<PinData>

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

    @Update
    suspend fun updatePins(pins : List<PinData>)

    @Insert
    suspend fun insertAll(pins: List<PinData>)

    @Query("SELECT * from pins where pinId in (:pids)")
    suspend fun getPins(pids: List<String>) : List<PinData>

    @Query("DELETE from pins")
    suspend fun deleteAllPins()

    @Query("DELETE from pins WHERE pinId IN (:pinIds)")
    suspend fun deletePins(pinIds : List<String>)

    @Transaction
    suspend fun updateData(pins: List<PinData>) {
        deleteAllPins()
        insertAll(pins)
        pinsUpdated.setValue(true)
    }
}


