package com.uu_uce

import android.content.Intent
import android.os.Bundle
import com.uu_uce.services.LocationServices
import com.uu_uce.services.getPermissions
import com.uu_uce.ui.TouchParent

class MainActivity : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, GeoMap::class.java)
        getPermissions(this, this, LocationServices.permissionsNeeded)
        startActivity(intent)
    }
}
