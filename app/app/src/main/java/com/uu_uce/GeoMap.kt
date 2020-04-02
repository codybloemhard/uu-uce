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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.database.PinViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.*
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.views.DragStatus
import kotlinx.android.synthetic.main.activity_geo_map.*
import java.io.File

class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private val permissionsNeeded = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        start()
        /* This may be needed if the maps are read out of external memory
        if(checkPermissions(this, permissionsNeeded).count() > 0){
            getPermissions(this, permissionsNeeded, EXTERNAL_FILES_REQUEST)
        }
        else{
            start()
        }*/
    }

    private fun start(){
        setContentView(R.layout.activity_geo_map)

        // Start database and get pins from database
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

        // Initialize menu
        val btn = ImageButton(this, null, android.R.attr.buttonBarButtonStyle)
        btn.setImageResource(R.drawable.logotp)
        btn.setBackgroundColor(Color.BLUE)
        btn.setOnClickListener{customMap.startAllPins()}
        btn.layoutParams = ViewGroup.LayoutParams(size, size)
        lower_menu_layout.addView(btn)

        dragButton.clickAction      = {menu.dragButtonTap()}
        dragButton.dragAction       = {dx, dy -> menu.drag(dx,dy)}
        dragButton.dragEndAction    = {dx, dy -> menu.snap(dx, dy)}

        menu.post {
            initMenu()
        }

        // Read map
        val dir = File(filesDir, "mydir")
        customMap.addLayer(LayerType.Water, dir, toggle_layer_layout, size)
        customMap.initializeCamera()

        customMap.tryStartLocServices(this)

        // Set center on location button functionality
        center_button.setOnClickListener{
            if(customMap.locationAvailable){
                customMap.zoomToDevice()
            }
            else{
                Toast.makeText(this, "Location not avaiable", Toast.LENGTH_LONG).show()
                getPermissions(this, LocationServices.permissionsNeeded, LOCATION_REQUEST)
            }
        }

        started = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Move the menu down when the map is tapped
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

        customMap.activePopup?.dismiss()
    }

    override fun onResume() {
        if(started){
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
            EXTERNAL_FILES_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Logger.log(LogType.Info,"GeoMap", "Permissions granted")
                    start()
                }
                else{
                    Logger.log(LogType.Info,"GeoMap", "Permissions were not granted, asking again")
                    getPermissions(this, permissionsNeeded, EXTERNAL_FILES_REQUEST)
                }
            }
            LOCATION_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Logger.log(LogType.Info,"GeoMap", "Permissions granted")
                    customMap.locationAvailable = true
                    customMap.startLocServices()
                }
                else{
                    Logger.log(LogType.Info,"GeoMap", "Permissions were not granted")
                    customMap.locationAvailable = false
                }
            }
        }
    }
}
