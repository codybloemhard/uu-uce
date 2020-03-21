package com.uu_uce.database

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.PinType
import com.uu_uce.services.UTMCoordinate

class PinConversion(context: Context){

    private var resource = context.resources
    private fun stringToUtm(coord: String): UTMCoordinate {
        val regex = "(\\d+|[a-zA-Z])".toRegex()
        val s = regex.findAll(coord)
        return UTMCoordinate(s.elementAt(0).value.toInt()       ,
                             s.elementAt(1).value.first()       ,
                             s.elementAt(2).value.toDouble()/10    ,
                             s.elementAt(4).value.toDouble()/10)

    }

    private fun stringToPinType(type: String): PinType {
        return when(type){
            "TEXT" -> PinType.TEXT
            "IMAGE" -> PinType.IMAGE
            "VIDEO" -> PinType.VIDEO
            else    -> error("unknown pin type")
        }
    }

    private fun stringToPinContent(content: String): PinContent {
        return PinContent(content)
    }

    private fun stringToDrawable(type: String, difficulty: Int): Drawable {
        var s  = "ic_pin"
        s += when (type) {
            "TEXT"  -> "_text"
            "VIDEO" -> "_video"
            "IMAGE" -> "_picture"
            else -> {
                "_link"
            }
        }
        s += when (difficulty) {
            1 -> "_groen"
            2 -> "_oranje"
            3 -> "_rood"
            else -> {
                "_grijs"
            }
        }
        return ResourcesCompat.getDrawable(
            resource, resource.getIdentifier(s, "drawable",
             "com.uu_uce"
         ), null) ?: ResourcesCompat.getDrawable(resource, R.drawable.pin, null) ?: error ("Image not found")

    }

    fun pinDataToPin(pinData: PinData): Pin {
        return Pin(
            stringToUtm(pinData.location)           , //location
            pinData.difficulty                      ,
            stringToPinType(pinData.type)           ,
            pinData.title                           ,
            stringToPinContent(pinData.content)     ,
            stringToDrawable(pinData.type, pinData.difficulty)
        )

    }
}