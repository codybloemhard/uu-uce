package com.uu_uce.database

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.PinTextContent
import com.uu_uce.pins.PinType
import com.uu_uce.services.UTMCoordinate

class PinConversion(context: Context){

    private var resource = context.resources
    fun stringToUtm(coord: String): UTMCoordinate {
        val regex = "(\\d+|[a-zA-Z])".toRegex()
        val s = regex.findAll(coord)
        return UTMCoordinate(s.elementAt(0).value.toInt()       ,
                             s.elementAt(1).value.first()       ,
                             s.elementAt(2).value.toDouble()/10    ,
                             s.elementAt(4).value.toDouble()/10)

    }

    fun stringToPinType(type: String): PinType {

        return PinType.TEXT
    }

    fun stringToPinContent(type: String): PinContent {

        return PinTextContent()
    }

    fun stringToDrawable(type: String): Drawable {

        return ResourcesCompat.getDrawable(resource, R.drawable.pin, null) ?: error ("Image not found")
    }

    public fun pinDataToPin(pinData: PinData): Pin {
        return Pin(
            stringToUtm(pinData.location)           , //location
            pinData.difficulty                      ,
            stringToPinType(pinData.type)           ,
            pinData.title                           ,
            stringToPinContent(pinData.content)     ,
            60                              ,
            stringToDrawable(pinData.type)
        )

    }
}