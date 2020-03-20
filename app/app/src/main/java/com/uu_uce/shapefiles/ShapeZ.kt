package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import diewald_shapeFile.files.shp.shapeTypes.ShpPoint
import diewald_shapeFile.files.shp.shapeTypes.ShpPolyLine
import diewald_shapeFile.files.shp.shapeTypes.ShpPolygon
import diewald_shapeFile.files.shp.shapeTypes.ShpShape

class ShapeZ {
    private var type: ShapeType
    var points: List<p2>
    var bMin = p3Zero
        private set
    var bMax = p3Zero
        private set

    constructor(t:ShapeType, p: List<p2>, bmi: p3, bma: p3){
        type = t
        points = p
        bMin = bmi
        bMax = bma
    }

    constructor(zoomPercentage: Double, baseShape: ShapeZ, bmi: p3, bma: p3){
        type = baseShape.type
        val mutablePoints = mutableListOf(baseShape.points.first())
        for(i in 1 until (baseShape.points.size * zoomPercentage).toInt()){
            mutablePoints.add(baseShape.points[(i/zoomPercentage).toInt()])
        }
        mutablePoints.add(baseShape.points.last())
        points = mutablePoints

        bMin = bmi
        bMax = bma
    }

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