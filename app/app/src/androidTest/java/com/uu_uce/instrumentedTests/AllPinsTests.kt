package com.uu_uce.instrumentedTests

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.uu_uce.AllPins
import com.uu_uce.R
import com.uu_uce.clickChildViewWithId
import com.uu_uce.databases.PinData
import com.uu_uce.databases.PinViewModel
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class AllPinsTests {
    private lateinit var pinViewModel: PinViewModel
    private lateinit var sharedPref : SharedPreferences

    @get:Rule
    var activityRule: ActivityTestRule<AllPins>
            = ActivityTestRule(AllPins::class.java)

    @Before
    fun initDatabase(){

        // Populate test database
        pinViewModel = ViewModelProvider(activityRule.activity).get(PinViewModel::class.java)
        val pinList: MutableList<PinData> = mutableListOf()
        pinList.add(
            PinData(
                0,
                "31N46777336N3149680E",
                1,
                "TEXT",
                "Test text",
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
                "Test image",
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
                "Test video",
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
                "Test quiz",
                "[{\"tag\":\"TEXT\", \"text\":\"Press right or also right\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\" , \"mc_correct_option\" : \"Also right\", \"mc_incorrect_option\" : \"Also wrong\", \"reward\" : 50}]",
                1,
                "-1",
                "-1"
            )
        )

        // Set sorting by type
        sharedPref = activityRule.activity.getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("selectedOption", 4)
            apply()
        }

        pinViewModel.setPins(pinList)
    }

    @Test
    fun loadAllPins(){
        // Check if allpins successfully loaded
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndClosePinClose(){
        // Open first pin
        onView(withId(R.id.allpins_recyclerview)).perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0, clickChildViewWithId(R.id.open_button)
                )
            )

        // Check if pin successfully opened
        onView(withId(R.id.scrollLayout))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Close pin content with close button
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if pin closed successfully
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndClosePinBack(){
        // Open first pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.scrollLayout))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Close pin content with back button
        pressBack()

        // Check if pin closed successfully
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndCloseVideoCross(){
        // Open video pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                3, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Open video player
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if video player opened successfully
        onView(withId(R.id.video_player))
            .inRoot(not(isPlatformPopup()))
            .check(matches(isDisplayed()))

        // Close video player using close button
        onView(withId(R.id.close_video_player))
            .inRoot(not(isPlatformPopup()))
            .perform(click())

        // Check if the player was closed successfully
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndCloseVideoBack(){
        // Open video pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                3, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Open video player
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if video player opened successfully
        onView(withId(R.id.video_player))
            .inRoot(not(isPlatformPopup()))
            .check(matches(isDisplayed()))

        // Close video player using back button
        pressBack()

        // Check if the player was closed successfully
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun multipleChoiceSuccess(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Select correct answer
        onView(
            allOf(
                isDescendantOfA(withId(R.id.multiple_choice_table)),
                withText("Right")
            )
        ).perform(click())

        // Click finish button

        // Check to see if popup was correct

        // Reopen popup

        // Check if pin is completed
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}