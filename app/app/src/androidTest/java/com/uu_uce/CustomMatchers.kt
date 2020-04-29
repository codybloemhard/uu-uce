package com.uu_uce

import android.view.View
import android.view.ViewGroup
import com.uu_uce.views.CustomMap

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {
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

fun layerShowing(layer : Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("Layer $layer is showing")
        }

        public override fun matchesSafely(view: View): Boolean {
            return view is CustomMap
                    && view.checkLayerVisibility(layer)
        }
    }
}

fun cameraCentered(): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("User location is at center of the screen")
        }

        public override fun matchesSafely(view: View): Boolean {
            return view is CustomMap
                    && view.userLocCentral()
        }
    }
}

fun cameraZoomedOut(): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("Camera is zoomed out all the way")
        }

        public override fun matchesSafely(view: View): Boolean {
            return view is CustomMap
                    && view.cameraZoomedOut()
        }
    }
}