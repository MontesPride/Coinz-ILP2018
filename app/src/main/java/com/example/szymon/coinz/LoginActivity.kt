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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var gso: GoogleSignInOptions
    private val RC_SIGN_IN: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.
        login_password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        //Going back to sign up activity
        go_back_to_signup.setOnClickListener { finish() }

        //Initialising instances of Firebase
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        //Attempt to login with email and password
        email_sign_in_button.setOnClickListener { attemptLogin() }

        //Logging in with Google
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        google_sign_in_button.setOnClickListener { googleSignIn() }
    }

    //Attempt to Sign in with email and password
    private fun attemptLogin() {

        // Reset errors.
        login_email.error = null
        login_password.error = null

        // Store values at the time of the login attempt.
        val emailStr = login_email.text.toString()
        val passwordStr = login_password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(passwordStr) || !isPasswordValid(passwordStr)) {
            login_password.error = getString(R.string.error_invalid_password)
            focusView = login_password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            login_email.error = getString(R.string.error_field_required)
            focusView = login_email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            login_email.error = getString(R.string.error_invalid_email)
            focusView = login_email
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


            //Signing in with email and password to Firebase
            mAuth.signInWithEmailAndPassword(emailStr, passwordStr)
                    .addOnCompleteListener {
                        showProgress(false)
                        if(!it.isSuccessful) {
                            Log.d(tag, "Unsuccessful log in")
                            //Unsuccessful Log in, display some toasts
                            when (it.exception?.message.toString()) {
                                getString(R.string.error_firebase_no_network_connection) -> Toast.makeText(this, getString(R.string.error_no_network_connection), Toast.LENGTH_LONG).show()
                                getString(R.string.error_firebase_incorrect_email) -> {
                                    login_email.error = getString(R.string.error_incorrect_email)
                                    login_email.requestFocus()
                                }
                                getString(R.string.error_firebase_incorrect_password) -> {
                                    login_password.error = getString(R.string.error_incorrect_password)
                                    login_password.requestFocus()
                                }
                                else -> Toast.makeText(this, getString(R.string.error_something_else_wrong), Toast.LENGTH_LONG).show()
                            }
                            return@addOnCompleteListener
                        } else {
                            //Successful Log in, go to MainActivity
                            Log.d(tag, "Successfully logged in user with uid: ${it.result?.user?.uid}")
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        }
                    }
                    .addOnFailureListener {
                        showProgress(false)
                        Log.d(tag, "Failed to log in user: ${it.message}")
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

    //If everything ok, log in to Firebase
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

    //Signing in to Firebase with Google idToken
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
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                } else {

                                    val userData = createUserDocument(mAuth.currentUser?.displayName!!)

                                    mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                Log.d(tag, "[firebaseAuthWithGoogle] Successfully added document to Firestore")
                                                showProgress(false)
                                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                            }
                                            .addOnFailureListener { e ->
                                                showProgress(false)
                                                Log.d(tag, "[firebaseAuthWithGoogle] ${e.message.toString()}")
                                            }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.d(tag, "[firebaseAuthWithGoogle] Unable to retrieve data, ${e.message.toString()}")
                            }
                }
                .addOnFailureListener {
                    showProgress(false)
                    Log.w(tag, "[firebaseAuthWithGoogle] signInWithCredential:failure, ${it.message.toString()}")
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

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
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

}
