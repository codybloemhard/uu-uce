package com.uu_uce.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.uu_uce.R
import com.uu_uce.ui.DoubleTapper
import com.uu_uce.ui.Scroller
import com.uu_uce.ui.ViewTouchParent
import com.uu_uce.ui.Zoomer

class Menu : ViewTouchParent {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var image : Drawable? = context.getDrawable(R.drawable.menu)

    init{
        //setup touch events
        addChild(Scroller(context, ::scroll))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.RED)
        image?.draw(canvas)
    }

    private fun scroll(dxpxf: Float, dypxf: Float){
        y -= dypxf
        Log.d("Menu", "$dypxf")
    }
}