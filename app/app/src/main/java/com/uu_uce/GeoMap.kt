package com.uu_uce

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.database.PinViewModel
import com.uu_uce.ui.DoubleTapper
import com.uu_uce.ui.Scroller
import com.uu_uce.ui.TouchParent
import com.uu_uce.ui.Zoomer
import com.uu_uce.shapefiles.p2
import com.uu_uce.ui.*
import kotlinx.android.synthetic.main.activity_geo_map.*

class GeoMap : TouchParent() {
    public lateinit var pinViewModel: PinViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        addChild(Zoomer(this, ::onZoom))
        addChild(Scroller(this, ::onScroll))
        addChild(DoubleTapper(this, ::onDoubleTap))
        addChild(SingleTapper(this, ::onSingleTap))
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        this.customMap.setViewModel(pinViewModel)
        this.customMap.setLifeCycleOwner(this)
        this.customMap.updatePins()

        button2.setOnClickListener{customMap.zoomToDevice()}
    }

    private fun onZoom(delta: Float){
        this.customMap.zoomMap(delta.toDouble())
    }

    private fun onScroll(dx: Float, dy: Float){
        this.customMap.moveMap(dx.toDouble(), dy.toDouble())
    }

    private fun onDoubleTap(){
        this.customMap.zoomOutMax()
    }

    private fun onSingleTap(tapLocation : p2){
        customMap.tapPin(tapLocation)
    }
}
