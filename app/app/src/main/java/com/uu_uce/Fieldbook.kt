package com.uu_uce

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.fieldbook.FieldbookHomeFragment
import com.uu_uce.fieldbook.FieldbookPinmapFragment
import com.uu_uce.fieldbook.FieldbookRouteFragment
import com.uu_uce.pins.ContentBlockInterface
import com.uu_uce.ui.createTopbar


class Fieldbook : AppCompatActivity() {

    lateinit var text : EditText
    private lateinit var sharedPref : SharedPreferences

    companion object;

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
        super.onCreate(null)
        setContentView(R.layout.activity_fieldbook)

        // Initiate views and layout
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.fieldbook_bottom_navigation)
        createTopbar(this, getString(R.string.fieldbook_topbar_title))

        openFragment(FieldbookHomeFragment.newInstance(window.decorView.rootView))

        // Set listeners
        bottomNavigation.setOnNavigationItemSelectedListener(
            BottomNavigationView.OnNavigationItemSelectedListener(
                fun(m): Boolean {
                    when (m.itemId) {
                        R.id.fieldbook_navigation_home -> {
                            openFragment(FieldbookHomeFragment.newInstance(window.decorView.rootView))
                            return true
                        }
                        R.id.fieldbook_navigation_pinmap -> {
                            val current = supportFragmentManager.findFragmentById(R.id.fieldbook_container) as Fragment
                            if(current !is FieldbookPinmapFragment){
                                openFragment(FieldbookPinmapFragment.newInstance())
                            }
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

    override fun onResumeFragments() {
        super.onResumeFragments()
        val current = supportFragmentManager.findFragmentById(R.id.fieldbook_container) as Fragment
        supportFragmentManager.beginTransaction().detach(current).attach(current).commit()
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

/**
 * Global function, that opens the popup specific to the Fieldbook Pins
 *
 * @param[activity] the currently opened activity
 * @param[rootView] the currently opened view
 * @param[entry] the selected FieldbookEntry
 * @param[content] the content of the mentioned entry
 */
fun openFieldbookPopup (activity: Activity, rootView: View, entry: FieldbookEntry, content: List<ContentBlockInterface>) {
    val layoutInflater = activity.layoutInflater

    var popupWindow: PopupWindow? = null


    val viewGroup: ViewGroup
    var newViewGroup: ViewParent = if(rootView is ViewParent) rootView else rootView.parent
    while(true){
        if(newViewGroup is ViewGroup){
            viewGroup = newViewGroup
            break
        }
        newViewGroup = newViewGroup.parent
    }
    // Build an custom view (to be inflated on top of our current view & build it's popup window)
    val customView = layoutInflater.inflate(R.layout.pin_content_view, viewGroup, false)
    customView.setOnKeyListener { v, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0 && popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            true
        }
        else false
    }

    popupWindow = PopupWindow(
        customView,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    popupWindow.setBackgroundDrawable(ColorDrawable())
    popupWindow.isOutsideTouchable = true
    popupWindow.isFocusable = true

    // Get elements
    val layout: LinearLayout =
        customView.findViewById(R.id.scrollLayout)
    val btnClosePopupWindow =
        customView.findViewById<Button>(R.id.popup_window_close_button)
    val windowTitle =
        customView.findViewById<TextView>(R.id.popup_window_title)
    val editButton =
        customView.findViewById<Button>(R.id.popup_window_edit_button).apply {
            isVisible   = true
            isClickable = true
        }

    // Add the title for the popup window
    windowTitle.text = entry.title

    // Fill layout of popup
    for(i in 0 until content.count()) {
        content[i].apply {
            showContent(i, layout, rootView, null)
        }
    }

    // Open popup
    popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)

    // Set onClickListeners
    btnClosePopupWindow.setOnClickListener {
        popupWindow.dismiss()
    }

    editButton.setOnClickListener {
        popupWindow.dismiss()
        val intent = Intent(activity, FieldbookEditor::class.java)
        intent.putExtra("fieldbook_index",entry.id)
        ContextCompat.startActivity(activity, intent, null)
    }
}
