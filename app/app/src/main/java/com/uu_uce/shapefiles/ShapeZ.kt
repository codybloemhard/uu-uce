package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint

class ShapeZ(t: ShapeType, p: List<p2>, bmi: p3, bma: p3) {
    private var type: ShapeType = t
    private var points: List<p2> = p
    var bMin = bmi
        private set
    var bMax = bma
        private set

    fun draw(
        canvas: Canvas,
        paint: Paint,
        viewport : Pair<p2, p2>,
        width: Int,
        height: Int
    ) {
        if (points.size < 2) return
        val drawPoints = FloatArray(4 * (points.size - 1))

        var lineIndex = 0
        for (i in 0..points.size - 2) {
            drawPoints[lineIndex++] =
                ((points[i].first - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i].second - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat()
            drawPoints[lineIndex++] =
                ((points[i + 1].first - viewport.first.first) / (viewport.second.first - viewport.first.first) * width).toFloat()
            drawPoints[lineIndex++] =
                (height - (points[i + 1].second - viewport.first.second) / (viewport.second.second - viewport.first.second) * height).toFloat()
        }

        canvas.drawLines(drawPoints, paint)
    }

    fun meanZ(): Int{
        return ((bMin.third + bMax.third) / 2).toInt()
    }
}