package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.uu_uce.database.PinData

@Dao
interface FieldbookDao {

    @Query("SELECT * from fieldbook")
    fun getAllFieldbookEntries() : LiveData<MutableList<FieldbookEntry>>


    @Insert
    suspend fun insert(entry: FieldbookEntry)


    @Query("DELETE from fieldbook")
    suspend fun deleteAllFieldbookEntries()
}