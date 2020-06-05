package com.uu_uce.instrumentedTests

import android.content.Context
import android.net.wifi.WifiManager
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
import androidx.test.rule.GrantPermissionRule
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
    private var wifi : WifiManager? = null

    @get:Rule
    var activityRule: ActivityTestRule<GeoMap>
            = ActivityTestRule(GeoMap::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    @Before
    fun init(){
        wifi = activityRule.activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        val pinList: MutableList<PinData> = mutableListOf()
        pinList.add(
            PinData(
                "0",
                "31N314968E4677733N",
                1,
                "TEXT",
                "A",
                "[{\"tag\":\"TEXT\", \"text\":\"test\"}]",
                1,
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                "1",
                "31N313368E4671833N",
                2,
                "IMAGE",
                "B",
                "[{\"tag\":\"IMAGE\", \"file_path\":\"Images/1afccc95-a809-4992-8e89-4f35c7e0b453.png\"}]",
                1,
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                "2",
                "31N313000E4671000N",
                3,
                "VIDEO",
                "C",
                "[{\"tag\":\"VIDEO\", \"file_path\":\"Videos/7fd7ee4c-62ac-4a55-a3aa-30cc91cdaf27.mp4\", \"title\":\"zoo video\"}]",
                1,
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                "3",
                "31N313468E4671533N",
                3,
                "MCQUIZ",
                "D",
                "[{\"tag\":\"TEXT\", \"text\":\"Press right or also right\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\" , \"mc_correct_option\" : \"Also right\", \"mc_incorrect_option\" : \"Also wrong\", \"reward\" : 50}]",
                1,
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
        onView(withId(R.id.dragBar))
            .perform(click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(not(isDisplayed())))

        // The second click should open the full menu
        onView(withId(R.id.dragBar))
            .perform(click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(isDisplayed()))

        // The final click should close the menu
        onView(withId(R.id.dragBar))
            .perform(click())

        onView(withId(R.id.toggle_layer_layout))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.lower_menu_layout))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun profileButton(){
        // First click should open the layer toggle buttons the second should open the lower menu
        onView(withId(R.id.dragBar))
            .perform(click(), click())

        // Test switching to profile
        onView(withId(R.id.profile_button))
            .perform(click())

        // Check if profile successfully loaded
        onView(withId(R.id.profile_layout))
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
        // First click should open the layer toggle buttons the second should open the lower menu
        onView(withId(R.id.dragBar))
            .perform(click(), click())

        // Test switching to fieldbook
        onView(withId(R.id.fieldbook_button))
            .perform(click())

        // Check if fieldbook successfully loaded
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
    fun allPinsButton(){
        // First click should open the layer toggle buttons the second should open the lower menu
        onView(withId(R.id.dragBar))
            .perform(click(), click())

        // Test switching to allPins
        onView(withId(R.id.allpins_button))
            .perform(click())

        // Check if allPins successfully loaded
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
    fun settingsButton(){
        // First click should open the layer toggle buttons the second should open the lower menu
        onView(withId(R.id.dragBar))
            .perform(click(), click())

        // Test switching to settings
        onView(withId(R.id.settings_button))
            .perform(click())

        // Check if settings successfully loaded
        onView(withId(R.id.settings_scrollview))
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
            .perform(tap(pinLocation.first, pinLocation.second - 5))

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
        onView(withId(R.id.dragBar))
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

    /*@Test
    fun deleteAndDownloadMaps(){
        // Open settings
        onView(withId(R.id.dragBar))
            .perform(click(), click())

        onView(withId(R.id.settings_button))
            .perform(click())

        // Check if settings successfully loaded
        onView(withId(R.id.settings_scrollview))
            .check(matches(isDisplayed()))

        // Delete maps
        onView(withId(R.id.delete_maps_button))
            .perform(click())

        // Do not delete maps
        onView(ViewMatchers.withText("No"))
            .perform(click())

        // Check that delete button not disappeared
        onView(withId(R.id.delete_maps_button))
            .check(matches(isDisplayed()))

        // Delete maps
        onView(withId(R.id.delete_maps_button))
            .perform(click())

        // Do not delete maps
        onView(ViewMatchers.withText("Yes"))
            .perform(click())

        // Check that delete button has disappeared
        onView(withId(R.id.delete_maps_button))
            .check(matches(not(isDisplayed())))

        // Check that delete button has disappeared
        onView(withId(R.id.delete_maps_button))
            .check(matches(not(isDisplayed())))

        // Switch back to geomap
        onView(withId(R.id.toolbar_back_button))
            .perform(click())

        // Check if maps are unloaded
        onView(withId(R.id.customMap))
            .check(matches(noLayersLoaded()))

        // Go back to settings
        onView(withId(R.id.dragBar))
            .perform(click())

        onView(withId(R.id.settings_button))
            .perform(click())

        // Check if delete is still gone
        onView(withId(R.id.delete_maps_button))
            .check(matches(not(isDisplayed())))

        // Download maps
        onView(withId(R.id.download_maps_button))
            .perform(click())

        // Wait for download to complete


        // Switch back to geomap
        onView(withId(R.id.toolbar_back_button))
            .perform(click())

        // Check if maps are reloaded
        onView(withId(R.id.customMap))
            .check(matches(layersLoaded()))
    }*/
}