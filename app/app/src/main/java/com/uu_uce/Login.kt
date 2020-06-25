package com.uu_uce

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.uu_uce.services.login
import kotlinx.android.synthetic.main.activity_login.*
import java.math.BigInteger
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * An activity in which the user can log into an acoount or choose to enter the app in offline mode.
 * @property[sharedPref] the shared preferences where the settings are stored.
 * @constructor a Login activity.
 */
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
        setContentView(R.layout.activity_login)

        //TODO: Get this information from server
        val orgToIp = mutableMapOf(Pair("Utrecht University", "http://131.211.31.176:8080"))
        val orgList = orgToIp.map{ (k, _) -> k }


        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, orgList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        orgselector.adapter = adapter

        signin_button.setOnClickListener {
            // Find Ip to send request to
            val ip = orgToIp[orgselector.selectedItem.toString()]
            if(ip != null){
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

                    login(username, password, ip, this){ response ->
                        when (response) {
                            HttpURLConnection.HTTP_OK -> {
                                with(sharedPref.edit()) {
                                    putString("com.uu_uce.USERNAME", username)
                                    putString("com.uu_uce.PASSWORD", password)
                                    putString("com.uu_uce.SERVER_IP", ip)
                                    putString("com.uu_uce.ORGNAME", orgselector.selectedItem.toString())
                                    apply()
                                }
                                this.runOnUiThread{
                                    Toast.makeText(this, getString(R.string.login_successfullogin), Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, GeoMap::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            HttpURLConnection.HTTP_NOT_FOUND -> {
                                this.runOnUiThread{
                                    Toast.makeText(this, getString(R.string.login_wrongcredentials_message), Toast.LENGTH_SHORT).show()
                                    username_field.text.clear()
                                    username_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
                                    password_field.text.clear()
                                    password_field.setHintTextColor(ResourcesCompat.getColor(resources, R.color.FusionRed, null))
                                }
                            }
                            HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                                this.runOnUiThread {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.login_serverdown),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        offline_button.setOnClickListener {
            Toast.makeText(this, getString(R.string.login_offline), Toast.LENGTH_LONG).show()
            with(sharedPref.edit()) {
                putString("com.uu_uce.USERNAME", "")
                putString("com.uu_uce.PASSWORD", "")
                putString("com.uu_uce.SERVER_IP", "")
                putString("com.uu_uce.ORGNAME", "")
                apply()
            }

            //TODO: if login becomes necessary remove this.
            val intent = Intent(this, GeoMap::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    /**
     * Calculates the SHA256 hash of a string.
     * @param[clearText] the string to be hashed.
     * @return the hash of the string.
     */
    private fun getHash(clearText: String): ByteArray {
        var digest: MessageDigest? = null
        try {
            digest = MessageDigest.getInstance("SHA-256")
        } catch (e1: NoSuchAlgorithmException) {
            e1.printStackTrace()
        }
        digest!!.reset()
        return digest.digest(clearText.toByteArray())
    }

    /**
     * Converts a ByteArray to a hexadecimal value in string format.
     * @param[data] a ByteArray to be converted to hexadecimal.
     * @return a hexadecimal value in string format.
     */
    private fun bin2hex(data: ByteArray): String {
        return java.lang.String.format("%0" + data.size * 2 + "X", BigInteger(1, data))
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

