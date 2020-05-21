package com.uu_uce.allpins

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pins")
open class PinData(
    @PrimaryKey
    var pinId          : String,
    var location       : String,
    var difficulty     : Int,
    var type           : String,
    var title          : String,
    var content        : String,
    var status         : Int,
    var predecessorIds : String,
    var followIds      : String
)
