package com.uu_uce.fieldbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.R

class FieldbookPinmapFragment : Fragment() {

    private lateinit var viewModel: FieldbookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fieldbook_fragment_pinmap, container, false)
    }

    companion object {
        fun newInstance() =
            FieldbookPinmapFragment()
    }
}
