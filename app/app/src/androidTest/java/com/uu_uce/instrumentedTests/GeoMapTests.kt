package com.uu_uce.instrumentedTests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.uu_uce.*
import com.uu_uce.allpins.PinData
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class GeoMapTests {
    private lateinit var pinLocation : Pair<Float, Float>
    @get:Rule
    var activityRule: ActivityTestRule<GeoMap>
            = ActivityTestRule(GeoMap::class.java)

    @Before
    fun init(){
        val pinList: MutableList<PinData> = mutableListOf()
        pinList.add(
            PinData(
                0,
                "31N46777336N3149680E",
                1,
                "TEXT",
                "A",
                "[{\"tag\":\"TEXT\", \"text\":\"test\"}]",
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                1,
                "31N46718336N3133680E",
                2,
                "IMAGE",
                "B",
                "[{\"tag\":\"IMAGE\", \"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/images/test.png\"}]",
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                2,
                "31N46710000N3130000E",
                3,
                "VIDEO",
                "C",
                "[{\"tag\":\"VIDEO\", \"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/videos/zoo.mp4\", \"thumbnail\":\"file:///data/data/com.uu_uce/files/pin_content/videos/thumbnails/zoothumbnail.png\", \"title\":\"zoo video\"}]",
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                3,
                "31N46715335N3134680E",
                3,
                "MCQUIZ",
                "D",
                "[{\"tag\":\"TEXT\", \"text\":\"Press right or also right\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\" , \"mc_correct_option\" : \"Also right\", \"mc_incorrect_option\" : \"Also wrong\", \"reward\" : 50}]",
                1,
                "-1",
                "-1"
            )
        )
        activityRule.activity.setPinData(pinList)

        pinLocation = activityRule.activity.getPinLocation()
    }

    @Test
    fun dragButtonClicks() {
        // First click should open the layer toggle buttons
        onView(withId(R.id.dragButton))
            .perform(click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(not(isDisplayed())))

        // The second click should open the full menu
        onView(withId(R.id.dragButton))
            .perform(click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(isDisplayed()))

        // The final click should close the menu
        onView(withId(R.id.dragButton))
            .perform(click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun allPinsButton(){
        // First click should open the layer toggle buttons
        onView(withId(R.id.dragButton))
            .perform(click(), click())

        // Test switching to all pins
        onView(withId(R.id.allpins_button))
            .perform(click())

        // Check if allpins successfully loaded
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))

        // Switch back to geomap
        onView(withId(R.id.toolbar_back_button))
            .perform(click())

        // Check if geomap successfully loaded
        onView(withId(R.id.lower_menu_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun fieldbookButton(){
        // First click should open the layer toggle buttons
        onView(withId(R.id.dragButton))
            .perform(click(), click())

        // Test switching to all pins
        onView(withId(R.id.fieldbook_button))
            .perform(click())

        // Check if allpins successfully loaded
        onView(withId(R.id.fieldbook_recyclerview))
            .check(matches(isDisplayed()))

        // Switch back to geomap
        onView(withId(R.id.toolbar_back_button))
            .perform(click())

        // Check if geomap successfully loaded
        onView(withId(R.id.lower_menu_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun openPin(){
        // Tap pin on the map
        onView(withId(R.id.customMap))
            .perform(tap(pinLocation.first, pinLocation.second))

        // Check if popup opened successfully
        onView(withId(R.id.popup_window_view))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Close pin popup
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if popup closed
        onView(withId(R.id.popup_window_view))
            .check(doesNotExist())
    }

    @Test
    fun toggleLayer(){
        // First click should open the layer toggle buttons
        onView(withId(R.id.dragButton))
            .perform(click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(isDisplayed()))

        // Check whether layer is showing initially
        onView(withId(R.id.customMap))
            .check(matches(layerShowing(0)))

        // Toggle layer off
        onView(childAtPosition(withId(R.id.toggle_layer_scroll), 0))
            .perform(click())

        // Check to see if layer was disabled
        onView(withId(R.id.customMap))
            .check(matches(not(layerShowing(0))))

        // Toggle layer back on
        onView(childAtPosition(withId(R.id.toggle_layer_scroll), 0))
            .perform(click())

        // Check whether layer is showing again
        onView(withId(R.id.customMap))
            .check(matches(layerShowing(0)))
    }

    @Test
    fun centerCamera(){
        // Click button to center location
        onView(withId(R.id.center_button))
            .perform(click())

        // Check if camera is centered
        onView(withId(R.id.customMap))
            .check(matches(cameraCentered()))
    }

    @Test
    fun zoomOutCamera(){
        // Zoom in the camera
        onView(withId(R.id.customMap))
            .perform(zoomIn())

        // Check if camera is zoomed in
        onView(withId(R.id.customMap))
            .check(matches(not(cameraZoomedOut())))

        // Double tap the screen to zoom out
        onView(withId(R.id.customMap))
            .perform(doubleClick())

        // Check if camera is zoomed out
        onView(withId(R.id.customMap))
            .check(matches(cameraZoomedOut()))
    }
}