package com.uu_uce.uiTests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.uu_uce.GeoMap
import com.uu_uce.R
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MenuTests {
    @get:Rule
    var activityRule: ActivityTestRule<GeoMap>
            = ActivityTestRule(GeoMap::class.java)

    @Test
    fun dragButtonClicks() {
        // First click should open the layer toggle buttons
        onView(withId(R.id.dragButton))
            .perform(ViewActions.click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(not(isDisplayed())))

        // The second click should open the full menu
        onView(withId(R.id.dragButton))
            .perform(ViewActions.click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(isDisplayed()))

        // The final click should close the menu
        onView(withId(R.id.dragButton))
            .perform(ViewActions.click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun allPinsButton(){
        // First click should open the layer toggle buttons
        onView(withId(R.id.dragButton))
            .perform(ViewActions.click(), ViewActions.click())

        // Test switching to all pins
        onView(withId(R.id.allpins_button))
            .perform(ViewActions.click())

        // Check if allpins successfully loaded
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))

        // Switch back to geomap
        onView(withId(R.id.toolbar_back_button))
            .perform(ViewActions.click())

        // Check if geomap successfully loaded
        onView(withId(R.id.lower_menu_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun fieldbookButtonTest(){
        // First click should open the layer toggle buttons
        onView(withId(R.id.dragButton))
            .perform(ViewActions.click(), ViewActions.click())

        // Test switching to all pins
        onView(withId(R.id.fieldbook_button))
            .perform(ViewActions.click())

        // Check if allpins successfully loaded
        onView(withId(R.id.fieldbook_recyclerview))
            .check(matches(isDisplayed()))

        // Switch back to geomap
        onView(withId(R.id.toolbar_back_button))
            .perform(ViewActions.click())

        // Check if geomap successfully loaded
        onView(withId(R.id.lower_menu_layout))
            .check(matches(isDisplayed()))
    }
}