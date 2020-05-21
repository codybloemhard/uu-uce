package com.uu_uce

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.uu_uce.services.login
import kotlinx.android.synthetic.main.activity_login_screen.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class Login : AppCompatActivity() {

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
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

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)

        signin_button.setOnClickListener {
            // Get credentials
            val username = username_field.text.toString()
            var password = password_field.text.toString()
            if(username.isEmpty() && password.isEmpty()){
                Toast.makeText(this, getString(R.string.login_nocredentials_message), Toast.LENGTH_SHORT).show()
                username_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
                password_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
            }
            else if(username.isEmpty()){
                Toast.makeText(this, getString(R.string.login_nousername_message), Toast.LENGTH_SHORT).show()
                username_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
            }
            else if(password.isEmpty()){
                Toast.makeText(this, getString(R.string.login_nopassword_message), Toast.LENGTH_SHORT).show()
                password_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
            }
            else{
                // Hash password for sending and storing
                password = bin2hex(getHash(password))

                val loginResult = login(username, password)
                if(loginResult){
                    with(sharedPref.edit()) {
                        putString("com.uu_uce.USERNAME", username)
                        putString("com.uu_uce.PASSWORD", password)
                        apply()
                    }
                    val intent = Intent(this, GeoMap::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun getHash(password: String): ByteArray {
        var digest: MessageDigest? = null
        try {
            digest = MessageDigest.getInstance("SHA-256")
        } catch (e1: NoSuchAlgorithmException) {
            e1.printStackTrace()
        }
        digest!!.reset()
        return digest.digest(password.toByteArray())
    }

    private fun bin2hex(data: ByteArray): String {
        return java.lang.String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
    }
}
