package com.uu_uce.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "pins")
open class PinData(
    @PrimaryKey(autoGenerate = true) var id : Int,
    var location       : String,
    var difficulty     : Int,
    var type           : String,
    var title          : String,
    var content        : String,
    var size           : Int
)