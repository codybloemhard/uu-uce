package com.uu_uce


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {
        val appCompatButton = onView(
            allOf(
                withId(R.id.fieldbook_button), withText("My fieldbook"),
                childAtPosition(
                    allOf(
                        withId(R.id.lower_menu_layout),
                        childAtPosition(
                            withId(R.id.menu),
                            3
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatButton.perform(click())

        val appCompatImageButton = onView(
            allOf(
                withId(R.id.toolbar_back_button), withContentDescription("Back Button"),
                childAtPosition(
                    allOf(
                        withId(R.id.general_menu_layout),
                        childAtPosition(
                            withId(R.id.fieldbook_layout),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatImageButton.perform(click())

        val appCompatButton2 = onView(
            allOf(
                withId(R.id.allpins_button), withText("Pins"),
                childAtPosition(
                    allOf(
                        withId(R.id.lower_menu_layout),
                        childAtPosition(
                            withId(R.id.menu),
                            3
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatButton2.perform(click())

        val appCompatButton3 = onView(
            allOf(
                withId(R.id.open_button), withText("Open"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.LinearLayout")),
                        2
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatButton3.perform(click())

        val frameLayout = onView(
            childAtPosition(
                childAtPosition(
                    withId(R.id.multiple_choice_table),
                    1
                ),
                0
            )
        )
        frameLayout.perform(scrollTo(), click())

        val button = onView(
            allOf(
                withText("Finish"),
                childAtPosition(
                    allOf(
                        withId(R.id.scrollLayout),
                        childAtPosition(
                            withId(R.id.scroll_view),
                            0
                        )
                    ),
                    2
                )
            )
        )
        button.perform(scrollTo(), click())
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
