package com.uu_uce

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceManager
import com.uu_uce.gestureDetection.TouchParent
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.login

//currently used only to switch to the GeoMap activity
class MainActivity : TouchParent() {

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val darkMode = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        // Set desired theme
        if(darkMode) setTheme(R.style.DarkTheme)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !darkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else if(!darkMode){
            window.statusBarColor = Color.BLACK// set status background white
        }

        if (checkLogin()) {
            val intent = Intent(this, GeoMap::class.java)
            startActivity(intent)
        }
        else{
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            Logger.setTypeEnabled(LogType.Continuous, true)
        }
    }

    @Suppress("UnnecessaryVariable") // TODO: remove when result which is a webtoken
    private fun checkLogin() : Boolean{
        val result = login(
            sharedPref.getString("com.uu_uce.USERNAME", "").toString(),
            sharedPref.getString("com.uu_uce.PASSWORD", "").toString()
        )
        return result
    }
}
