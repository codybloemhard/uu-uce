package com.uu_uce

import android.content.Intent
import android.os.Bundle
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.ShapeMap
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger
import com.uu_uce.ui.TouchParent
import diewald_shapeFile.files.shp.SHP_File
import diewald_shapeFile.shapeFile.ShapeFile
import java.io.File

class GeoMap : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        addChild(Flinger(this, ::action))
    }

    private fun action(dir: FlingDir, delta: Float){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        if(dir == FlingDir.VER) return
        if(delta > 0.0f)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        else
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
