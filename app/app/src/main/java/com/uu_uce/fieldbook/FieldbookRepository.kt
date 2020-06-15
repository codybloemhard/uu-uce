package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import com.uu_uce.allpins.PinData
import com.uu_uce.database.FieldbookDao

class FieldbookRepository(private val fieldbookDao: FieldbookDao) {

    val allEntries: LiveData<List<FieldbookEntry>> = fieldbookDao.getAll()

    suspend fun getContent(entryId: Int, action: ((FieldbookEntry) -> Unit)) {
        action(fieldbookDao.getContent(entryId))
    }

    suspend fun insert(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.insert(fieldbookEntry)
    }

    suspend fun delete(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.delete(fieldbookEntry)
    }

    suspend fun deleteAll(){
        fieldbookDao.deleteAll()
    }

    suspend fun search(searchText : String, action : ((List<FieldbookEntry>?) -> Unit)){
        if(searchText.count() > 0){
            action(fieldbookDao.search("%$searchText%"))
        }
        else{
            action(allEntries.value)
        }
    }

    suspend fun update(title: String, content: String, entryId: Int) {
        fieldbookDao.update(title, content, entryId)
    }

    suspend fun getPins(pinIds : List<String>, action: (List<FieldbookEntry>) -> Unit){
        action(fieldbookDao.getPins(pinIds))
    }
}