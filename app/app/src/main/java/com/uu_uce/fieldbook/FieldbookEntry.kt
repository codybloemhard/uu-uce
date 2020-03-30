package com.uu_uce.fieldbook

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fieldbook")
open class FieldbookEntry(
    var location       : String,
    var dateTime       : String,
    var content        : String
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var size: Int = 60
}