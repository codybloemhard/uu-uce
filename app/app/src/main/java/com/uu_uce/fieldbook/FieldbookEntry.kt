package com.uu_uce.fieldbook

import androidx.room.Entity
import androidx.room.PrimaryKey

//TODO: What info are we going to store and how. Use JSON parser?

@Entity(tableName = "fieldbook")
open class FieldbookEntry(
    @PrimaryKey(autoGenerate = true) var id : Int,
    var location       : String,
    var dateTime       : String,
    var content        : String,
    var size           : Int
)