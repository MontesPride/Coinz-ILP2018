package com.example.szymon.coinz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.support.v7.app.AppCompatActivity
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.android.synthetic.main.activity_sign_up.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.collections.HashMap

/**
 * A login screen that offers login via email/password.
 */
class SignUpActivity : AppCompatActivity() {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    private val tag = "SignUpActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var gso: GoogleSignInOptions
    private val RC_SIGN_IN: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Set up the login form.
        signup_password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        //Going to LoginActivity
        go_to_login.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        //Initialising instances of Firebase
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        //Attempt to register with email and password
        email_sign_up_button.setOnClickListener { attemptLogin() }

        //Register with Google
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        google_sign_up_button.setOnClickListener { googleSignIn() }



    }

    private fun attemptLogin() {

        // Reset errors.
        signup_email.error = null
        signup_password.error = null
        signup_username.error = null

        // Store values at the time of the login attempt.
        val emailStr = signup_email.text.toString()
        val passwordStr = signup_password.text.toString()
        val usernameStr = signup_username.text.toString()

        var cancel = false
        var focusView: View? = null
        //var success = false

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(passwordStr) || !isPasswordValid(passwordStr)) {
            signup_password.error = getString(R.string.error_invalid_password)
            focusView = signup_password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            signup_email.error = getString(R.string.error_field_required)
            focusView = signup_email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            signup_email.error = getString(R.string.error_invalid_email)
            focusView = signup_email
            cancel = true
        }

        // Check for a valid username
        if (TextUtils.isEmpty(usernameStr)) {
            signup_username.error = getString(R.string.error_invalid_username)
            focusView = signup_username
            cancel = true
        }  else if (usernameStr.length > 30) {
            signup_username.error = getString(R.string.error_too_long_username)
            focusView = signup_username
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)

            Log.d(tag, "[attemptLogin] Creating new user")
            mAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                    .addOnCompleteListener {

                        if (!it.isSuccessful) {
                            showProgress(false)
                            Log.d(tag, "${getString(R.string.error_firebase_user_already_exists) == it.exception?.message.toString()}")
                            Log.d(tag, "${it.exception?.message}")
                            //Unsuccessful Sign Up, display some toasts
                            when (it.exception?.message.toString()) {
                                getString(R.string.error_firebase_no_network_connection) -> Toast.makeText(this, getString(R.string.error_no_network_connection), Toast.LENGTH_LONG).show()
                                getString(R.string.error_firebase_user_already_exists) -> Toast.makeText(this, getString(R.string.action_user_already_exists), Toast.LENGTH_LONG).show()
                                else -> Toast.makeText(this, getString(R.string.error_something_else_wrong), Toast.LENGTH_LONG).show()
                            }
                            return@addOnCompleteListener
                        } else {
                            //Successful Sign Up, create empty document for a new user and go to MainActivity if successful
                            Log.d(tag, "[attemptLogin] Successfully created user with uid: ${it.result?.user?.uid}")
                            val user = mAuth.currentUser
                            val userData = createUserDocument(usernameStr)

                            mStore.collection("Coinz").document(user?.email!!)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Log.d(tag, "[attemptLogin] Successfully added data to Firestore")

                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(usernameStr)
                                                .build()

                                        user.updateProfile(profileUpdates)
                                                .addOnCompleteListener { task ->
                                                    showProgress(false)
                                                    if (task.isSuccessful) {
                                                        Log.d(tag, "[attemptLogin] User profile updated.")
                                                    }
                                                    startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                                                }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d(tag, "[attemptLogin] ${e.message.toString()}")
                                    }
                        }
                    }
                    .addOnFailureListener {
                        showProgress(false)
                        Log.d(tag, "[attemptLogin] Failed to create user: ${it.message}")
                    }
        }
    }

    //Signing in to Google
    private fun googleSignIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    //Retrieving result of signing in to Google
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    //If everything ok, sign up to Firebase
    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(tag, "[handleResult] Name: ${account?.displayName}, Email: ${account?.email}")
            showProgress(true)
            firebaseAuthWithGoogle(account!!)
        }catch (e: ApiException) {
            Log.d(tag, "[handleResult] ${e.message.toString()}")
        }
    }

    //Signing up to Firebase with Google idToken
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener {
                    Log.d(tag, "[firebaseAuthWithGoogle] signInWithCredential:success")

                    //Check if there is a document in the Firestore corresponding to the given user, if not, create it and then move on to MainActivity
                    mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                            .get()
                            .addOnSuccessListener { document ->
                                if(document.exists()) {
                                    showProgress(false)
                                    startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                                } else {
                                    val userData = createUserDocument(mAuth.currentUser?.displayName!!)

                                    mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                Log.d(tag, "[firebaseAuthWithGoogle] Successfully added data to Firestore")
                                                showProgress(false)
                                                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                                            }
                                            .addOnFailureListener { e ->
                                                showProgress(false)
                                                Log.d(tag, "[firebaseAuthWithGoogle] ${e.message.toString()}")
                                            }
                                }
                            }
                            .addOnFailureListener {
                                Log.d(tag, "[firebaseAuthWithGoogle] Unable to retrieve data")
                            }
                }
                .addOnFailureListener {
                    showProgress(false)
                    Log.w(tag, "[firebaseAuthWithGoogle]signInWithCredential:failure, ${it.message.toString()}")
                }
    }

    //Generating new document for a ner user
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

    //Email validation function
    private fun isEmailValid(email: String): Boolean {
        return Pattern.compile(
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
                        + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
                        + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
        ).matcher(email).matches()
    }

    //Password validation function
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 6
    }

    //This code was generated by Android Studio
    //I like it so I decided to leave it
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            signup_form.visibility = if (show) View.GONE else View.VISIBLE
            signup_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            signup_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            signup_progress.visibility = if (show) View.VISIBLE else View.GONE
            signup_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            signup_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            signup_progress.visibility = if (show) View.VISIBLE else View.GONE
            signup_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }



    public override fun onStart() {
        super.onStart()
        if (mAuth.currentUser?.uid != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    public override fun onResume() {
        super.onResume()
        if (mAuth.currentUser?.uid != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }


}
