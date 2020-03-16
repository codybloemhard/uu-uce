package com.uu_uce

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.allpins.PinListAdapter
import com.uu_uce.database.PinViewModel

class AllPins : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var pinViewModel: PinViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_pins)
        viewManager = LinearLayoutManager(this)
        val viewAdapter = PinListAdapter(this)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerview).apply {
            //setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        pinViewModel.allPinData.observe(this, Observer { pins ->
            pins?.let { viewAdapter.setPins(it) }
        })
    }

    override fun onBackPressed(){
        
        super.onBackPressed()
    }
}