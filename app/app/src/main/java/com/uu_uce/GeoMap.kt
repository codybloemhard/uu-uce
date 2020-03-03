package com.uu_uce

import android.content.Intent
import android.os.Bundle
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger
import com.uu_uce.ui.TouchParent
import com.uu_uce.ui.Zoomer
import kotlinx.android.synthetic.main.activity_geo_map.*

class GeoMap : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        addChild(Zoomer(this, ::action))
    }

    private fun action(delta: Float){
        this.customMap.zoomMap(delta.toDouble())
    }
}
