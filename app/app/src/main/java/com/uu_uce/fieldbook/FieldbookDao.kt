package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import androidx.room.Insert
import androidx.room.Query
import com.uu_uce.database.PinData

interface FieldbookDao {

    @Query("SELECT * from fieldbook")
    suspend fun getAllFieldbookEntries() : LiveData<List<FieldbookEntry>>


    @Insert
    suspend fun insert(entry: FieldbookEntry)


    @Query("DELETE from fieldbook")
    suspend fun deleteAllFieldbookEntries()
}