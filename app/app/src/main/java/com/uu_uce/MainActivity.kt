package com.uu_uce

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.login
import java.net.HttpURLConnection

//currently used only to switch to the GeoMap activity
class MainActivity : AppCompatActivity() {

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

        val username = sharedPref.getString("com.uu_uce.USERNAME", "").toString()
        val password = sharedPref.getString("com.uu_uce.PASSWORD", "").toString()
        val ip       = sharedPref.getString("com.uu_uce.SERVER_IP", "").toString()

        if(username.isNotEmpty() && password.isNotEmpty() && ip.isNotEmpty()){
            login(
                username,
                password,
                ip,
                this
            )
            { response ->
                when(response){
                    HttpURLConnection.HTTP_OK -> {
                        val intent = Intent(this, GeoMap::class.java)
                        startActivity(intent)
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        openLogin()
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        this.runOnUiThread{
                            Toast.makeText(this, getString(R.string.login_serverdown), Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> {
                        openLogin()
                    }
                }
            }
        }
        else{
            openLogin()
        }
    }

    private fun openLogin(){
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        Logger.setTypeEnabled(LogType.Continuous, true)
    }
}
