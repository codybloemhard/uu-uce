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
import com.uu_uce.services.LoginResult
import com.uu_uce.services.login
import kotlinx.android.synthetic.main.activity_login_screen.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class Login : AppCompatActivity() {

    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get preferences
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // Get desired theme
        if(sharedPref.getBoolean("com.uu_uce.DARKMODE", false)) setTheme(R.style.DarkTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login_screen)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }

        signin_button.setOnClickListener {
            // Get credentials
            val username = username_field.text.toString()
            val password = bin2hex(getHash(password_field.text.toString()))

            // Try to log in
            when (login(username, password)) {
                LoginResult.SUCCESS -> {
                    with(sharedPref.edit()) {
                        putString("com.uu_uce.USERNAME", username)
                        putString("com.uu_uce.PASSWORD", password)
                        apply()
                    }
                    val intent = Intent(this, GeoMap::class.java)
                    startActivity(intent)
                }
                LoginResult.NO_CREDENTIALS -> {
                    Toast.makeText(this, getString(R.string.login_nocredentials_message), Toast.LENGTH_SHORT).show()
                    username_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
                    password_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
                }
                LoginResult.NO_USERNAME -> {
                    Toast.makeText(this, getString(R.string.login_nousername_message), Toast.LENGTH_SHORT).show()
                    username_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
                }
                LoginResult.NO_PASSWORD -> {
                    Toast.makeText(this, getString(R.string.login_nopassword_message), Toast.LENGTH_SHORT).show()
                    password_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
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
