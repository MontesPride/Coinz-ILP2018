package com.example.szymon.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_race.*

class RaceActivity : AppCompatActivity() {

    private var tag = "RaceActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private lateinit var Amounts: List<String>
    private lateinit var Times: List<String>
    private lateinit var Rewards: List<Int>
    private var Reward = 500
    private var AmountIndex = 0
    private var TimeIndex = 0

    private var Wager = HashMap<String, Any>()
    private var WageredToday = false
    private var GOLD = 0.0
    private var CollectedID: MutableList<String> = arrayListOf()

    //private var Amount = HashMap<Int, String>()
    //private var Time = HashMap<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race)

        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        Amounts = listOf("5 Coinz", "10 Coinz", "15 Coinz", "20 Coinz", "25 Coinz", "30 Coinz", "35 Coinz", "40 Coinz", "45 Coinz", "50 Coinz")
        Times = listOf("3 minutes", "6 minutes", "9 minutes", "12 minutes", "15 minutes", "18 minutes", "21 minutes", "24 minutes", "27 minutes", "30 minutes")
        Rewards = listOf(50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 600, 800, 1000, 1250, 1500, 2000, 3000, 5000, 10000)
        Reward = 500

        race_AmountSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, Amounts)
        race_TimeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, Times)

        race_AmountSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(tag, "[AmountSpinner] $position")
                AmountIndex = position
                updateWagerOffer()
            }
        }

        race_TimeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(tag, "[AmountSpinner] $position")
                TimeIndex = position
                updateWagerOffer()
            }

        }

        race_WagerButton.setOnClickListener {
            if (Reward > GOLD) {
                race_WagerTextView.text = getString(R.string.WagerNotEnoughGOLD)
            } else if (5*(AmountIndex + 1) > 50 - CollectedID.size) {
                race_WagerTextView.text = getString(R.string.WagerNotEnoughCoinz)
            } else {
                WageredToday = true
                Wager["Amount"] = 5*(AmountIndex + 1)
                Wager["CompletionStage"] = 0
                Wager["Time"] = 3*(TimeIndex + 1)*60 + 5
                Wager["Start"] = Timestamp.now().seconds
                Wager["Reward"] = Reward
                Log.d(tag, "[onWagerButtonClick] Amount: ${5*(AmountIndex + 1)}, Time: ${3*(TimeIndex + 1)*60 + 5}, Start: ${Timestamp.now().seconds}")
                setData()
            }
        }
    }

    private fun updateWagerOffer() {

        if (WageredToday && Wager.isEmpty()) {
            race_WagerTextView.text = getString(R.string.AlreadyWageredToday)
            race_WagerButton.isEnabled = false
        } else if (!Wager.isEmpty()) {
            race_WagerTextView.text = getString(R.string.WagerHappeningNow)
            race_WagerButton.isEnabled = false
        } else {
            race_WagerTextView.text = String.format(getString(R.string.WagerOffer), Rewards[9 + AmountIndex - TimeIndex])
            race_WagerButton.isEnabled = true
        }
        Reward = Rewards[9 + AmountIndex - TimeIndex]
        Log.d(tag, "[updateWagerOffer] $Reward")
    }

    private fun getData() {
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {
                    @Suppress("UNCHECKED_CAST")
                    Wager = it.get("Wager") as HashMap<String, Any>
                    WageredToday = it.get("WageredToday") as Boolean
                    GOLD = it.get("GOLD").toString().toDouble()
                    @Suppress("UNCHECKED_CAST")
                    CollectedID = it.get("CollectedID") as MutableList<String>
                    updateWagerOffer()
                }
                .addOnFailureListener {

                }
    }

    private fun setData() {
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .update("Wager", Wager,
                        "WageredToday", WageredToday)
                .addOnSuccessListener {
                    Log.d(tag, "[setData] Successfully added wager")
                    finish()
                    //updateWagerOffer()
                }
                .addOnFailureListener {
                    Log.d(tag, "[setData] ${it.message.toString()}")
                }
    }

    public override fun onStart() {
        super.onStart()
        getData()
    }

}
