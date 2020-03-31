package com.uu_uce

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.database.PinViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.LocationServices
import com.uu_uce.services.checkPermissions
import com.uu_uce.services.getPermissions
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.views.DragStatus
import kotlinx.android.synthetic.main.activity_geo_map.*
import java.io.File

class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0
    private val permissionsNeeded = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(checkPermissions(this, permissionsNeeded).count() > 0){
            getPermissions(this, permissionsNeeded, 1)
        }
        else{
            start()
        }
    }

    private fun start(){
        setContentView(R.layout.activity_geo_map)

        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        this.customMap.setViewModel(pinViewModel)
        this.customMap.setLifeCycleOwner(this)
        this.customMap.setPins()

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

        customMap.tryStartLocServices(this)


        button.setOnClickListener{customMap.zoomToDevice()}
        dragButton.clickAction = {menu.dragButtonTap()}
        dragButton.dragAction = {dx, dy -> menu.drag(dx,dy) }
        dragButton.dragEndAction = {dx, dy -> menu.snap(dx, dy)}

        menu.post {
            initMenu()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if(menu.dragStatus != DragStatus.Down &&
            ev.action == MotionEvent.ACTION_DOWN &&
            !(ev.rawX > menu.x && ev.rawX < menu.x + menu.width && ev.rawY > menu.y && ev.rawY < menu.y + menu.height)){
            menu.down()
            return true
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if(menu.dragStatus != DragStatus.Down){
            menu.down()
            return
        }
        super.onBackPressed()
    }

    override fun onResume() {
        if(checkPermissions(this, LocationServices.permissionsNeeded + permissionsNeeded).count() == 0){
            super.onResume()
            customMap.setPins()
            customMap.redrawMap()
        }

        super.onResume()
    }

    private fun initMenu(){
        menu.setScreenHeight(screenDim.y - statusBarHeight, dragButton.height, toggle_layer_scroll.height, lower_menu_layout.height)
    }

    // Respond to permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Logger.log(LogType.Info,"GeoMap", "Permissions granted")
                    start()
                }
                else{
                    Logger.log(LogType.Info,"GeoMap", "Permissions were not granted, asking again")
                    getPermissions(this, permissionsNeeded, 1)
                }
            }
            2 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Logger.log(LogType.Info,"CustomMap", "Permissions granted")
                    customMap.drawLocation = true
                    customMap.startLocServices()
                }
                else{
                    Logger.log(LogType.Info,"CustomMap", "Permissions were not granted")
                    customMap.drawLocation = false
                }
            }
        }
    }
}
