package com.uu_uce.uiTests

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.uu_uce.AllPins
import com.uu_uce.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class AllPinsTests(){
    @get:Rule
    var activityRule: ActivityTestRule<AllPins>
            = ActivityTestRule(AllPins::class.java)

    @Test
    fun loadAllPins(){
        // Check if allpins successfully loaded
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }
}