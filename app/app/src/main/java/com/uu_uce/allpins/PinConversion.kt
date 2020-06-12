package com.uu_uce.allpins

import android.app.Activity
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.mergedPinBackground
import com.uu_uce.pins.SinglePin
import com.uu_uce.pins.PinContent
import com.uu_uce.services.UTMCoordinate

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

class PinConversion(val activity: Activity){
    companion object {
        fun stringToUtm(coord: String): UTMCoordinate {
            val regex = "(\\d+|[a-zA-Z])".toRegex()
            val s = regex.findAll(coord)
            return UTMCoordinate(
                s.elementAt(0).value.toInt(),
                s.elementAt(1).value.first(),
                s.elementAt(2).value.toFloat(),
                s.elementAt(4).value.toFloat())
        }

        fun difficultyToBackground(difficulty: Int, activity: Activity, resource: Resources): Bitmap {
            val color = when (difficulty) {
                0 -> ContextCompat.getColor(activity, R.color.HighBlue) //Neutral
                1 -> ContextCompat.getColor(activity, R.color.ReptileGreen)
                2 -> ContextCompat.getColor(activity, R.color.OrangeHibiscus)
                3 -> ContextCompat.getColor(activity, R.color.Desire)
                mergedPinBackground -> ContextCompat.getColor(activity, R.color.Boyzone)
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
            return drawableToBitmap(background)
        }

        fun typeToIcon(type: String, resource: Resources): Drawable {
            val image = when (type) {
                "TEXT"      -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text, null)   ?: error("image not found")
                "IMAGE"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image, null)  ?: error("image not found")
                "VIDEO"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video, null)  ?: error("image not found")
                "MCQUIZ"    -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quiz, null)   ?: error("image not found")
                "MERGEDPIN" -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_stack, null)    ?: error("image not found")
                "TASK"    -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)      ?: error("image not found")
                else        -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)  ?: error("image not found")
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
    }

    private val resource = activity.resources

    private fun stringToPinContent(content: String): PinContent {
        return PinContent(content, activity, false)
    }

    private fun stringToIds(ids : String) : List<String>{
        return ids.split(',').map{s -> s}
    }

    fun pinDataToPin(pinData : PinData, viewModel : PinViewModel): SinglePin {
        val pin = SinglePin(
            pinData.pinId,
            stringToUtm(pinData.location), //location
            pinData.title,
            stringToPinContent(pinData.content),
            difficultyToBackground(pinData.difficulty, activity, resource),
            typeToIcon(pinData.type, resource),
            pinData.status,
            stringToIds(pinData.predecessorIds),
            stringToIds(pinData.followIds),
            viewModel
        )
        pin.content.parent = pin
        return pin
    }

    fun fieldbookEntryToPin(entry: FieldbookEntry, viewModel: FieldbookViewModel) : SinglePin {
        return SinglePin(
            entry.id.toString(),
            stringToUtm(entry.location),
            entry.title,
            PinContent(entry.content, activity, true),
            difficultyToBackground(0, activity, resource),
            typeToIcon("", resource),
            2,
            listOf(),
            listOf(),
            viewModel
        ).apply {
            content.parent = this
        }
    }
}
