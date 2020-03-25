package com.uu_uce.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import androidx.core.view.updateLayoutParams

class Menu : View {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var dragStatus = DragStatus.Down

    private val menuChilds : MutableList<MenuChild> = mutableListOf()

    private val downPercent = 0.05f
    private val barPercent = 0.2f
    private val upPercent = 1f
    var downY = 0f
    var barY = 0f
    private var upY = 0f
    private var screenSiz = 0

    init{
        //make the menu start at the bottom of the screen
        val listener =
            OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                y = oldTop.toFloat()
            }
        addOnLayoutChangeListener(listener)
        post{
            updateLayoutParams{height = screenSiz}
            y = screenSiz - downY
        }
    }

    fun addMenuChild(child: MenuChild){
        menuChilds.add(child)
    }

    fun setScreenHeight(h: Int){
        screenSiz = h
        downY = h * downPercent
        barY = h * barPercent
        upY = h * upPercent
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.argb(255,254,212,2))
        for(child in menuChilds)
            child.onDraw(canvas)
    }

    fun open(){
        when(dragStatus){
            DragStatus.Down ->{
                dragStatus = DragStatus.Bar
                animate().y(screenSiz - barY)
            }
            DragStatus.Bar ->{
                dragStatus = DragStatus.Down
                animate().y(screenSiz - downY)
            }
            DragStatus.Up ->{

            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        for(child in menuChilds){
            if(child.onTouchEvent(event))
                break
        }
        var x = event.x
        var y = event.y
        var rx = event.rawX
        var ry = event.rawY
        return true
    }
}

enum class DragStatus{
    Down, Bar, Up
}