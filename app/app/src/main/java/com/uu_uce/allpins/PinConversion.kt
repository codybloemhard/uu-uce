package com.uu_uce.allpins

import android.app.Activity
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinContent
import com.uu_uce.services.UTMCoordinate

class PinConversion(val activity: Activity){

    companion object {
        fun stringToUtm(coord: String): UTMCoordinate {
            val regex = "(\\d+|[a-zA-Z])".toRegex()
            val s = regex.findAll(coord)
            return UTMCoordinate(
                s.elementAt(0).value.toInt(),
                s.elementAt(1).value.first(),
                s.elementAt(4).value.toDouble()/10,
                s.elementAt(2).value.toDouble()/10)
        }
    }

    private val resource = activity.resources

    private fun stringToPinContent(content: String): PinContent {
        return PinContent(content, activity)
    }

    private fun difficultyToBackground(difficulty: Int): Drawable {
        val color = when (difficulty) {
            1 -> ContextCompat.getColor(activity, R.color.ReptileGreen)
            2 -> ContextCompat.getColor(activity, R.color.OrangeHibiscus)
            3 -> ContextCompat.getColor(activity, R.color.Desire)
            else -> {
                ContextCompat.getColor(activity, R.color.TextGrey)
            }
        }
        var background =  ResourcesCompat.getDrawable(resource, R.drawable.ic_pin, null) ?: error ("Image not found")
        background = background.mutate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             background.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        }
        else{
            // Older versions will use depricated function
            @Suppress("DEPRECATION")
            background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        return background
    }

    private fun typeToIcon(type: String): Drawable {
        val image = when (type) {
            "TEXT"      -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text, null)     ?: error("image not found")
            "IMAGE"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image, null)    ?: error("image not found")
            "VIDEO"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video, null)    ?: error("image not found")
            "MCQUIZ"    -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quiz, null)     ?: error("image not found")
            else        -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)    ?: error("image not found")
        }

        val color = Color.WHITE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            image.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        }
        else{
            // Older versions will use depricated function
            @Suppress("DEPRECATION")
            image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        return image
    }

    private fun stringToIds(ids : String) : List<Int>{
        return ids.split(',').map{s -> s.toInt()}
    }

    fun pinDataToPin(pinData : PinData, viewModel : PinViewModel): Pin {
        val pin = Pin(
            pinData.pinId                           ,
            stringToUtm(pinData.location)           , //location
            pinData.title                           ,
            stringToPinContent(pinData.content)     ,
            difficultyToBackground(pinData.difficulty),
            typeToIcon(pinData.type)                ,
            pinData.status                          ,
            stringToIds(pinData.predecessorIds)     ,
            stringToIds(pinData.followIds)          ,
            viewModel
        )
        pin.getContent().parent = pin
        return pin
    }
}
