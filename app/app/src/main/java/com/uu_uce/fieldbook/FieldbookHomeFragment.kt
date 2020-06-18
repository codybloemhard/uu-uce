package com.uu_uce.fieldbook

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.FieldbookEditor
import com.uu_uce.R

class FieldbookHomeFragment(view: View) : Fragment() {

    companion object {
        fun newInstance(view: View) =
            FieldbookHomeFragment(view)
    }

    private lateinit var viewModel          : FieldbookViewModel
    private lateinit var viewAdapter        : FieldbookAdapter
    private lateinit var fragmentActivity   : FragmentActivity

    private val parentView              = view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentActivity = requireActivity()

        viewModel = fragmentActivity.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fieldbook_fragment_home, container, false).also { view ->
            val recyclerView = view.findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
            val addButton = view.findViewById<ImageView>(R.id.fieldbook_addpin)

            viewAdapter = FieldbookAdapter(fragmentActivity, viewModel, parentView)

            viewModel.allFieldbookEntries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                viewAdapter.setFieldbook(it)
            })

            recyclerView.layoutManager = LinearLayoutManager(fragmentActivity)
            recyclerView.adapter = viewAdapter

            val searchBar = view.findViewById<EditText>(R.id.fieldbook_searchbar)

            searchBar.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    //Perform Code
                    val search = searchBar.text.toString()
                    searchFieldbook(search)
                    return@OnKeyListener true
                }
                false
            })

            addButton.setOnClickListener {
                val intent = Intent(requireContext(), FieldbookEditor::class.java)
                startActivity(intent)
            }
        }
    }

    /**
     * Filters on entries with a title that contains or is the given string
     *
     * @param[search] the title we query on
     */
    private fun searchFieldbook(search : String){
        viewModel.search(search){ fieldbook ->
            fieldbook?.let {
                viewAdapter.setFieldbook(fieldbook)
            }
            hideKeyboard(fragmentActivity)
        }
    }

    /**
     * Hides the keyboard, after text input
     *
     * @param[activity] the currently opened activity
     * @param[currentView] the currently opened view
     */
    private fun hideKeyboard(activity: Activity, currentView : View? = null) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        var view = currentView
        if(view == null){
            //Find the currently focused view, so we can grab the correct window token from it.
            view = activity.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
        }

        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}