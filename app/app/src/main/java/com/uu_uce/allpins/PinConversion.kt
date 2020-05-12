package com.uu_uce.allpins

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinContent
import com.uu_uce.services.UTMCoordinate


class PinConversion(context: Context){

    companion object {
        fun stringToUtm(coord: String): UTMCoordinate {
            val regex = "(\\d+|[a-zA-Z])".toRegex()
            val s = regex.findAll(coord)
            return UTMCoordinate(s.elementAt(0).value.toInt()       ,
                s.elementAt(1).value.first()       ,
                s.elementAt(4).value.toDouble()/10    ,
                s.elementAt(2).value.toDouble()/10)
        }
    }

    private val resource = context.resources

    private fun stringToPinContent(content: String): PinContent {
        return PinContent(content)
    }

    private fun difficultyToBackground(difficulty: Int): Bitmap {
        val color = when (difficulty) {
            1 -> Color.parseColor("#5DB678")
            2 -> Color.parseColor("#F08135")
            3 -> Color.parseColor("#E83C5B")
            else -> {
                Color.parseColor("#696969") //Nice
            }
        }
        val background =  ResourcesCompat.getDrawable(resource, R.drawable.ic_pin, null) ?: error ("Image not found")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             background.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        }
        else{
            // Older versions will use depricated function
            background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        return drawableToBitmap(background)
    }

    private fun typeToIcon(type: String): Drawable {
        val image = when (type) {
            "TEXT"      -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text, null)     ?: error("image not found")
            "IMAGE"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image, null)    ?: error("image not found")
            "VIDEO"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video, null)    ?: error("image not found")
            "MCQUIZ"    -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quiz, null)    ?: error("image not found")
            else        -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)     ?: error("image not found")
        }

        val color = Color.WHITE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            image.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        }
        else{
            // Older versions will use depricated function
            image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        return image
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
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
