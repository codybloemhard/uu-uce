package com.uu_uce.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.uu_uce.fieldbook.FieldbookEntry

@Dao
interface FieldbookDao {

    @Query("SELECT * from fieldbook")
    fun getAll() : LiveData<List<FieldbookEntry>>

    @Insert
    suspend fun insert(entry: FieldbookEntry)

    @Delete
    suspend fun delete(entry: FieldbookEntry)
}