package com.uu_uce.fieldbook

import android.icu.text.CaseMap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fieldbook")
class FieldbookEntry(
    var title          : String,
    var location       : String,
    var dateTime       : String,
    var content        : String
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var size: Int = 60
}