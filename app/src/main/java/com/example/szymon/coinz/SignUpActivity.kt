package com.example.szymon.coinz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView

import android.Manifest.permission.READ_CONTACTS
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
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

/**
 * A login screen that offers login via email/password.
 */
class SignUpActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    private val tag = "SignUpActivity"

    private var mAuthTask: UserLoginTask? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var gso: GoogleSignInOptions
    private val RC_SIGN_IN: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Set up the login form.
        populateAutoComplete()
        signup_password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        go_to_login.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        email_sign_up_button.setOnClickListener { attemptLogin() }

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)


        google_sign_up_button.setOnClickListener { googleSignIn() }



    }

    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        loaderManager.initLoader(0, null, this)
    }

    private fun mayRequestContacts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(signup_email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            {requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)})
        } else {
            requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
        }
        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

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
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            //mAuthTask = UserLoginTask(emailStr, passwordStr)
            //mAuthTask!!.execute(null as Void?)
            Log.d(tag, "[attemptLogin] email: $emailStr")
            mAuth?.createUserWithEmailAndPassword(emailStr, passwordStr)
                    ?.addOnCompleteListener {

                        if (!it.isSuccessful) {

                            showProgress(false)

                            when (it.exception?.message) {
                                getString(R.string.error_firebase_no_network_connection) -> Toast.makeText(this, getString(R.string.error_no_network_connection), Toast.LENGTH_LONG).show()
                                "Failed to create user: A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> Toast.makeText(this, getString(R.string.error_no_network_connection), Toast.LENGTH_LONG).show()
                                else -> Toast.makeText(this, getString(R.string.error_something_else_wrong), Toast.LENGTH_LONG).show()
                            }
                            return@addOnCompleteListener
                        } else {
                            Log.d(tag, "Successfully created user with uid: ${it.result?.user?.uid}")

                            val user = mAuth.currentUser

                            val userData = createUserDocument(usernameStr)

                            mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Log.d(tag, "[addData] Successfully added data to Firestore")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d(tag, "[addData] ${e.message.toString()}")
                                    }

                            val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(usernameStr)
                                    .build()

                            user?.updateProfile(profileUpdates)
                                    ?.addOnCompleteListener { task ->
                                        showProgress(false)
                                        if (task.isSuccessful) {
                                            Log.d(tag, "User profile updated.")
                                        }
                                        startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                                    }

                        }
                    }
                    ?.addOnFailureListener {
                        showProgress(false)
                        Log.d(tag, "Failed to create user: ${it.message}")
                    }
        }
    }

    private fun googleSignIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

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

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(tag, "[firebaseAuthWithGoogle] Going to authenticate")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        Log.d(tag, "[firebaseAuthWithGoogle] Going to authenticate2")
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener {
                    Log.d(tag, "[firebaseAuthWithGoogle] signInWithCredential:success")
                    Log.d(tag, mAuth.currentUser?.email.toString())

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
                                                Log.d(tag, "[addData] Successfully added data to Firestore")
                                                showProgress(false)
                                                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                                            }
                                            .addOnFailureListener { e ->
                                                showProgress(false)
                                                Log.d(tag, "[addData] ${e.message.toString()}")
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
        val Amount = (3..6).shuffled().first()
        val Currency = arrayListOf("QUID", "PENY", "DOLR", "SHIL").shuffled().first()
        val Reward = arrayListOf(100, 150, 200, 300)[Amount - 3]
        val Quests: MutableList<HashMap<String, Any>> = arrayListOf()
        val Quest = HashMap<String, Any>()
        Quest["Amount"] = Amount
        Quest["Currency"] = Currency
        Quest["Reward"] = Reward
        Quest["CompletionStage"] = 0
        Quests.add(Quest)
        userData["Quests"] = Quests
        userData["Wager"] = HashMap<String, Any>()
        userData["WageredToday"] = false

        return userData
    }

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

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 6
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
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
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            signup_progress.visibility = if (show) View.VISIBLE else View.GONE
            signup_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this@SignUpActivity,
                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        signup_email.setAdapter(adapter)
    }

    object ProfileQuery {
        val PROJECTION = arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
        val ADDRESS = 0
        val IS_PRIMARY = 1
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask internal constructor(private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean? {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }

            return DUMMY_CREDENTIALS
                    .map { it.split(":") }
                    .firstOrNull { it[0] == mEmail }
                    ?.let {
                        // Account exists, return true if the password matches.
                        it[1] == mPassword
                    }
                    ?: true
        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
            } else {
                signup_password.error = getString(R.string.error_incorrect_password)
                signup_password.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }

    public override fun onStart() {
        super.onStart()
        if (mAuth.currentUser?.uid != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        //updateUI(currentUser)
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

    companion object {

        /**
         * Id to identity READ_CONTACTS permission request.
         */
        private val REQUEST_READ_CONTACTS = 0

        /**
         * A dummy authentication store containing known user names and passwords.
         * TODO: remove after connecting to a real authentication system.
         */
        private val DUMMY_CREDENTIALS = arrayOf("foo@example.com:hello", "bar@example.com:world")
    }
}
