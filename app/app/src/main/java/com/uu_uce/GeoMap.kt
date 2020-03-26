package com.uu_uce

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.database.PinViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.LocationServices
import com.uu_uce.services.getPermissions
import com.uu_uce.views.MenuButton
import kotlinx.android.synthetic.main.activity_geo_map.*

class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)

        getPermissions(this, LocationServices.permissionsNeeded + customMap.permissionsNeeded)

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
            val t = Paint()
            t.color = Color.GREEN
            val c1 = MenuButton((menu.width - menu.downY)/ 2, 0f, (menu.width + menu.downY)/ 2, menu.downY, { menu.open() }, p)
            val c2 = MenuButton(20f, menu.downY, 20+(menu.barY - menu.downY), menu.barY, { customMap.toggleLayer(0) }, p)
            val c3 = MenuButton(20+(menu.barY - menu.downY), menu.downY, 20+(2*(menu.barY - menu.downY)), menu.barY, { customMap.allPins() }, t)
            menu.addMenuChild(c1)
            menu.addMenuChild(c2)
            menu.addMenuChild(c3)
        }

    }

    override fun onResume() {
        super.onResume()
        this.customMap.updatePins()
    }

    private fun initMenu(){
        (Display::getSize)(windowManager.defaultDisplay, screenDim)
        menu.setScreenHeight(screenDim.y - statusBarHeight)
    }

    // Respond to permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Logger.log(LogType.Info,"GPS", "Permissions granted")
                customMap.startLocServices()
            }
            else
                Logger.log(LogType.Info,"GPS", "Permissions were not granted")
        }
    }
}
