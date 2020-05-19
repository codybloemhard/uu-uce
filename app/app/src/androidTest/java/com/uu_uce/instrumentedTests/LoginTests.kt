package com.uu_uce.instrumentedTests

import android.view.KeyEvent
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.android.material.internal.ContextUtils.getActivity
import com.uu_uce.GeoMap
import com.uu_uce.MainActivity
import com.uu_uce.R
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginTests {

    @get:Rule
    var activityRule: ActivityTestRule<GeoMap>
            = ActivityTestRule(GeoMap::class.java)

    @Before
    fun init(){
        // Open lower menu
        onView(withId(R.id.dragBar))
            .perform(click(), click())

        onView(withId(R.id.logout_button))
            .perform(click())
    }

    @Test
    fun noCredentialsLogin(){
        onView(withId(R.id.signin_button))
            .perform(click())

        onView(withText(R.string.login_nocredentials_message))
            .inRoot(withDecorView(not(`is`(activityRule.activity.window.decorView))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun noPasswordLogin(){
        onView(withId(R.id.username_field))
            .perform(typeText("Username"), pressKey(KeyEvent.KEYCODE_ENTER))

        onView(withId(R.id.password_field))
            .perform(clearText(), pressKey(KeyEvent.KEYCODE_ENTER))

        onView(withId(R.id.signin_button))
            .perform(click())

        onView(withText(R.string.login_nopassword_message))
            .inRoot(withDecorView(not(`is`(activityRule.activity.window.decorView))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun noUsernameLogin(){
        onView(withId(R.id.username_field))
            .perform(clearText(), pressKey(KeyEvent.KEYCODE_ENTER))

        onView(withId(R.id.password_field))
            .perform(typeText("Password"), pressKey(KeyEvent.KEYCODE_ENTER))

        onView(withId(R.id.signin_button))
            .perform(click())

        onView(withText(R.string.login_nousername_message))
            .inRoot(withDecorView(not(`is`(activityRule.activity.window.decorView))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun successFullLogin(){
        onView(withId(R.id.username_field))
            .perform(typeText("Username"), pressKey(KeyEvent.KEYCODE_ENTER))

        onView(withId(R.id.password_field))
            .perform(typeText("Password"), pressKey(KeyEvent.KEYCODE_ENTER))

        onView(withId(R.id.signin_button))
            .perform(click())

        onView(withId(R.id.customMap))
            .check(matches(isDisplayed()))
    }
}