package com.uu_uce

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.fieldbook.FieldbookAdapter
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.ui.createTopbar
import java.text.DateFormat.getDateTimeInstance
import java.util.*

class FieldBook : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        createTopbar(this, "my fieldbook")

        val sdf = getDateTimeInstance()
        val currentDate = sdf.format(Date())

        val fieldbook : List<FieldbookEntry> =
            listOf(
                FieldbookEntry(
                    0,
                    "31N3149680N46777336E",
                    currentDate,
                    "[{\"tag\":\"TEXT\",\"text\":\"Dit is een faketekst. Alles wat hier staat is slechts om een indruk te geven van het grafische effect van tekst op deze plek. Wat u hier leest is een voorbeeldtekst. Deze wordt later vervangen door de uiteindelijke tekst, die nu nog niet bekend is. De faketekst is dus een tekst die eigenlijk nergens over gaat. Het grappige is, dat mensen deze toch vaak lezen. Zelfs als men weet dat het om een faketekst gaat, lezen ze toch door.\"},{\"tag\":\"IMAGE\",\"file_name\":\"test.png\"}]",
                    60
                ),
                FieldbookEntry(
                    1,
                    "31N3133680N46718336E",
                    currentDate,
                    "[{\"tag\":\"TEXT\",\"text\":\"Dit is een faketekst. Alles wat hier staat is slechts om een indruk te geven van het grafische effect van tekst op deze plek. Wat u hier leest is een voorbeeldtekst. Deze wordt later vervangen door de uiteindelijke tekst, die nu nog niet bekend is. De faketekst is dus een tekst die eigenlijk nergens over gaat. Het grappige is, dat mensen deze toch vaak lezen. Zelfs als men weet dat het om een faketekst gaat, lezen ze toch door.\"},{\"tag\":\"VIDEO\", \"file_name\":\"zoo.mp4\", \"thumbnail\":\"zoothumbnail.png\", \"title\":\"zoo video\"}]",
                    60
                )
            )

        val recyclerView = findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FieldbookAdapter(this, fieldbook)
    }
}
