package com.uu_uce.instrumentedTests

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
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
import com.uu_uce.FieldbookEditor.Companion.currentUri
import com.uu_uce.fieldbook.FieldbookViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


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

        intending(hasAction(Intent.ACTION_GET_CONTENT))
            .respondWith(setFile(Uri.parse("/sdcard/Android/data/com.uu_uce/files/PinContent/Images/75751e73-4864-41cf-a667-e4202269eca6.png")))
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
            .respondWith(setFile(Uri.parse("/sdcard/Android/data/com.uu_uce/files/PinContent/Images/75751e73-4864-41cf-a667-e4202269eca6.png")))
        intending(hasAction(MediaStore.ACTION_VIDEO_CAPTURE))
            .respondWith(setFile(Uri.parse("/sdcard/Android/data/com.uu_uce/files/PinContent/Videos/c5a553b5-a682-49ee-ae58-a898a987cb44.mp4")))
    }

    @Test
    fun loadFieldbook(){
        // Check if fieldbook successfully loaded
        onView(withId(R.id.fieldbook_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun fieldbookTitle(){
        val testTitle = "This is a dummy title"

        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Type title
        onView(withId(R.id.add_title))
            .perform(typeText(testTitle), closeSoftKeyboard())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.fieldbook_pin_editor))
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
        onView(withId(R.id.popup_window_view))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Check to see if pin was has correct content
        onView(withId(R.id.popup_window_title))
            .inRoot(isPlatformPopup())
            .check(matches(withText(testTitle)))

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
        onView(withText(intentsTestRule.activity.getString(R.string.delete_popup_title)))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText(intentsTestRule.activity.getString(R.string.negative_button_text)))
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
        onView(withText(intentsTestRule.activity.getString(R.string.positive_button_text)))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    @Test
    fun textPin(){
        val testText = "Lorem ipsum dolor sit amet"
        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Add text block
        onView(withId(R.id.add_text_block))
            .perform(click())

        // Type text
        onView(withId(R.id.text_field))
            .perform(typeText(testText), closeSoftKeyboard())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.fieldbook_pin_editor))
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
        onView(withText(intentsTestRule.activity.getString(R.string.delete_popup_title)))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText(intentsTestRule.activity.getString(R.string.negative_button_text)))
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
        onView(withText(intentsTestRule.activity.getString(R.string.positive_button_text)))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    @Test
    fun galleryImagePin(){
        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Upload image
        onView(withId(R.id.add_image_block))
            .perform(click())

        onView(withText(intentsTestRule.activity.getString(R.string.editor_imageselection_gallery)))
            .perform(click())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.fieldbook_pin_editor))
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
        onView(withId(R.id.popup_window_view))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Check to see if pin was has correct content
        onView(childAtPosition(withId(R.id.scrollLayout), 0))
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
        onView(withText(intentsTestRule.activity.getString(R.string.delete_popup_title)))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText(intentsTestRule.activity.getString(R.string.negative_button_text)))
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
        onView(withText(intentsTestRule.activity.getString(R.string.positive_button_text)))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    @Test
    fun cameraImagePin(){
        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Upload image
        onView(withId(R.id.add_image_block))
            .perform(click())

        onView(withText(intentsTestRule.activity.getString(R.string.editor_imageselection_camera)))
            .perform(click())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.fieldbook_pin_editor))
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
        onView(withId(R.id.popup_window_view))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Check to see if pin was has correct content
        onView(childAtPosition(withId(R.id.scrollLayout), 0))
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
        onView(withText(intentsTestRule.activity.getString(R.string.delete_popup_title)))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText(intentsTestRule.activity.getString(R.string.negative_button_text)))
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
        onView(withText(intentsTestRule.activity.getString(R.string.positive_button_text)))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    @Test
    fun cameraVideoPin(){
        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Upload image
        onView(withId(R.id.add_video_block))
            .perform(click())

        onView(withText(intentsTestRule.activity.getString(R.string.editor_videoselection_camera)))
            .perform(click())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .perform(click())

        // Check to see that popup disappeared
        onView(withId(R.id.fieldbook_pin_editor))
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
        onView(withId(R.id.popup_window_view))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Check to see if pin was has correct content
        onView(childAtPosition(withId(R.id.scrollLayout), 0))
            .inRoot(isPlatformPopup())
            .check(matches(withId(R.id.video_block)))

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
        onView(withText(intentsTestRule.activity.getString(R.string.delete_popup_title)))
            .check(matches(isDisplayed()))

        // Cancel deletion
        onView(withText(intentsTestRule.activity.getString(R.string.negative_button_text)))
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
        onView(withText(intentsTestRule.activity.getString(R.string.positive_button_text)))
            .perform(click())

        // Check if pin has been deleted
        onView(childAtPosition(withId(R.id.fieldbook_recyclerview), 0))
            .check(doesNotExist())
    }

    @Test
    fun openEditor(){
        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Upload image
        onView(withId(R.id.add_video_block))
            .perform(click())

        onView(withText(intentsTestRule.activity.getString(R.string.editor_videoselection_camera)))
            .perform(click())

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .perform(click())

        // Open new pin
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, click()
            )
        )

        // Check to see if pin opened successfully
        onView(withId(R.id.popup_window_view))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Open editor
        onView(withId(R.id.popup_window_edit_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if editor successfully opened
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))
    }

    @Test
    fun removeBlock(){
        val stringToBeTyped = "Test text content block"
        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Add text
        onView(withId(R.id.add_text_block))
            .perform(click())

        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .perform(typeText(stringToBeTyped))

        // Finish pin
        onView(withId(R.id.add_fieldbook_pin))
            .perform(click())

        // Open new pin
        onView(withId(R.id.fieldbook_recyclerview)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, click()
            )
        )

        // Check to see if pin opened successfully
        onView(withId(R.id.popup_window_view))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        // Open editor
        onView(withId(R.id.popup_window_edit_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if editor successfully opened
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Feign deletion
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .perform(longClick())

        onView(withText(intentsTestRule.activity.getString(R.string.cancel_button)))
            .perform(click())

        // Make sure text wasn't deleted
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .check(matches(isDisplayed()))

        // Actually delete
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .perform(longClick())

        onView(withText(intentsTestRule.activity.getString(R.string.editor_delete_block)))
            .perform(click())

        // Make sure text was deleted
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .check(doesNotExist())
    }

    @Test
    fun moveBlock(){
        val firstString = "Start at the top"
        val secondString = "Start at the bottom"

        //Open add popup
        onView(withId(R.id.fieldbook_addpin))
            .perform(click())

        // Check if popup opened up
        onView(withId(R.id.fieldbook_pin_editor))
            .check(matches(isDisplayed()))

        // Add text
        onView(withId(R.id.add_text_block))
            .perform(click())

        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .perform(typeText(firstString))

        // Add text
        onView(withId(R.id.add_text_block))
            .perform(click())

        onView(childAtPosition(withId(R.id.fieldbook_content_container), 1))
            .perform(typeText(secondString))

        // Check if order was correct
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .check(matches(withText(firstString)))

        onView(childAtPosition(withId(R.id.fieldbook_content_container), 1))
            .check(matches(withText(secondString)))

        // Open edit menu
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .perform(longClick())

        // Check that move up is unavailable
        onView(withText(intentsTestRule.activity.getString(R.string.editor_moveup)))
            .check(doesNotExist())

        // Move down
        onView(withText(intentsTestRule.activity.getString(R.string.editor_movedown)))
            .perform(click())

        // Check if order was correct
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .check(matches(withText(secondString)))

        onView(childAtPosition(withId(R.id.fieldbook_content_container), 1))
            .check(matches(withText(firstString)))

        // Open edit menu
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 1))
            .perform(longClick())

        // Check that move down is unavailable
        onView(withText(intentsTestRule.activity.getString(R.string.editor_movedown)))
            .check(doesNotExist())

        // Move up
        onView(withText(intentsTestRule.activity.getString(R.string.editor_moveup)))
            .perform(click())


        // Check if order was correct
        onView(childAtPosition(withId(R.id.fieldbook_content_container), 0))
            .check(matches(withText(firstString)))

        onView(childAtPosition(withId(R.id.fieldbook_content_container), 1))
            .check(matches(withText(secondString)))
    }

    private fun setFile(uri: Uri) : Instrumentation.ActivityResult? {
        val resultData = Intent()
        resultData.data = uri
        currentUri = uri
        return Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    }
}