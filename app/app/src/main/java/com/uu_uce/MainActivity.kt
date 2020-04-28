package com.uu_uce

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import com.uu_uce.gestureDetection.TouchParent
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger

class MainActivity : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }

        val intent = Intent(this, GeoMap::class.java)
        startActivity(intent)
        Logger.setTypeEnabled(LogType.Continuous, true)
    }
}
