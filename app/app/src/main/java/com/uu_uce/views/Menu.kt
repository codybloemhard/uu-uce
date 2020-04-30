package com.uu_uce.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams
import com.uu_uce.R

//view that holds most buttons, and can be dragged up and down
class Menu : RelativeLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var dragStatus = DragStatus.Down

    //various height variables
    val buttonPercent = 0.1f
    private var downY = 0f
    private var barY = 0f
    private var upY = 0f
    private var screenHeight = 0
    private var minScroll = 0f

    private lateinit var dragButton : ImageView

    //when screen height is known, it should be passed on to Menu to update its variables
    fun setScreenHeight(scrnHeight: Int, openBtnHeight: Int, scrollHeight: Int, lowerMenuHeight: Int){
        screenHeight = scrnHeight
        downY = screenHeight - openBtnHeight.toFloat()
        barY = downY - scrollHeight.toFloat()
        upY = barY - lowerMenuHeight.toFloat() * 1.2f
        updateLayoutParams{height = screenHeight}
        y = downY
        minScroll = screenHeight * 0.1f

        dragButton = findViewById(R.id.dragButton)
    }

    //when the button is released, snap to the closest position in the drag-direction
    fun snap(dx: Float, dy: Float){
        when {
            y < upY ->{
                up()
            }
            y < barY -> {
                if(dy < 0) up()
                else bar()
            }
            y < downY -> {
                if(dy < 0) bar()
                else down()
            }
            else -> {
                down()
            }
        }
    }

    //drag up/down by delta-y
    fun drag(dx: Float, dy: Float){
        y = maxOf(y + dy, minScroll)
    }

    //move to up position (everything visible)
    private fun up(){
        dragStatus = DragStatus.Up
        animate().y(upY)
        dragButton.setImageResource(R.drawable.ic_sprite_arrowdown)
    }

    //move to bar position (only upper row visible)
    private fun bar(){
        dragStatus = DragStatus.Bar
        animate().y(barY)
        dragButton.setImageResource(R.drawable.ic_sprite_arrowup)
    }

    //move to down position (only drag button visible)
    fun down(){
        dragStatus = DragStatus.Down
        animate().y(downY)
        dragButton.setImageResource(R.drawable.ic_sprite_arrowup)
    }

    //when button is tapped, cycle through positions
    fun dragButtonTap(){
        animate().y(y-100)
        when(dragStatus){
            DragStatus.Down ->{
                bar()
            }
            DragStatus.Bar ->{
                up()
            }
            DragStatus.Up ->{
                down()
            }
        }
    }
}

enum class DragStatus{
    Down, Bar, Up
}