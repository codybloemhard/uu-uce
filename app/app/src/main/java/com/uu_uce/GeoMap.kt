package com.uu_uce

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.database.PinViewModel
import com.uu_uce.shapefiles.p2
import com.uu_uce.ui.*
import com.uu_uce.views.MenuButton
import kotlinx.android.synthetic.main.activity_geo_map.*
import kotlinx.android.synthetic.main.activity_geo_map.view.*

class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        this.customMap.setViewModel(pinViewModel)
        this.customMap.setLifeCycleOwner(this)
        this.customMap.updatePins()

        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        button.setOnClickListener{customMap.zoomToDevice()}

        initMenu()
        menu.post {
            val p = Paint()
            p.color = Color.RED
            var c1 = MenuButton((menu.width - menu.downY)/ 2, 0f, (menu.width + menu.downY)/ 2, menu.downY, { menu.open() }, p)
            var c2 = MenuButton(20f, menu.downY, 20+(menu.barY - menu.downY), menu.barY, { customMap.toggleLayer(0) }, p)
            menu.addMenuChild(c1)
            menu.addMenuChild(c2)
        }
    }

    private fun initMenu(){
        (Display::getSize)(windowManager.defaultDisplay, screenDim)
        menu.setScreenHeight(screenDim.y - statusBarHeight)
    }

    private fun onSingleTap(tapLocation : p2){
        customMap.tapPin(tapLocation)
    }
}
