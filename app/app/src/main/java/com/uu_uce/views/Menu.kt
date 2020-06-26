package com.uu_uce.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams
import com.uu_uce.R
import kotlinx.android.synthetic.main.activity_geo_map.view.*

/**
 * view that holds the menu buttons, and can be dragged up and down
 */
class Menu : RelativeLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var dragStatus = DragStatus.Down
    private var disableBar = false

    //various height variables
    val buttonPercent = 0.1f
    var downY = 0f
    private var barY = 0f
    private var upY = 0f
    private var screenHeight = 0
    private var minScroll = 0f

    private lateinit var dragButton: ImageView

    /**
     * when screen height is known, it should be passed on to Menu to update its variables
     * @param[scrnHeight] height of the screen
     * @param[dragButton] height of the dragButton
     * @param[scrollHeight] height of scrollLayout containing toggle buttons
     * @param[lowerMenuHeight] height of the part of the menu below the toggles
     */
    fun setScreenHeight(scrnHeight: Int, dragButton: Int, scrollHeight: Int, lowerMenuHeight: Int) {
        disableBar = scrollHeight == 0 // Disable bar when no layers are available
        if (disableBar) {
            toggle_layer_scroll.visibility = View.GONE
        } else {
            toggle_layer_scroll.visibility = View.VISIBLE
        }

        dragStatus = DragStatus.Down
        screenHeight = scrnHeight
        downY = screenHeight - dragButton.toFloat()
        barY = downY - scrollHeight.toFloat()
        upY = barY - lowerMenuHeight.toFloat() * 1.2f
        updateLayoutParams { height = screenHeight }
        y = downY
        minScroll = screenHeight * 0.1f

        this.dragButton = findViewById(R.id.dragButton)
        this.dragButton.setImageResource(R.drawable.ic_sprite_arrowup)
    }

    /**
     * when the button is released, snap to the closest position in the drag-direction
     * @param[dx] velocity in x direction
     * @param[dy] velocity in y direction
     */
    fun snap(dx: Float, dy: Float) {
        when {
            y < upY -> {
                up()
            }
            y < barY -> {
                if (dy < 0) up()
                else bar()
            }
            y < downY -> {
                if (dy < 0) bar()
                else down()
            }
            else -> {
                down()
            }
        }
    }

    /**
     * drag the menu up/down
     * @param[dx] velocity in x direction
     * @param[dy] velocity in y direction
     */
    fun drag(dx: Float, dy: Float) {
        y = maxOf(y + dy, minScroll)
    }

    /**
     * move to up position (everything visible)
     */
    private fun up() {
        dragStatus = DragStatus.Up
        animate().y(upY)
        dragButton.setImageResource(R.drawable.ic_sprite_arrowdown)
    }

    /**
     * move to bar position (only upper row visible)
     */
    private fun bar() {
        dragStatus = DragStatus.Bar
        animate().y(barY)
        dragButton.setImageResource(R.drawable.ic_sprite_arrowup)
    }

    /**
     * move to down position (only drag button visible)
     */
    fun down() {
        dragStatus = DragStatus.Down
        animate().y(downY)
        dragButton.setImageResource(R.drawable.ic_sprite_arrowup)
    }

    /**
     * when button is tapped, cycle through positions
     */
    fun dragButtonTap() {
        animate().y(y - 100)
        when (dragStatus) {
            DragStatus.Down -> {
                if (disableBar) up()
                else bar()
            }
            DragStatus.Bar -> {
                up()
            }
            DragStatus.Up ->{
                down()
            }
        }
    }
}

/**
 * different statuses of the menu
 */
enum class DragStatus{
    Down, Bar, Up
}


