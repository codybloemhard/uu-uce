package com.uu_uce

import android.Manifest
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uu_uce.fieldbook.FieldbookHomeFragment
import com.uu_uce.fieldbook.FieldbookPinmapFragment
import com.uu_uce.fieldbook.FieldbookRouteFragment
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.ui.createTopbar


class Fieldbook : AppCompatActivity() {

    lateinit var text: EditText

    companion object {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        // initiate views and layout
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.fieldbook_bottom_navigation)
        createTopbar(this, "my fieldbook")

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
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fieldbook_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
