package com.example.szymon.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_race.*

class RaceActivity : AppCompatActivity() {

    private var tag = "RaceActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private lateinit var amounts: List<String>
    private lateinit var times: List<String>
    private lateinit var rewards: List<Int>
    private var reward = 500
    private var amountIndex = 0
    private var timeIndex = 0

    private var wager = HashMap<String, Any>()
    private var wageredToday = false
    private var gold = 0.0
    private var collectedID: MutableList<String> = arrayListOf()

    private var dataRetrieved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race)

        //Initialising Firebase Instances
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        //Creating lists of possibilities of wagers
        amounts = listOf("5 Coinz", "10 Coinz", "15 Coinz", "20 Coinz", "25 Coinz", "30 Coinz", "35 Coinz", "40 Coinz", "45 Coinz", "50 Coinz")
        times = listOf("3 minutes", "6 minutes", "9 minutes", "12 minutes", "15 minutes", "18 minutes", "21 minutes", "24 minutes", "27 minutes", "30 minutes")
        rewards = listOf(50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 600, 800, 1000, 1250, 1500, 2000, 3000, 5000, 10000)
        reward = 500

        //Setting up amounts and times spinners
        race_AmountSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, amounts)
        race_TimeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, times)

        race_AmountSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(tag, "[AmountSpinner] $position")
                amountIndex = position
                updateWagerOffer()
            }
        }

        race_TimeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(tag, "[AmountSpinner] $position")
                timeIndex = position
                updateWagerOffer()
            }

        }

        //setting up WagerButton's onClickListener which starts the wager if possible
        race_WagerButton.setOnClickListener {

            if (reward > gold) {
                race_WagerTextView.text = getString(R.string.WagerNotEnoughGOLD)
            } else if (5*(amountIndex + 1) > 50 - collectedID.size) {
                race_WagerTextView.text = getString(R.string.WagerNotEnoughCoinz)
            } else {
                wageredToday = true
                wager["Amount"] = 5*(amountIndex + 1)
                wager["CompletionStage"] = 0
                wager["Time"] = 3*(timeIndex + 1)*60 + 5
                wager["Start"] = Timestamp.now().seconds
                wager["Reward"] = reward
                Log.d(tag, "[onWagerButtonClick] Amount: ${5*(amountIndex + 1)}, Time: ${3*(timeIndex + 1)*60 + 5}, Start: ${Timestamp.now().seconds}")
                setData()
            }
        }
    }

    //Updating TextView with the wager offer
    private fun updateWagerOffer() {
        Log.d(tag, "[updateWagerOffer] Updating wager offer. Data retrieved: $dataRetrieved")
        if (dataRetrieved) {
            if (wageredToday && wager.isEmpty()) {
                race_WagerTextView.text = getString(R.string.AlreadyWageredToday)
                race_WagerButton.isEnabled = false
            } else if (!wager.isEmpty()) {
                race_WagerTextView.text = getString(R.string.WagerHappeningNow)
                race_WagerButton.isEnabled = false
            } else {
                race_WagerTextView.text = String.format(getString(R.string.WagerOffer), rewards[9 + amountIndex - timeIndex])
                race_WagerButton.isEnabled = true
            }
            reward = rewards[9 + amountIndex - timeIndex]
        }
    }

    //retrieving data from Firestore
    private fun getData() {
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {
                    @Suppress("UNCHECKED_CAST")
                    wager = it.get("Wager") as HashMap<String, Any>
                    wageredToday = it.get("WageredToday") as Boolean
                    gold = it.get("GOLD").toString().toDouble()
                    @Suppress("UNCHECKED_CAST")
                    collectedID = it.get("CollectedID") as MutableList<String>
                    dataRetrieved = true
                    updateWagerOffer()
                    Log.d(tag, "[getData] Successfully retrieved data")
                }
                .addOnFailureListener {
                    Log.d(tag, "[getData] ${it.message.toString()}")
                    race_WagerButton.isEnabled = false
                    race_WagerTextView.text = getString(R.string.DownloadDataFail)
                    Toast.makeText(this, getString(R.string.DownloadDataFail), Toast.LENGTH_LONG).show()
                }
    }

    //updating data in Firestore and after it updates, moving to MainActivity
    private fun setData() {
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .update("Wager", wager,
                        "WageredToday", wageredToday)
                .addOnSuccessListener {
                    Log.d(tag, "[setData] Successfully added wager")
                    finish()
                }
                .addOnFailureListener {
                    Log.d(tag, "[setData] ${it.message.toString()}")
                    Toast.makeText(this, getString(R.string.UpdateDataFail), Toast.LENGTH_LONG).show()
                }
    }

    public override fun onStart() {
        super.onStart()
        Log.d(tag, "[onStart] Activity is onStart")
        race_WagerButton.isEnabled = false
        race_WagerTextView.text = getString(R.string.LoadingData)
        dataRetrieved = false
        getData()
    }

}
