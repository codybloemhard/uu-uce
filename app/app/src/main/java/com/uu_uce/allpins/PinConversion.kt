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
import com.uu_uce.pins.ContentBlockInterface
import com.uu_uce.pins.SinglePin
import com.uu_uce.pins.PinContent
import com.uu_uce.services.UTMCoordinate

/**
 * Converts a Drawable to a Bitmap
 *
 * @param[drawable] the background for a pin, as a Drawable
 * @return the background for a pin, as a Bitmap
 */
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

/**
 * Handles converting to a data to the Pin object
 *
 * @property[activity] the associated activity
 * @constructor creates a PinConversion
 */
class PinConversion(val activity: Activity) {

    companion object {
        /**
         * Converts a string containing location data to a UTMCoordinate
         *
         * @param[coord] a string containing information about the location,
         * formatted as "{UTM Zone: Int}{Hemisphere designator: Char}{Northern grid position}N{Eastern grid position}E"
         * @return the location as a UTM Coordinate
         */
        fun stringToUtm(coord: String): UTMCoordinate {
            val regex = "(\\d+|[a-zA-Z])".toRegex()
            val s = regex.findAll(coord)
            return UTMCoordinate(
                s.elementAt(0).value.toInt(),
                s.elementAt(1).value.first(),
                s.elementAt(2).value.toFloat(),
                s.elementAt(4).value.toFloat())
        }

    /**
     * Returns the background for a pin, representing its difficulty
     *
     * @param[difficulty] the difficulty for a pin, ranging from 0 (Neutral), 1 (Easy) to 3 (Hard)
     * @return the background for a pin, colored according to the difficulty
     */
    fun difficultyToBackground(difficulty: Int, activity: Activity, resource: Resources): Bitmap {
        val color = when (difficulty) {
            0 -> ContextCompat.getColor(activity, R.color.HighBlue) // Neutral
            1 -> ContextCompat.getColor(activity, R.color.ReptileGreen) // Easy
            2 -> ContextCompat.getColor(activity, R.color.OrangeHibiscus) // Medium
            3 -> ContextCompat.getColor(activity, R.color.Desire) // Hard
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

    /**
     * Returns the icon for a pin, representing its type
     *
     * @param[type] the type of a pin: could be "TEXT", "IMAGE", "VIDEO", "MCQUIZ"
     * @return the icon to be drawn on a pin, according to its type
     */
    fun typeToIcon(type: String, resource: Resources): Drawable {
        val image = when (type) {
            "TEXT"      -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text , null)
                ?: error("image not found")
            "IMAGE"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image, null)
                ?: error("image not found")
            "VIDEO"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video, null)
                ?: error("image not found")
            "MCQUIZ"    -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quiz , null)
                ?: error("image not found")
            "MERGEDPIN" -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_stack, null)
                ?: error("image not found")
            "TASK"      -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)
                ?: error("image not found")
            else        -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)
                ?: error("image not found")
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

    /**
     * Converts a JSON string to PinContent
     *
     * @param[content] the content to be shown in the pin, as a JSON string
     * @param[fieldbookPin]
     * @return the content shown in the pin
     */
    private fun stringToPinContent(content: String, fieldbookPin: Boolean): PinContent {
        return PinContent(content, activity, fieldbookPin)
    }

    /**
     * Converts a String to a List of Strings
     *
     * @param[ids] the ids of following or preceding pins, all stored to one string
     * @return the ids of following or preceding pins, as seperate Strings in a List
     */
    private fun stringToIds(ids : String) : List<String>{
        return ids.split(',').map{s -> s}
    }

    /**
     * Converts the data from the database to a usable pin for the app
     *
     * @param[pinData] the data for a pin, as stored in the database
     * @param[viewModel] used for accessing the database
     * @return a drawable pin, with the necessary functions
     */
    fun pinDataToPin(
        pinData: PinData,
        viewModel: PinViewModel
    ): SinglePin {
        val pin = SinglePin(
            pinData.pinId,
            stringToUtm(pinData.location), //location
            pinData.title,
            stringToPinContent(pinData.content, false),
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

    /**
     * Converts the data from the database to a usable pin for the app
     *
     * @param[entry] the data of a fieldbook entry, as stored in the database
     * @param[viewModel] used for accessing the database
     * @return a drawable pin, with the necessary functions
     */
    fun fieldbookEntryToPin(
        entry: FieldbookEntry,
        viewModel: FieldbookViewModel
    ) : SinglePin {
        return SinglePin(
            entry.id.toString(),
            stringToUtm(entry.location),
            entry.title,
            stringToPinContent(entry.content,  true),
            difficultyToBackground(0, activity, resource),
            typeToIcon("", resource),
            2,
            listOf(),
            listOf(),
            viewModel
        ).apply {
            this.content.parent = this
        }
    }
}
