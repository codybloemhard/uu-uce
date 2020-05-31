package com.uu_uce.fieldbook

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.uu_uce.R
import com.uu_uce.defaultPinSize
import com.uu_uce.misc.ListenableBoolean
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.needsReload
import com.uu_uce.services.LOCATION_REQUEST
import com.uu_uce.shapefiles.HeightLineReader
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.PolygonReader
import kotlinx.android.synthetic.main.activity_geo_map.*
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [FieldbookPinmapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FieldbookPinmapFragment : Fragment() {

    companion object {
        fun newInstance() =
            FieldbookPinmapFragment()
    }

    private lateinit var viewModel  : FieldbookViewModel
    private lateinit var frActivity : Activity
    private lateinit var frContext  : Context
    private lateinit var window     : Window

    private var statusBarHeight = 0
    private var resourceId = 0
    private var started = false

    private lateinit var sharedPref : SharedPreferences

    // Popup for showing download progress
    private var popupWindow: PopupWindow? = null
    private lateinit var progressBar : ProgressBar
    private var downloadResult = false

    // TODO: Remove temporary hardcoded map information
    private val mapsName = "maps.zip"
    private lateinit var maps : List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        frActivity = requireActivity()
        frContext  = requireContext()
        window     = frActivity.window

        maps = listOf(frActivity.getExternalFilesDir(null)?.path + File.separator + mapsName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fieldbook_fragment_pinmap, container, false)
    }

    override fun onStart() {
        super.onStart()

        start()
    }


    private fun start(){
        // Get preferences
        sharedPref = PreferenceManager.getDefaultSharedPreferences(frContext)

        // Set settings
        this.customMap.pinSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)

        this.customMap.setActivity(frActivity)

        // TODO: Remove when releasing
        with(sharedPref.edit()) {
            putInt("com.uu_uce.USER_POINTS", 0)
            apply()
        }

        // Start database and get pins from database
        this.customMap.setFieldbookViewModel(viewModel)
        this.customMap.setLifeCycleOwner(this)
        viewModel.allFieldbookEntries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            this.customMap.setFieldbook(it)
        })

        // Get statusbar height
        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        //add layers to map
        loadMap()

        needsReload.setListener(object : ListenableBoolean.ChangeListener {
            override fun onChange() {
                if(needsReload.getValue()){
                    loadMap()
                }
            }
        })

        started = true
    }

    // Respond to permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
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

    private fun loadMap(){
        val mydir = File(frContext.getExternalFilesDir(null)?.path + "/Maps/")
        try {
            val heightlines = File(mydir, "Heightlines")
            customMap.addLayer(
                LayerType.Height,
                HeightLineReader(heightlines),
                toggle_layer_layout,
                true
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $heightlines")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }
        try {
            val polygons = File(mydir, "Polygons")
            customMap.addLayer(
                LayerType.Water,
                PolygonReader(polygons),
                toggle_layer_layout,
                false
            )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $mydir")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }

        //create camera based on layers
        customMap.initializeCamera()

        customMap.setCameraWAspect()
        needsReload.setValue(false)
        customMap.redrawMap()
    }

    private fun openProgressPopup(currentView: View){
        val layoutInflater = layoutInflater

        // Build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.progress_popup, geoMapLayout, false)

        popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        progressBar = customView.findViewById(R.id.progress_popup_progressBar)

        // Open popup
        popupWindow?.showAtLocation(currentView, Gravity.CENTER, 0, 0)
    }
}
