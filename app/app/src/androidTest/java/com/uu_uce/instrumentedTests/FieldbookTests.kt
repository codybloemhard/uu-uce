package com.uu_uce.instrumentedTests

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.uu_uce.Fieldbook
import com.uu_uce.R
import com.uu_uce.childAtPosition
import com.uu_uce.fieldbook.FieldbookViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


@RunWith(AndroidJUnit4::class)
@LargeTest
class FieldbookTests {
    private lateinit var fieldbookViewmodel: FieldbookViewModel

    @get:Rule
    var intentsTestRule: IntentsTestRule<Fieldbook>
            = IntentsTestRule(Fieldbook::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA"
        )

    @Before
    fun init(){
        fieldbookViewmodel = ViewModelProvider(intentsTestRule.activity).get(FieldbookViewModel::class.java)
        fieldbookViewmodel.deleteAll()

        intending(hasAction(Intent.ACTION_PICK)).respondWith(getFileURIResult())
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(getFileBitmap())
    }

    @Test
    fun loadFieldbook(){
        // Check if fieldbook successfully loaded
        onView(withId(R.id.fieldbook_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun textPin(){
        val testText = "Lorem ipsum dolor sit amet"
        //Open add popup
        onView(withId(R.id.fieldbook_fab))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.add_fieldbook_pin_popup))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Type text
        onView(withId(R.id.add_text_block))
            .inRoot(isPlatformPopup())
            .perform(typeText(testText), closeSoftKeyboard())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.add_fieldbook_pin_popup))
            .check(doesNotExist())

        // Check to see that pin was created
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(matches(isDisplayed()))

        // Open new pin
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, click()
            )
        )

        // Check to see if pin opened successfully
        onView(withId(R.id.scrollLayout))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Check to see if pin was has correct content
        onView(childAtPosition(withId(R.id.scrollLayout), 0))
            .inRoot(isPlatformPopup())
            .check(matches(withText(testText)))

        // Close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see if pin closed successfully
        onView(withId(R.id.popup_window_view))
            .check(doesNotExist())

        // Open delete dialog
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, longClick()
            )
        )

        // Check to see if delete dialog popped up
        onView(withText("Delete"))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText("NO"))
            .perform(click())

        // Check if pin is still there
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(matches(isDisplayed()))

        // Open delete dialog again
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, longClick()
            )
        )

        // Confirm deletion
        onView(withText("YES"))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    @Test
    fun galleryImagePin(){
        //Open add popup
        onView(withId(R.id.fieldbook_fab))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.add_fieldbook_pin_popup))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Upload image
        onView(withId(R.id.add_image_block))
            .inRoot(isPlatformPopup())
            .perform(click())

        onView(withText("Choose from gallery"))
            .perform(click())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.add_fieldbook_pin_popup))
            .check(doesNotExist())

        // Check to see that pin was created
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(matches(isDisplayed()))

        // Open new pin
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, click()
            )
        )

        // Check to see if pin opened successfully
        onView(withId(R.id.scrollLayout))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Check to see if pin was has correct content
        onView(childAtPosition(withId(R.id.scrollLayout), 1))
            .inRoot(isPlatformPopup())
            .check(matches(withId(R.id.image_block)))

        // Close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see if pin closed successfully
        onView(withId(R.id.popup_window_view))
            .check(doesNotExist())

        // Open delete dialog
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, longClick()
            )
        )

        // Check to see if delete dialog popped up
        onView(withText("Delete"))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText("NO"))
            .perform(click())

        // Check if pin is still there
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(matches(isDisplayed()))

        // Open delete dialog again
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, longClick()
            )
        )

        // Confirm deletion
        onView(withText("YES"))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    @Test
    fun cameraImagePin(){
        //Open add popup
        onView(withId(R.id.fieldbook_fab))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.add_fieldbook_pin_popup))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Upload image
        onView(withId(R.id.add_video_block))
            .inRoot(isPlatformPopup())
            .perform(click())

        onView(withText("Take Photo"))
            .perform(click())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.add_fieldbook_pin_popup))
            .check(doesNotExist())

        // Check to see that pin was created
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(matches(isDisplayed()))

        // Open new pin
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, click()
            )
        )

        // Check to see if pin opened successfully
        onView(withId(R.id.scrollLayout))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Check to see if pin was has correct content
        onView(childAtPosition(withId(R.id.scrollLayout), 1))
            .inRoot(isPlatformPopup())
            .check(matches(withId(R.id.image_block)))

        // Close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see if pin closed successfully
        onView(withId(R.id.popup_window_view))
            .check(doesNotExist())

        // Open delete dialog
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, longClick()
            )
        )

        // Check to see if delete dialog popped up
        onView(withText("Delete"))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText("NO"))
            .perform(click())

        // Check if pin is still there
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(matches(isDisplayed()))

        // Open delete dialog again
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, longClick()
            )
        )

        // Confirm deletion
        onView(withText("YES"))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    private fun getFileURIResult(): Instrumentation.ActivityResult? {
        val resultData = Intent()
        val dir: File? = intentsTestRule.activity.filesDir
        val file = File(dir?.path, "pin_content/images/test.png")
        val uri: Uri = Uri.fromFile(file)
        resultData.data = uri
        return Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    }

    private fun getFileBitmap(): Instrumentation.ActivityResult? {
        val resultData = Intent()
        val filePath = "/data/data/com.uu_uce/files/pin_content/images/test.png"
        val bitmap = BitmapFactory.decodeFile(filePath)
        resultData.putExtra("data", bitmap)
        return Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    }
}