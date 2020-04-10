package com.uu_uce

import android.Manifest
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uu_uce.fieldbook.*
import com.uu_uce.ui.createTopbar

class FieldBook : AppCompatActivity() {

    companion object {
        var permissionsNeeded = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        // initiate views and layout
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.fieldbook_bottom_navigation)
        createTopbar(this, "my fieldbook")

        // What are we doing? TODO

        openFragment(FieldbookHomeFragment.newInstance("",""))

        val fieldbookViewModel = ViewModelProvider(this).get(FieldbookViewModel::class.java)

        // set listeners

        bottomNavigation.setOnNavigationItemSelectedListener(
            BottomNavigationView.OnNavigationItemSelectedListener(
                fun(m): Boolean {
                    when(m.itemId) {
                        R.id.fieldbook_navigation_home -> {
                            openFragment(FieldbookHomeFragment.newInstance("",""))
                            return true
                        }
                        R.id.fieldbook_navigation_pinmap -> {
                            openFragment(FieldbookPinmapFragment.newInstance())
                            return true
                        }
                        R.id.fieldbook_navigation_route -> {
                            openFragment(FieldbookRouteFragment.newInstance("",""))
                            return true
                        }
                    }
                    return false
                }
            )
        )
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fieldbook_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
