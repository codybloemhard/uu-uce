package com.uu_uce.shapefiles

import kotlin.math.pow
import kotlin.math.sqrt

fun distXy(p: p3, q: p3): Double{
    return sqrt((p.first - q.first).pow(2) +
            (p.second - q.second).pow(2))
}

fun lerp(a: Double, b: Double, t: Double): Double{
    return a + (b - a) * t
}

enum class AnimType{
    NONE, TRANS, OUT
}

enum class UpdateResult{
    NOOP, REDRAW, ANIM
}

class Camera(
    private var x: Double,
    private var y: Double,
    private var zoom: Double,
    private val viewMin: p3,
    private val viewMax: p3){

    private val mx = (viewMin.first + viewMax.first) / 2.0
    private val my = (viewMin.second + viewMax.second) / 2.0
    private val maxDistXy = distXy(viewMin, viewMax)

    var maxZoom = 1.0
    private var minZoom = 0.0000000001

    private var lastWoff = 0.0
    private var lastHoff = 0.0
    private var changed = true

    private var animType: AnimType = AnimType.NONE
    private var animBegin = p3Zero
    private var animTarget = p3Zero
    private var animDuration = 0.0
    private var animStartT = 0.0
    private var animT = 0.0

    fun getViewport(wAspect: Double): Pair<p2,p2>{
        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val woff = w * wAspect / 2.0 * zoom
        val hoff = h / 2.0 * zoom
        lastWoff = woff
        lastHoff = hoff
        val nmin = p2(x - woff, y - hoff)
        val nmax = p2(x + woff, y + hoff)
        return Pair(nmin, nmax)
    }

    private fun isBusy(): Boolean{
        return animType != AnimType.NONE
    }

    fun needsInvalidate(): Boolean{
        return changed || isBusy()
    }

    private fun setXy(xx: Double, yy: Double){
        changed = changed || xx != x || yy != y
        x = xx
        y = yy
    }

    private fun setPosCenter(){
        if(isBusy()) return
        setXy(mx, my)
    }

    private fun setPos(newX: Double, newY: Double){
        if(isBusy()) return
        val minx = viewMin.first + lastWoff
        val maxx = viewMax.first - lastWoff
        val miny = viewMin.second + lastHoff
        val maxy = viewMax.second - lastHoff
        if(minx >= maxx || miny >= maxy)
            setPosCenter()
        else
            setXy(newX.coerceIn(minx, maxx),newY.coerceIn(miny, maxy))

    }

    fun moveView(dx: Double, dy: Double){
        if(isBusy()) return
        setPos(x + (dx * lastWoff), y + (dy * lastHoff))
    }

    fun getZoom(): Double{
        return zoom
    }

    private fun setZ(zz: Double){
        changed = changed || zoom != zz
        zoom = zz
    }

    fun setZoom(newZoom: Double){
        if(isBusy()) return
        setZ(newZoom.coerceIn(minZoom, maxZoom))
    }

    fun zoomIn(factor: Double){
        if(isBusy()) return
        val z = zoom * factor
        setZ(z.coerceIn(minZoom, maxZoom))
    }

    fun zoomOutMax(duration: Double){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = Triple(mx,my,maxZoom)
        animStartT = System.currentTimeMillis().toDouble()
        animDuration = duration
        animType = AnimType.OUT
        animT = 0.0
    }

    fun startAnimation(target: p3, durationMs: Double){
        if(isBusy()) return
        animBegin = Triple(x, y, zoom)
        animTarget = target
        animStartT = System.currentTimeMillis().toDouble()
        animDuration = durationMs
        animType = AnimType.TRANS
        animT = 0.0
    }

    private fun smooth(t: Double): Double{
        return -(t - 1.0).pow(2.0) + 1.0
    }

    //Updates and returns true if viewport has changed
    fun update(): UpdateResult{
        if(!changed && !isBusy()){
            return UpdateResult.NOOP
        }
        changed = false
        when(animType){
            AnimType.NONE -> {}
            AnimType.TRANS -> updateTrans()
            AnimType.OUT -> updateOut()
        }
        return if(isBusy())
            UpdateResult.ANIM
        else
            UpdateResult.REDRAW
    }

    private fun updateOut(){
        val ct = System.currentTimeMillis().toDouble()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0, 1.0)
        x = animBegin.first + (animTarget.first - animBegin.first) * t
        y = animBegin.second + (animTarget.second - animBegin.second) * t
        zoom = animBegin.third + (animTarget.third - animBegin.third) * t
        if(ct > animStartT + animDuration){
            animType = AnimType.NONE
            return
        }
    }

    private fun updateTrans(){
        val ct = System.currentTimeMillis().toDouble()
        val t = ((ct - animStartT) / animDuration).coerceIn(0.0, 1.0)
        val distFraction = distXy(animBegin, animTarget) / maxDistXy
        val zoomAvg = (animBegin.third + animTarget.third) / 2.0
        val midZoom = (maxZoom - zoomAvg)*distFraction + zoomAvg
        val zt = 1.0/3.0
        when {
            t < zt -> {
                zoom = lerp(animBegin.third, midZoom, smooth(t / zt))
            }
            t < 1-zt -> {
                val tt = (t - zt) / (1 - 2*zt)
                x = animBegin.first + (animTarget.first - animBegin.first) * tt
                y = animBegin.second + (animTarget.second - animBegin.second) * tt
            }
            else -> {
                x = animTarget.first
                y = animTarget.second
                zoom = lerp(animTarget.third,midZoom, 1 - smooth((t - (1 - zt)) / zt))
            }
        }

        if(ct > animStartT + animDuration){
            animType = AnimType.NONE
            return
        }
    }
}