package com.uu_uce.fieldbook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider

import com.uu_uce.R

/**
 * A simple [Fragment] subclass.
 * Use the [FieldbookPinmapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment FieldbookPinmapFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance() =
            FieldbookPinmapFragment()
    }
}
