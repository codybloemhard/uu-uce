package com.uu_uce.fieldbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val allRouteLocations : LiveData<List<RoutePoint>> = TODO()

    init {

    }
}