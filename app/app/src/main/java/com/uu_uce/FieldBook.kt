package com.uu_uce

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.database.UceRoomDatabase
import com.uu_uce.fieldbook.FieldbookAdapter
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.ui.createTopbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.text.DateFormat.getDateTimeInstance
import java.util.*

class FieldBook : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        createTopbar(this, "my fieldbook")

        val fieldbook = getFieldbookData()

        val recyclerView = findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FieldbookAdapter(this, fieldbook)
    }

    fun getFieldbookData(): MutableList<FieldbookEntry> {
        val fieldbookDao = UceRoomDatabase.getDatabase(this, MainScope()).fieldbookDao()
        return fieldbookDao.getAllFieldbookEntries()
    }
}
