package com.uu_uce

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.database.PinViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.LocationServices.Companion.permissionsNeeded
import com.uu_uce.services.checkPermissions
import com.uu_uce.services.getPermissions
import com.uu_uce.shapefiles.LayerType
import kotlinx.android.synthetic.main.activity_geo_map.*
import java.io.File

class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)

        getPermissions(this, this, permissionsNeeded + customMap.permissionsNeeded)

        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        this.customMap.setViewModel(pinViewModel)
        this.customMap.setLifeCycleOwner(this)
        this.customMap.updatePins()

        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        (Display::getSize)(windowManager.defaultDisplay, screenDim)
        val longest = maxOf(screenDim.x, screenDim.y)
        val size = (longest*menu.buttonPercent).toInt()

        val btn = ImageButton(this, null, android.R.attr.buttonBarButtonStyle)
        btn.setImageResource(R.drawable.logotp)
        btn.setBackgroundColor(Color.BLUE)
        btn.setOnClickListener{customMap.allPins()}
        btn.layoutParams = ViewGroup.LayoutParams(size, size)
        lower_menu_layout.addView(btn)

        val dir = File(filesDir, "mydir")
        customMap.addLayer(LayerType.Water, dir, toggle_layer_layout, size)

        val missingPermissions = checkPermissions(this,customMap.permissionsNeeded + permissionsNeeded)
        if(missingPermissions.count() == 0){
            customMap.startLocServices()
        }

        button.setOnClickListener{customMap.zoomToDevice()}
        dragButton.clickAction = {menu.dragButtonTap()}
        dragButton.dragAction = {dx, dy -> menu.drag(dx,dy) }
        dragButton.dragEndAction = {dx, dy -> menu.snap(dx, dy)}

        menu.post {
            initMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        this.customMap.updatePins()
    }

    private fun initMenu(){
        menu.setScreenHeight(screenDim.y - statusBarHeight, dragButton.height, toggle_layer_scroll.height)
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
