package com.uu_uce

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uu_uce.fieldbook.FieldbookHomeFragment
import com.uu_uce.fieldbook.FieldbookPinmapFragment
import com.uu_uce.fieldbook.FieldbookRouteFragment
import com.uu_uce.ui.createTopbar


class Fieldbook : AppCompatActivity() {

    lateinit var text: EditText
    private lateinit var sharedPref     : SharedPreferences


    companion object;

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // Get desired theme
        if(sharedPref.getBoolean("com.uu_uce.DARKMODE", false)) setTheme(R.style.DarkTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fieldbook)

        // initiate views and layout
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.fieldbook_bottom_navigation)
        createTopbar(this, "My Fieldbook")

        openFragment(FieldbookHomeFragment.newInstance())

        // set listeners
        bottomNavigation.setOnNavigationItemSelectedListener(
            BottomNavigationView.OnNavigationItemSelectedListener(
                fun(m): Boolean {
                    when (m.itemId) {
                        R.id.fieldbook_navigation_home -> {
                            openFragment(FieldbookHomeFragment.newInstance())
                            return true
                        }
                        R.id.fieldbook_navigation_pinmap -> {
                            openFragment(FieldbookPinmapFragment.newInstance())
                            return true
                        }
                        R.id.fieldbook_navigation_route -> {
                            openFragment(FieldbookRouteFragment.newInstance("", ""))
                            return true
                        }
                    }
                    return false
                }
            )
        )

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fieldbook_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onBackPressed() {
        finish()
    }
}
