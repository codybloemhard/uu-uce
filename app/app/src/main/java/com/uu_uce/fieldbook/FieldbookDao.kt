package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FieldbookDao {

    @Query("SELECT * from fieldbook")
    fun getAll() : LiveData<MutableList<FieldbookEntry>>

    @Insert
    suspend fun insert(entry: FieldbookEntry)

    @Delete
    suspend fun delete(entry: FieldbookEntry)

    @Query("DELETE from fieldbook")
    suspend fun deleteAll()
}