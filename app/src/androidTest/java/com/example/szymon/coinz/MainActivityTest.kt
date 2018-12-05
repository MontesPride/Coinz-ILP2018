package com.example.szymon.coinz

import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mRuntimePermissionRule = GrantPermissionRule
            .grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun mainActivityExchangeCoin() {

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Bank))
                .perform(ViewActions.click())

        Espresso.onView((ViewMatchers.withId(R.id.coinExchange)))
                .perform(ViewActions.click())

    }

    @Test
    fun mainActivityWagerNoGold() {

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Race))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.race_WagerButton))
                .perform(ViewActions.click())

        /*Espresso.onView(ViewMatchers.withId(R.id.main_WagerTextView))
                .check(matches(isDisplayed()))*/

        Espresso.onView(ViewMatchers.withId(R.id.race_WagerTextView))
                .check(matches(withText("You don't have enough GOLD")))

    }

    @Test
    fun mainActivityAlreadyWagered() {

        Espresso.onView(ViewMatchers.withId(R.id.go_to_login))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.login_email))
                .perform(ViewActions.replaceText("EspressoEmail@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.login_password))
                .perform(ViewActions.replaceText("EspressoPassword"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_in_button))
                .perform(ViewActions.click())

        Thread.sleep(5000)

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Race))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.race_WagerButton))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.race_WagerTextView))
                .check(matches(withText("You have already wagered today")))

    }

    @Test
    fun mainActiviRerollQuest() {

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Quest))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.quest_rerollButton))
                .perform(ViewActions.click())

        Thread.sleep(1000)

        Espresso.onView(ViewMatchers.withId(R.id.quest_rerollButton))
                .check(matches(not(isEnabled())))

    }

    @Test
    fun mainActivitySpinnerSelect() {

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Race))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.race_TimeSpinner))
                .perform(ViewActions.click())

        Espresso.onData(hasToString(startsWith("30")))
                .perform(ViewActions.click())

    }



}