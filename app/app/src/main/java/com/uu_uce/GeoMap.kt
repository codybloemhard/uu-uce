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

        var dir = File(filesDir, "mydir")
        dir.mkdir()
        var path = File(dir, "bt25mv10sh0f6422al1r020.shp")
        var test = SHP_File(null,  path)
        test.read()
        var smap = ShapeMap()
        smap.addLayer(LayerType.Height, test)
        var lol = 0
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
