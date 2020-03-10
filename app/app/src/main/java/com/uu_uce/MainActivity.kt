package com.uu_uce

import android.content.Intent
import android.os.Bundle
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.ui.TouchParent

class MainActivity : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, GeoMap::class.java)
        startActivity(intent)
        Logger.setTypeEnabled(LogType.Continuous, true)
    }
}
