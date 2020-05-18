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
    fun getAll() : LiveData<List<FieldbookEntry>>

    @Insert
    suspend fun insert(entry: FieldbookEntry)

    @Delete
    suspend fun delete(entry: FieldbookEntry)

    @Query("DELETE from fieldbook")
    suspend fun deleteAll()

    @Query("SELECT * from fieldbook where LOWER(title) LIKE LOWER(:search)")
    suspend fun search(search : String) : List<FieldbookEntry>

    @Query("UPDATE fieldbook SET content = :changedContent where id = :entryId")
    suspend fun updateContent(changedContent : String, entryId : Int)
}