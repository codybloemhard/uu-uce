package com.uu_uce.shapefiles

import android.util.Log
import kotlin.math.absoluteValue
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
    NONE, TRANS
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
    var minZoom = 0.0000000001

    private var lastWoff = 0.0
    private var lastHoff = 0.0

    private var animType: AnimType = AnimType.NONE
    private var animBegin = p3Zero
    private var animTarget = p3Zero
    private var animDuration = 0.0
    private var animStartT = 0.0
    private var animT = 0.0

    fun getViewport(waspect: Double): Pair<p3,p3>{
        val w = viewMax.first - viewMin.first
        val h = viewMax.second - viewMin.second
        val woff = w * waspect / 2.0 * zoom
        val hoff = h / 2.0 * zoom
        lastWoff = woff
        lastHoff = hoff
        val nmin = Triple(x - woff, y - hoff, Double.MIN_VALUE)
        val nmax = Triple(x + woff, y + hoff, Double.MAX_VALUE)
        return Pair(nmin, nmax)
    }

    private fun isBusy(): Boolean{
        return animType != AnimType.NONE
    }

    fun setPosCenter(){
        if(isBusy()) return
        x = mx
        y = my
    }

    fun setPos(newX: Double, newY: Double){
        if(isBusy()) return
        val minx = viewMin.first + lastWoff
        val maxx = viewMax.first - lastWoff
        val miny = viewMin.second + lastHoff
        val maxy = viewMax.second - lastHoff
        if(minx >= maxx || miny >= maxy){
            setPosCenter()
        }else{
            x = newX.coerceIn(minx, maxx)
            y = newY.coerceIn(miny, maxy)
        }
    }

    fun moveView(dx: Double, dy: Double){
        if(isBusy()) return
        setPos(x + (dx * lastWoff), y + (dy * lastHoff))
    }

    fun getZoom(): Double{
        return zoom
    }

    fun setZoom(newZoom: Double){
        if(isBusy()) return
        zoom = newZoom.coerceIn(minZoom, maxZoom)
    }

    fun zoomIn(factor: Double){
        if(isBusy()) return
        zoom *= factor
        zoom = zoom.coerceIn(minZoom, maxZoom)
    }

    fun zoomOutMax(duration: Double){
        if(isBusy()) return
        startAnimation(Triple(mx,my,maxZoom), duration)
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

    fun update(){
        if(!isBusy()) return
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