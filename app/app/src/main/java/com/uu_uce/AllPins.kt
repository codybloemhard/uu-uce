package com.uu_uce

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinListAdapter
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.activity_all_pins.*


class AllPins : AppCompatActivity() {
    private lateinit var recyclerView   : RecyclerView
    private lateinit var viewManager    : RecyclerView.LayoutManager
    private lateinit var pinViewModel   : PinViewModel
    private lateinit var sharedPref     : SharedPreferences
    private lateinit var viewAdapter    : PinListAdapter
    private var sortmode                : Int = 0

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
        setContentView(R.layout.activity_all_pins)

        createTopbar(this, getString(R.string.allpins_topbar_title))

        viewManager = LinearLayoutManager(this)

        viewAdapter = PinListAdapter(this)

        recyclerView = findViewById<RecyclerView>(R.id.allpins_recyclerview).apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        pinViewModel.allPinData.observe(this, Observer { pins ->
            pins?.let { viewAdapter.setPins(sortList(pins, sharedPref.getInt("com.uu_uce.SORTMODE", 0)), pinViewModel) }
        })

        val filterButton : FloatingActionButton = findViewById(R.id.fab)
        registerForContextMenu(filterButton)



        val searchBar = findViewById<EditText>(R.id.pins_searchbar)

        searchBar.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                val search = searchBar.text.toString()
                searchPins(search)
                return@OnKeyListener true
            }
            false
        })

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }
    }

    override fun onBackPressed() {
        if(viewAdapter.activePopup != null) {
            viewAdapter.activePopup?.dismiss()
            return
        }
        else if(pins_searchbar.text.toString().count() > 0){
            pins_searchbar.text.clear()
            searchPins("")
            return
        }
        super.onBackPressed()
    }

    fun openDialog(view : View) {
        val filterOptions : Array<String> = arrayOf(
            getString(R.string.allpins_sorting_title_az),
            getString(R.string.allpins_sorting_title_za),
            getString(R.string.allpins_sorting_difficulty_easyhard),
            getString(R.string.allpins_sorting_difficulty_hardeasy),
            getString(R.string.allpins_sorting_type_az),
            getString(R.string.allpins_sorting_type_za))

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.allpins_sort_popup_title))
            .setSingleChoiceItems(filterOptions, sharedPref.getInt("com.uu_uce.SORTMODE", 0)) { dialog, which ->
                sortmode = which
                dialog.dismiss()
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.SORTMODE", sortmode)
                    apply()
                }
                sortPins()
            }.show()
    }

    private fun sortPins(){
        viewAdapter = PinListAdapter(this)
        recyclerView.adapter = viewAdapter
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        pinViewModel.allPinData.observe(this, Observer { pins ->
            pins?.let { viewAdapter.setPins(sortList(it, sharedPref.getInt("com.uu_uce.SORTMODE", 0)), pinViewModel) }
        })
    }

    private fun sortList(pins : List<PinData>, sortmode: Int) : List<PinData> {
        return when(sortmode) {
            0 -> pins.sortedWith(compareBy { it.title })
            1 -> pins.sortedWith(compareByDescending { it.title })
            2 -> pins.sortedWith(compareBy { it.difficulty })
            3 -> pins.sortedWith(compareByDescending { it.difficulty })
            4 -> pins.sortedWith(compareBy { it.type })
            5 -> pins.sortedWith(compareByDescending { it.type })
            else -> {
                pins
            }
        }
    }

    private fun searchPins(search : String){
        pinViewModel.searchPins(search){ pins ->
            pins?.let {
                viewAdapter.setPins(sortList(pins, sharedPref.getInt("com.uu_uce.SORTMODE", 0)), pinViewModel)
            }
            hideKeyboard(this)
        }
    }

    private fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}