package com.uu_uce.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlin.math.abs

class DragButton: AppCompatImageView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var clickAction: () -> Unit = {}
    var dragAction: (dx: Float, dy: Float) -> Unit = {_,_->  }
    var dragEndAction: (dx: Float, dy: Float) -> Unit = {_,_ ->}

    private var lastx = 0f
    private var secondlastx = 0f
    private var lasty = 0f
    private var secondlasty = 0f
    private var clickStart = 0f
    private var maximumClickDistance = 10
    private var shouldClick = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_DOWN ->{
                clickStart = event.rawY
                secondlastx = lastx
                secondlasty = lasty
                lastx = event.rawX
                lasty = event.rawY
                shouldClick = true
            }
            MotionEvent.ACTION_MOVE ->{
                val dx = event.rawX - lastx
                val dy = event.rawY - lasty
                dragAction(dx, dy)

                secondlastx = lastx
                secondlasty = lasty
                lastx = event.rawX
                lasty = event.rawY
            }
            MotionEvent.ACTION_UP ->{
                if(abs(event.rawY - clickStart) < maximumClickDistance && shouldClick){
                    clickAction()
                }
                else{
                    val dx = event.rawX - secondlastx
                    val dy = event.rawY - secondlasty
                    dragEndAction(dx,dy)
                }
            }
        }
        return true
    }
}