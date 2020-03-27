package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.uu_uce.database.PinData

@Dao
interface FieldbookDao {

    @Query("SELECT * from fieldbook")
    fun getAllFieldbookEntries() : List<FieldbookEntry>


    @Insert
    fun insert(entry: FieldbookEntry)


    @Query("DELETE from fieldbook")
    fun deleteAllFieldbookEntries()
}