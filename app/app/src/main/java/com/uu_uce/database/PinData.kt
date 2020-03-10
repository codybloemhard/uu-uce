package com.uu_uce.database

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.PinType
import com.uu_uce.services.UTMCoordinate


@Entity(tableName = "pins")
open class PinData(
    @PrimaryKey(autoGenerate = true) var id : Int,
    var location       : String,
    var difficulty     : Int,
    var type           : String,
    var title          : String,
    var content        : String, //JSON?
    var size           : Int
)