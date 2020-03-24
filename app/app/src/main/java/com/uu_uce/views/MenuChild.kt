package com.uu_uce.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.Image
import android.view.MotionEvent

abstract class MenuChild(
    var minx: Float,
    var miny: Float,
    var maxx: Float,
    var maxy: Float){

    abstract fun onDraw(canvas: Canvas)

    open fun onTouchEvent(event: MotionEvent) : Boolean {
        return event.x > minx && event.x < maxx &&
                event.y > miny && event.y < maxy
    }
}

open class MenuButton(
    minx: Float,
    miny: Float,
    maxx: Float,
    maxy: Float,
    var action: (MenuButton) -> Unit,
    private var images: List<Drawable>,
    private var menu: Menu
): MenuChild(minx, miny, maxx, maxy){
    private var index = 0

    constructor(minx: Float, miny: Float, maxx: Float, maxy: Float, action: (MenuButton) -> Unit, image: Drawable, menu: Menu) : this(minx,miny,maxx,maxy,action,listOf(image), menu)

    override fun onDraw(canvas: Canvas) {
        val image = images[index]
        image.setBounds(minx.toInt(),miny.toInt(),maxx.toInt(),maxy.toInt())
        image.draw(canvas)
    }

    fun changeImage(){
        index = (index + 1)%images.size
        menu.invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(super.onTouchEvent(event)){
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    action(this)
                    return true
                }
            }
        }
        return false
    }
}