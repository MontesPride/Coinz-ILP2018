package com.example.szymon.coinz


import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.hasErrorText
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SignUpActivityTest1 {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SignUpActivity::class.java)

    @Rule
    @JvmField
    var mRuntimePermissionRule = GrantPermissionRule
            .grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun signUpActivityTestNoCredentials() {

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_up_button))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.signup_username))
                .check(matches(hasErrorText("This username is too short")))

    }

    @Test
    fun signUpActivityTestOnlyUsername() {

        Espresso.onView(ViewMatchers.withId(R.id.signup_username))
                .perform(ViewActions.replaceText("EspressoUsername"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_up_button))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.signup_email))
                .check(matches(hasErrorText("This field is required")))
    }

    @Test
    fun signUpActivityTestNoPassword() {

        Espresso.onView(ViewMatchers.withId(R.id.signup_username))
                .perform(ViewActions.replaceText("EspressoUsername"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_email))
                .perform(ViewActions.replaceText("EspressoEmail@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_up_button))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.signup_password))
                .check(matches(hasErrorText("This password is too short")))

    }

    @Test
    fun signUpActivityTestInvalidEmail() {

        Espresso.onView(ViewMatchers.withId(R.id.signup_username))
                .perform(ViewActions.replaceText("EspressoUsername"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_password))
                .perform(ViewActions.replaceText("EspressoPassword"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_email))
                .perform(ViewActions.replaceText("EspressoEmailgmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_up_button))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.signup_email))
                .check(matches(hasErrorText("This email address is invalid")))

    }

    @Test
    fun signUpActivityTestValidRegister() {

        Espresso.onView(ViewMatchers.withId(R.id.signup_username))
                .perform(ViewActions.replaceText("EspressoUsername"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_password))
                .perform(ViewActions.replaceText("EspressoPassword"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_email))
                .perform(ViewActions.replaceText("EspressoEmail@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_up_button))
                .perform(ViewActions.click())

        Thread.sleep(15000)

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

    }


}
