package com.uu_uce

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import kotlinx.android.synthetic.main.activity_login_screen.*

class LoginScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
            when (singIn(username_field.text.toString(), password_field.text.toString())) {
                LoginResult.SUCCESS -> {
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

    private fun singIn(username : String, password : String) : LoginResult {
        // TODO: Connect to server and send credentials
        if(username.count() < 1 && password.count() < 1) return LoginResult.NO_CREDENTIALS
        else if(username.count() < 1) return LoginResult.NO_USERNAME
        else if(password.count() < 1) return LoginResult.NO_PASSWORD
        return LoginResult.SUCCESS
    }

    enum class LoginResult{
        SUCCESS,
        NO_CREDENTIALS,
        NO_USERNAME,
        NO_PASSWORD;
    }
}