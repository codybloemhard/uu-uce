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
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.SinglePin
import com.uu_uce.services.UTMCoordinate

/**
 * Converts a Drawable to a Bitmap
 *
 * @param[drawable] a drawable which is to be converted to a bitmap.
 * @return A bitmap which can be drawn using openGL.
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
                s.elementAt(4).value.toFloat()
            )
        }

        /**
         * Returns the background for a pin, representing its difficulty
         *
         * @param[difficulty] the difficulty of the pin - ranging from 0 (Neutral), 1 (Easy) to 3 (Hard) - which the color of the pin will be based on.
         * @param[activity] the current activity.
         * @return a bitmap colored in according to the supplied difficulty.
         */
        fun difficultyToBackground(difficulty: Int, activity: Activity): Bitmap {
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
            var background =
                ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_pin, null)
                    ?: error("Image not found")
            background = background.mutate()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                background.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
            } else {
                // Older versions will use depricated function
                @Suppress("DEPRECATION")
                background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
            return drawableToBitmap(background)
        }

        /**
         * Returns the icon for a pin, representing its type
         *
         * @param[type] the type of the pin you wish to get an icon for.
         * @param[resource] the resources to get drawables from.
         * @return a drawable according to the pin type.
         */
        fun typeToIcon(type: String, resource: Resources): Drawable {
            val image = when (type) {
                "TEXT" -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text, null)
                    ?: error("image not found")
                "IMAGE" -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image, null)
                    ?: error("image not found")
                "VIDEO" -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video, null)
                    ?: error("image not found")
                "MCQUIZ" -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quiz, null)
                    ?: error("image not found")
                "MERGEDPIN" -> ResourcesCompat.getDrawable(
                    resource,
                    R.drawable.ic_symbol_stack,
                    null
                ) ?: error("image not found")
                "TASK" -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)
                    ?: error("image not found")
                else -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null)
                    ?: error("image not found")
            }

            val color = Color.WHITE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                image.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
            } else {
                // Older versions will use depricated function
                @Suppress("DEPRECATION")
                image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
            return image
        }
    }

    private val resource = activity.resources

    /**
    <<<<<<< HEAD
     * Converts a JSON string to PinContent
     *
     * @param[content] a JSON string containing the content of a pin.
     * @param[fieldbookPin]
     * @return a parsed PinContent from the supplied content.
     */
    private fun stringToPinContent(content: String, fieldbookPin: Boolean): PinContent {
        return PinContent(content, activity, fieldbookPin)
    }

    /**
     * Converts the data from the database to a usable pin for the app
     *
     * @param[pinData] the PinData that is to be parsed into a Pin, as stored in the database
     * @param[viewModel] the viewModel that the Pin is to have access to, used for accessing the database
     * @return the Pin parsed from the supplied PinData.
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
            difficultyToBackground(pinData.difficulty, activity),
            typeToIcon(pinData.type, resource),
            pinData.status,
            pinData.predecessorIds.split(','),
            pinData.followIds.split(','),
            viewModel
        )
        pin.content.parent = pin
        return pin
    }

    /**
     * Converts the data from the database to a usable pin for the app
     *
     * @param[entry] the FieldbookEntry that is to be parsed to a Pin, as stored in the database
     * @param[viewModel] the viewModel that the Pin is to have access to, used for accessing the database
     * @return a drawable Pin parsed from the FieldbookEntry
     */
    fun fieldbookEntryToPin(
        entry: FieldbookEntry,
        viewModel: FieldbookViewModel
    ) : SinglePin {
        return SinglePin(
            entry.id.toString(),
            stringToUtm(entry.location),
            entry.title,
            PinContent(entry.content, activity, true),
            difficultyToBackground(0, activity),
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
