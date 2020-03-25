package com.uu_uce.fieldbook

import androidx.room.Entity
import androidx.room.PrimaryKey

//TODO: What info are we going to store and how. Use JSON parser?

@Entity(tableName = "mypins")
open class FieldbookEntry(
    @PrimaryKey(autoGenerate = true) var id : Int,
    var location       : String,
    var dateTime       : String,
    var content        : List<Content>,
    var size           : Int
)

class Content(val type: String, val content: String)