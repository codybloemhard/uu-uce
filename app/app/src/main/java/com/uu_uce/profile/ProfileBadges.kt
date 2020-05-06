package com.uu_uce.profile

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R

class ProfileBadges : Fragment() {
    private lateinit var fragmentActivity : Activity
    private lateinit var recyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentActivity = requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile_fragment_badges, container, false)//.also { view ->
            /*recyclerView = view.findViewById(R.id.badge_recyclerview)
            recyclerView.layoutManager = GridLayoutManager(fragmentActivity, 3)*/
        //}
    }
}
