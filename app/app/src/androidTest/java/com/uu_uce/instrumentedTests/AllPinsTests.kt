package com.uu_uce.instrumentedTests

import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.uu_uce.AllPins
import com.uu_uce.R
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.childAtPosition
import com.uu_uce.clickChildViewWithId
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep


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
                "[{\"tag\":\"IMAGE\", \"file_path\":\"Images/75751e73-4864-41cf-a667-e4202269eca6.png\"}]",
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
                "[{\"tag\":\"VIDEO\", \"file_path\":\"Videos/c5a553b5-a682-49ee-ae58-a898a987cb44.mp4\", \"thumbnail\" : \"Videos/Thumbnails/f5e3c37b-b3c5-4eb4-a3bb-8299b6e671b1.png\", \"title\":\"zoo video\"}]",
                1,
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                "3",
                "31N3134680E46715335N",
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

        // Set sorting by type
        sharedPref = PreferenceManager.getDefaultSharedPreferences(activityRule.activity)
        with(sharedPref.edit()) {
            putInt("com.uu_uce.SORTMODE", 4)
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
                    0, clickChildViewWithId(R.id.recyclerview_item)
                )
            )

        // Check if pin successfully opened
        onView(withId(R.id.popup_window_view))
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
                0, clickChildViewWithId(R.id.recyclerview_item)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.popup_window_view))
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
                3, clickChildViewWithId(R.id.recyclerview_item)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.video_block))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Open video player
        onView(withId(R.id.video_block))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if video player opened successfully
        onView(withId(R.id.video_viewer_layout))
            .inRoot(not(isPlatformPopup()))
            .check(matches(isDisplayed()))

        // Close video player using close button
        onView(withId(R.id.close_video_player))
            .inRoot(not(isPlatformPopup()))
            .perform(click())

        // Check if the player was closed successfully
        onView(withId(R.id.video_block))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndCloseVideoBack(){
        // Open video pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                3, clickChildViewWithId(R.id.recyclerview_item)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.video_block))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Open video player
        onView(withId(R.id.video_block))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if video player opened successfully
        onView(withId(R.id.video_viewer_layout))
            .inRoot(not(isPlatformPopup()))
            .check(matches(isDisplayed()))

        // Close video player using back button
        pressBack()

        // Check if the player was closed successfully
        onView(withId(R.id.video_block))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun multipleChoiceSuccess(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.recyclerview_item)
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
        )
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Click finish button
        onView(withId(R.id.finish_quiz_button))
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Check to see if popup was correct
        sleep(500)
        onView(withId(R.id.quiz_result_text))
            .inRoot(isPlatformPopup())
            .check(matches(withText(R.string.pin_quiz_success_head)))

        // Reopen popup
        onView(withId(R.id.reopen_button))
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Check if pin is completed
        onView(withId(R.id.completed_marker))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun multipleChoiceFail(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.recyclerview_item)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Select wrong answer
        onView(
            allOf(
                isDescendantOfA(withId(R.id.multiple_choice_table)),
                withText("Wrong")
            )
        )
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Click finish button
        onView(withId(R.id.finish_quiz_button))
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Check to see if popup was correct
        sleep(500)
        onView(withId(R.id.quiz_result_text))
            .inRoot(isPlatformPopup())
            .check(matches(withText(R.string.pin_quiz_fail_head)))

        // Reopen popup
        onView(withId(R.id.reopen_button))
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Check if pin is completed
        onView(withId(R.id.completed_marker))
            .inRoot(isPlatformPopup())
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun multipleChoiceCloseWarning(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.recyclerview_item)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Select an answer
        onView(
            allOf(
                isDescendantOfA(withId(R.id.multiple_choice_table)),
                withText("Also right")
            ))
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Attempt to close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see that warning pops up when progress is made
        onView(withText(activityRule.activity.getString(R.string.pin_close_warning_head)))
            .check(matches(isDisplayed()))

        // Stay in pin
        onView(withText(activityRule.activity.getString(R.string.negative_button_text)))
            .perform(click())

        //Check to see that pin didn't close
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Attempt to close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Close pin
        onView(withText(activityRule.activity.getString(R.string.positive_button_text)))
            .perform(click())

        // Check if pin closed
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun multipleChoiceCloseNoWarning(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.recyclerview_item)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Attempt to close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see that warning doesn't pops up when no progress is made
        onView(withText(activityRule.activity.getString(R.string.pin_close_warning_head)))
            .check(doesNotExist())

        sleep(100)

        // Check if pin closed
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun pinSorting(){
        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by title
        onView(withText(activityRule.activity.getString(R.string.allpins_sorting_title_az)))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("A")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by title reversed
        onView(withText(activityRule.activity.getString(R.string.allpins_sorting_title_za)))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("D")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by difficulty
        onView(withText(activityRule.activity.getString(R.string.allpins_sorting_difficulty_easyhard)))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("A")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by difficulty reversed
        onView(withText(activityRule.activity.getString(R.string.allpins_sorting_difficulty_hardeasy)))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("C")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by type
        onView(withText(activityRule.activity.getString(R.string.allpins_sorting_type_az)))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("B")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by type reversed
        onView(withText(activityRule.activity.getString(R.string.allpins_sorting_type_za)))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("C")))
    }

    @Test
    fun pinSearching(){
        // Search for D
        onView(withId(R.id.pins_searchbar))
            .perform(typeText("D"), pressKey(KeyEvent.KEYCODE_ENTER))

        sleep(100)

        // Check if D was found
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("D")))

        // Press clear search to stop searching
        onView(withId(R.id.pins_searchbar))
            .perform(clearText(), pressKey(KeyEvent.KEYCODE_ENTER))

        sleep(100)

        // Check if sorting was stopped
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("B")))

        // Search for C
        onView(withId(R.id.pins_searchbar))
            .perform(typeText("C"), pressKey(KeyEvent.KEYCODE_ENTER))

        sleep(100)

        // Check if C was found
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("C")))
    }
}