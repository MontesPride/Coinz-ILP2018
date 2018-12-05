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
import org.hamcrest.Matchers
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FinalFullTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SignUpActivity::class.java)

    @Rule
    @JvmField
    var mRuntimePermissionRule = GrantPermissionRule
            .grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun espressoFinalTest() {

        Espresso.onView(ViewMatchers.withId(R.id.signup_username))
                .perform(ViewActions.replaceText("EspressoUsernameFinal"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_password))
                .perform(ViewActions.replaceText("EspressoPasswordFinal"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_email))
                .perform(ViewActions.replaceText("EspressoEmailFinal@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_up_button))
                .perform(ViewActions.click())

        Thread.sleep(25000)

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Ranking))
                .perform(ViewActions.click())

        Thread.sleep(1000)

        Espresso.onView(ViewMatchers.withId(R.id.ranking_PlayerRanking))
                .check(matches(withText("Player Ranking")))

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Bank))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Race))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.race_TimeSpinner))
                .perform(ViewActions.click())

        Espresso.onData(Matchers.hasToString(startsWith("30")))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.race_WagerButton))
                .perform(ViewActions.click())

        Thread.sleep(2000)

        Espresso.onView(ViewMatchers.withId(R.id.main_WagerTextView))
                .check(matches(isDisplayed()))

        Espresso.onView(ViewMatchers.withId(R.id.main_WagerTextView))
                .check(matches(withText(startsWith("Time Left: 30"))))

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Transfer))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.transferHistory_TransferHistoryTextView))
                .check(matches(withText("Transfer History")))

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Quest))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.quest_rerollButton))
                .perform(ViewActions.click())

        Thread.sleep(1000)

        Espresso.onView(ViewMatchers.withId(R.id.quest_rerollButton))
                .check(matches(Matchers.not(isEnabled())))

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_SignOut))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onView(ViewMatchers.withId(R.id.go_to_login))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.login_email))
                .perform(ViewActions.replaceText("EspressoEmailFinal@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.login_password))
                .perform(ViewActions.replaceText("EspressoPasswordFinal"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_in_button))
                .perform(ViewActions.click())

        Thread.sleep(5000)

        Espresso.onView(ViewMatchers.withId(R.id.main_WagerTextView))
                .check(matches(isDisplayed()))

    }

}