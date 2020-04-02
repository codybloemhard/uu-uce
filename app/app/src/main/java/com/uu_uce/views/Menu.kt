package com.uu_uce.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams

class Menu : RelativeLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var dragStatus = DragStatus.Down

    val buttonPercent = 0.1f
    var downY = 0f
    var barY = 0f
    private var upY = 0f
    private var screenHeight = 0
    private var minScroll = 0f

    fun setScreenHeight(scrnHeight: Int, openBtnHeight: Int, scrollHeight: Int, lowerMenuHeight: Int){
        screenHeight = scrnHeight
        downY = screenHeight - openBtnHeight.toFloat()
        barY = downY - scrollHeight.toFloat()
        upY = barY - lowerMenuHeight.toFloat()
        updateLayoutParams{height = screenHeight}
        y = downY
        minScroll = screenHeight * 0.1f
    }

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

    fun drag(dx: Float, dy: Float){
        y = maxOf(y + dy, minScroll)
    }

    fun up(){
        dragStatus = DragStatus.Up
        animate().y(upY)
    }

    fun bar(){
        dragStatus = DragStatus.Bar
        animate().y(barY)
    }

    fun down(){
        dragStatus = DragStatus.Down
        animate().y(downY)
    }

    fun dragButtonTap(){
        animate().y(y-100)
        when(dragStatus){
            DragStatus.Down ->{
                bar()
            }
            DragStatus.Bar ->{
                down()
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