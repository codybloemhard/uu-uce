package com.uu_uce

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.MotionEvents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.uu_uce.views.CustomMap
import org.hamcrest.Matcher


fun clickChildViewWithId(id: Int): ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View>? {
            return null
        }

        override fun getDescription(): String {
            return "Click on a child view with specified id."
        }

        override fun perform(uiController: UiController?, view: View) {
            val v: View = view.findViewById(id)
            v.performClick()
        }
    }
}

fun tap(x: Float, y: Float): ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isDisplayed()
        }

        override fun getDescription(): String {
            return "Send touch events."
        }

        override fun perform(
            uiController: UiController,
            view: View
        ) {
            // Get view absolute position
            val location = IntArray(2)
            view.getLocationOnScreen(location)

            // Offset coordinates by view position
            val coordinates =
                floatArrayOf(x + location[0], y + location[1])
            val precision = floatArrayOf(1f, 1f)

            // Send down event, pause, and send up
            val down = MotionEvents.sendDown(uiController, coordinates, precision).down
            uiController.loopMainThreadForAtLeast(200)
            MotionEvents.sendUp(uiController, down, coordinates)
        }
    }
}

fun zoomIn() : ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isDisplayed()
        }

        override fun getDescription(): String {
            return "Send touch events."
        }

        override fun perform(
            uiController: UiController,
            view: View
        ) {
            val customMap = view as CustomMap
            customMap.zoomIn()
        }
    }
}
