package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FieldbookDao {

    @Query("SELECT * from fieldbook")
    fun getAll() : LiveData<MutableList<FieldbookEntry>>

    @Insert
    suspend fun insert(entry: FieldbookEntry)

    @Query("DELETE from fieldbook")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(entry: FieldbookEntry)

    @Insert
    suspend fun insertAll(pins: List<FieldbookEntry>)

    @Transaction
    suspend fun updateData(pins: List<FieldbookEntry>) {
        deleteAll()
        insertAll(pins)
    }
}