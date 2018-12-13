package com.example.szymon.coinz

import android.graphics.Typeface
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.widget.GridView
import android.widget.ListView
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@LargeTest
@RunWith(AndroidJUnit4::class)
class FinalFullTest {

    //THIS IS A COMPLETE TEST OF THE APP, UNFORTUNATELY IT REQUIRES THAT THE EspressoEmailFinal1@gmail.com account is deleted.
    //I WASN'T ABLE TO DELETE THE ACCOUNT ON EACH START WITH CODE SO IT NEEDS TO BE DONE MANUALLY ON FIREBASE
    //OTHER THAN THAT, TEST IS REPRODUCIBLE

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SignUpActivity::class.java)

    @Rule
    @JvmField
    var mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule
            .grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    fun withBoldStyle(): Matcher<View> {
        return object: TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Text should be bold")
            }

            override fun matchesSafely(item: View): Boolean {
                return (item as TextView).typeface == Typeface.DEFAULT_BOLD
            }

        }
    }

    private fun withGridSize(size: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            public override fun matchesSafely(view: View): Boolean {
                return (view as GridView).count >= size
            }
            override fun describeTo(description: Description) {
                description.appendText("ListView should have at least $size items")
            }
        }
    }

    private fun withListSize(size: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            public override fun matchesSafely(view: View): Boolean {
                return (view as ListView).count >= size
            }
            override fun describeTo(description: Description) {
                description.appendText("ListView should have at least $size items")
            }
        }
    }

    private fun createUserDocument(username: String): HashMap<String, Any> {

        val userData = HashMap<String, Any>()
        userData["GOLD"] = 0
        userData["CollectedCoinz"] = listOf<HashMap<String, Any>>()
        userData["CollectedID"] = listOf<String>()
        userData["LastDate"] = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        userData["LastTimestamp"] = Timestamp.now().seconds
        userData["CoinzExchanged"] = 0
        userData["CoinzReceived"] = 0
        userData["Username"] = username
        userData["Rerolled"] = false
        userData["TransferHistory"] = listOf<HashMap<String, Any>>()
        userData["AllCollectedToday"] = false

        val amount = (3..6).shuffled().first()
        val currency = arrayListOf("QUID", "PENY", "DOLR", "SHIL").shuffled().first()
        val reward = arrayListOf(100, 150, 200, 300)[amount - 3]
        val quests: MutableList<HashMap<String, Any>> = arrayListOf()
        val quest = HashMap<String, Any>()
        quest["Amount"] = amount
        quest["Currency"] = currency
        quest["Reward"] = reward
        quest["CompletionStage"] = 0
        quests.add(quest)

        userData["Quests"] = quests
        userData["Wager"] = HashMap<String, Any>()
        userData["WageredToday"] = false

        return userData
    }

    @Before
    fun beforeTest() {

        val espressoUser2 = createUserDocument("EspressoUsernameFinal2")
        espressoUser2["CoinzExchanged"] = 22
        espressoUser2["GOLD"] = 123.4
        val collectedCoinz1: MutableList<HashMap<String, Any>> = arrayListOf()
        for (i in (0..5)) {
            val currencies = listOf("QUID", "PENY", "DOLR", "SHIL", "QUID", "PENY")
            val coin = HashMap<String, Any>()
            coin["Currency"] = currencies[i]
            coin["Value"] = (i+0.25)*2
            coin["Time"] = Timestamp.now().seconds
            collectedCoinz1.add(coin)
        }
        espressoUser2["CollectedCoinz"] = collectedCoinz1
        FirebaseAuth.getInstance().createUserWithEmailAndPassword("espressoemailfinal2@gmail.com", "EspressoPasswordFinal2")
                .addOnSuccessListener {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName("EspressoUsernameFinal2")
                            .build()
                    FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)
                }
        FirebaseFirestore.getInstance().collection("Coinz").document("espressoemailfinal2@gmail.com")
                .set(espressoUser2)

        val espressoUser3 = createUserDocument("EspressoUsernameFinal3")
        espressoUser3["CoinzReceived"] = 23
        espressoUser3["GOLD"] = 567.8
        val transfer = HashMap<String, Any>()
        transfer["Amount"] = 999.9
        transfer["From"] = "Admin"
        val transfers = arrayListOf(transfer)
        espressoUser3["TransferHistory"] = transfers
        FirebaseAuth.getInstance().createUserWithEmailAndPassword("espressoemailfinal3@gmail.com", "EspressoPasswordFinal3")
                .addOnSuccessListener {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName("EspressoUsernameFinal3")
                            .build()
                    FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)
                }
        FirebaseFirestore.getInstance().collection("Coinz").document("espressoemailfinal3@gmail.com")
                .set(espressoUser3)
    }

    @Test
    fun espressoFinalTest() {

        Espresso.onView(ViewMatchers.withId(R.id.signup_username))
                .perform(ViewActions.replaceText("EspressoUsernameFinal1"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_password))
                .perform(ViewActions.replaceText("EspressoPasswordFinal1"))

        Espresso.onView(ViewMatchers.withId(R.id.signup_email))
                .perform(ViewActions.replaceText("EspressoEmailFinal1@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_up_button))
                .perform(ViewActions.click())

        Thread.sleep(15000)

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_SignOut))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onView(ViewMatchers.withId(R.id.go_to_login))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.login_email))
                .perform(ViewActions.replaceText("EspressoEmailFinal1@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.login_password))
                .perform(ViewActions.replaceText("EspressoPasswordFinal1"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_in_button))
                .perform(ViewActions.click())

        Thread.sleep(5000)

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_SignOut))
                .perform(ViewActions.click())

        Thread.sleep(500)
        //Register and login test done.
        //Starting test of all activities, exchanging, transfering, setting up wager, and rerolling quest

        Espresso.onView(ViewMatchers.withId(R.id.login_email))
                .perform(ViewActions.replaceText("EspressoEmailFinal2@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.login_password))
                .perform(ViewActions.replaceText("EspressoPasswordFinal2"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_in_button))
                .perform(ViewActions.click())

        Thread.sleep(5000)


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

        Espresso.onView(withId(R.id.bank_GOLDvalue))
                .check(matches(not(withText("0.0"))))

        Espresso.onView(ViewMatchers.withId(R.id.bank_GOLDvalue))
                .check(matches(withText("123.4")))

        Thread.sleep(500)

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Thread.sleep(2000)

        Espresso.onView(ViewMatchers.withId(R.id.bank_GOLDvalue))
                .check(matches(not(withText("123.4"))))

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onView(withId(R.id.bank_exchangedTextView))
                .check(matches(withText(containsString("back tomorrow to exchange"))))

        Espresso.onView(withId(R.id.bank_coinzGridView))
                .check(matches(withGridSize(3)))

        Espresso.onView(withId(R.id.bank_transferButton))
                .perform(ViewActions.click())

        Espresso.onView(withId(R.id.bank_transferEmail))
                .perform(ViewActions.replaceText("EspressoEmailFinal2@gmail.com"))

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.bank_transferEmail))
                .check(matches(hasErrorText("You cannot transfer Coinz to yourself")))

        Espresso.onView(withId(R.id.bank_transferEmail))
                .perform(ViewActions.replaceText("JustTesting"))

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.bank_transferEmail))
                .check(matches(hasErrorText("This email address is invalid")))

        Espresso.onView(withId(R.id.bank_transferEmail))
                .perform(ViewActions.replaceText("EspressoEmailFinal3@gmail.com"))

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.bank_coinzGridView))
                .atPosition(0)
                .onChildView(withId(R.id.coinExchange))
                .perform(ViewActions.click())

        Thread.sleep(5000)

        Espresso.onView(withId(R.id.bank_coinzGridView))
                .check(matches(withGridSize(1)))

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
                .check(matches(withText(startsWith("Time Left:"))))

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

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.quest_listView))
                .atPosition(0)
                .onChildView(withId(R.id.quest_rerollButton))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.quest_listView))
                .atPosition(0)
                .onChildView(withId(R.id.quest_rerollButton))
                .check(matches(Matchers.not(isEnabled())))

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_SignOut))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onView(ViewMatchers.withId(R.id.login_email))
                .perform(ViewActions.replaceText("EspressoEmailFinal2@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.login_password))
                .perform(ViewActions.replaceText("EspressoPasswordFinal2"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_in_button))
                .perform(ViewActions.click())

        Thread.sleep(5000)

        Espresso.onView(ViewMatchers.withId(R.id.main_WagerTextView))
                .check(matches(isDisplayed()))

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_SignOut))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onView(ViewMatchers.withId(R.id.login_email))
                .perform(ViewActions.replaceText("EspressoEmailFinal3@gmail.com"))

        Espresso.onView(ViewMatchers.withId(R.id.login_password))
                .perform(ViewActions.replaceText("EspressoPasswordFinal3"))

        Espresso.onView(ViewMatchers.withId(R.id.email_sign_in_button))
                .perform(ViewActions.click())

        Thread.sleep(5000)

        Espresso.onView(ViewMatchers.withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.main_Transfer))
                .perform(ViewActions.click())

        Thread.sleep(500)

        Espresso.onView(ViewMatchers.withId(R.id.transferHistory_listView))
                .check(matches(withListSize(2)))

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.transferHistory_listView))
                .atPosition(1)
                .onChildView(withId(R.id.transferHistory_DeleteButton))
                .perform(ViewActions.click())

        Thread.sleep(250)

        Espresso.onView(ViewMatchers.withId(R.id.transferHistory_listView))
                .check(matches(withListSize(1)))

        Espresso.onData(anything())
                .inAdapterView(withId(R.id.transferHistory_listView))
                .atPosition(0)
                .onChildView(withId(R.id.transferHistory_DeleteAllButton))
                .perform(ViewActions.click())

        Thread.sleep(250)

        Espresso.onView(ViewMatchers.withId(R.id.transferHistory_listView))
                .check(matches(withListSize(0)))

        Espresso.pressBack()

        Espresso.onView(withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(withId(R.id.main_Bank))
                .perform(ViewActions.click())

        Espresso.onView(withId(R.id.bank_GOLDvalue))
                .check(matches(not(withText("567.8"))))

        Espresso.onView(withId(R.id.bank_GOLDvalue))
                .check(matches(not(withText("0.0"))))

        Espresso.pressBack()

        Espresso.onView(withId(R.id.main_OpenMenu))
                .perform(ViewActions.click())

        Espresso.onView(withId(R.id.main_SignOut))
                .perform(ViewActions.click())

        Thread.sleep(2500)


    }

}