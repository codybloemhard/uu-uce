package com.uu_uce.fieldbook

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import java.text.DateFormat.getDateTimeInstance
import java.util.*

class FieldBook : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        val sdf = getDateTimeInstance()
        val currentDate = sdf.format(Date())

        val fieldbook : List<FieldbookEntry> =
            listOf(
                FieldbookEntry(
                    0,
                    "31N3149680N46777336E",
                    currentDate,
                    listOf(
                        Content(
                            "TEXT",
                            "Lorem Ipsum"),
                        Content(
                            "IMAGE",
                            "file:///data/data/com.uu_uce/files/pin_content/images/test.png"
                        )
                    ),
                    60
                ),
                FieldbookEntry(
                    1,
                    "31N3133680N46718336E",
                    currentDate,
                    listOf(
                        Content(
                            "TEXT",
                            "Lorem Ipsum"),
                        Content(
                            "IMAGE",
                            "file:///data/data/com.uu_uce/files/pin_content/images/test.png"
                        )
                    ),
                    60
                )
            )

        val recyclerView = findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FieldbookAdapter(this, fieldbook)

        //onCreateToolbar(this, "my fieldbook")
    }
}
