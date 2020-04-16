package com.uu_uce.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.uu_uce.fieldbook.FieldbookEntry

@Dao
interface FieldbookDao {

    @Query("SELECT * from fieldbook")
    fun getAllFieldbookEntries() : LiveData<List<FieldbookEntry>>

    @Insert
    suspend fun insert(entry: FieldbookEntry)

    @Query("DELETE from fieldbook")
    suspend fun deleteAllFieldbookEntries()

    @Delete
    suspend fun delete(entry: FieldbookEntry)
}