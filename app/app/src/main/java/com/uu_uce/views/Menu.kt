package com.uu_uce.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams

class Menu : RelativeLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var dragStatus = DragStatus.Down

    val buttonPercent = 0.1f
    var downY = 0f
    var barY = 0f
    private var upY = 0f
    private var screenHeight = 0

    fun setScreenHeight(scrnHeight: Int, openBtnHeight: Int, scrollHeight: Int){
        screenHeight = scrnHeight
        downY = openBtnHeight.toFloat()
        barY = downY + scrollHeight.toFloat()
        upY = scrnHeight.toFloat()

        updateLayoutParams{height = screenHeight}
        y = screenHeight - downY
    }

    fun open(){
        dragStatus = DragStatus.Bar
        animate().y(screenHeight - barY)
    }

    fun close(){
        dragStatus = DragStatus.Down
        animate().y(screenHeight - downY)
    }

    fun dragButtonTap(){
        animate().y(y-100)
        when(dragStatus){
            DragStatus.Down ->{
                open()

            }
            DragStatus.Bar ->{
                close()
            }
            DragStatus.Up ->{
            }
        }
    }
}

enum class DragStatus{
    Down, Bar, Up
}