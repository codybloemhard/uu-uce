package com.uu_uce.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * a simple draggable button that's used for dragging the menu up and down
 */
class DragButton: FrameLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    //actions to be performed at the respective events
    var clickAction: () -> Unit = {}
    var dragAction: (dx: Float, dy: Float) -> Unit = { _, _ -> }
    var dragEndAction: (dx: Float, dy: Float) -> Unit = { _, _ -> }

    private var lastx = 0f
    private var secondlastx = 0f
    private var lasty = 0f
    private var secondlasty = 0f
    private var clickStart = 0f
    private var maximumClickDistance = 10

    /**
     * handle the touchevent by performing the correct actions
     * @return true if the event was handled, false otherwise
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //store where the drag action started
                clickStart = event.rawY
                secondlastx = lastx
                secondlasty = lasty
                lastx = event.rawX
                lasty = event.rawY
            }
            MotionEvent.ACTION_MOVE ->{
                //store where the touch moved and perform dragAction
                val dx = event.rawX - lastx
                val dy = event.rawY - lasty
                dragAction(dx, dy)

                secondlastx = lastx
                secondlasty = lasty
                lastx = event.rawX
                lasty = event.rawY
            }
            MotionEvent.ACTION_UP ->{
                //perform a click if the button has been moved only very slightly
                if(abs(event.rawY - clickStart) < maximumClickDistance){
                    clickAction()
                }
                //otherwise, perform dragEndAction
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


