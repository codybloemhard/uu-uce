package com.uu_uce.mapOverlay

import android.content.Context
import com.uu_uce.GeoMap
import com.uu_uce.shapefiles.p3
import com.uu_uce.shapefiles.p3NaN
import kotlinx.android.synthetic.main.activity_geo_map.*

class ScaleWidget(private val minLength: Float){
    private val maxLength = minLength * 2

    private var viewport = Pair(p3NaN,p3NaN)

    fun update(viewport: Pair<p3,p3>, mapWidth: Int){
        var width = viewport.second.first - viewport.first.first



        this.viewport = viewport
    }

    fun showScale(lineProgram: Int, geomap: GeoMap){
        geomap.scale_text.text = (viewport.second.first - viewport.first.first).toString() + "m"
    }
}