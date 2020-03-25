package com.uu_uce

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.allpins.PinListAdapter
import com.uu_uce.database.PinData
import com.uu_uce.database.PinViewModel

class AllPins : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var pinViewModel: PinViewModel
    private var selectedOption: Int = 0
    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_pins)

        onCreateToolbar(this, "all pins")

        viewManager = LinearLayoutManager(this)

        val viewAdapter = PinListAdapter(this)

        recyclerView = findViewById<RecyclerView>(R.id.allpins_recyclerview).apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        pinViewModel.allPinData.observe(this, Observer { pins ->
            pins?.let { viewAdapter.setPins(sortList(it, sharedPref.getInt("selectedOption", 0))) }
        })

        val filterButton : FloatingActionButton = findViewById<FloatingActionButton>(R.id.fab)
        registerForContextMenu(filterButton)

        sharedPref = this.getPreferences(Context.MODE_PRIVATE)
    }

    fun openDialog(view: View) {
        val builder : AlertDialog.Builder = this.let {
            AlertDialog.Builder(it)
        }
        val filterOptions : Array<String> = arrayOf("Title a-z", "Title z-a", "Difficulty easy-hard", "Difficulty hard-easy", "Type a-z", "Type z-a")
        builder
            .setTitle("Filter by:")
            .setSingleChoiceItems(filterOptions, sharedPref.getInt("selectedOption", 0)) { dialog, which ->
                selectedOption = which
                dialog.dismiss()
                sortList(selectedOption)
                with(sharedPref.edit()) {
                    putInt("selectedOption", selectedOption)
                    apply()
                }
            }

        builder.show()
    }

    private fun sortList(category: Int){
        val viewAdapter = PinListAdapter(this)
        recyclerView.adapter = viewAdapter
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        pinViewModel.allPinData.observe(this, Observer { pins ->
            pins?.let {
                viewAdapter.setPins(sortList(it, category)) }
        })
    }

    private fun sortList(pins : List<PinData>, id: Int) : List<PinData> {
        return when(id) {
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

    // TODO: wasn't sure on where to put this...
    fun onCreateToolbar(activity : Activity, title: String)
    {
        activity.findViewById<TextView>(R.id.toolbar_title).text = title

        activity.findViewById<ImageButton>(R.id.toolbar_back_button).setOnClickListener{
            activity.finish()
        }
    }

}