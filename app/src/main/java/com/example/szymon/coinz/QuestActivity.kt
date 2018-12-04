package com.example.szymon.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_quest.*

class QuestActivity : AppCompatActivity() {

    private var tag = "QuestActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private var rerolled = false
    private var quests: MutableList<HashMap<String, Any>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)

        //Initialising Firebase instances
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()
    }

    //displaying ListView with quests
    private fun displayQuests() {
        if (quests.size <= 0) {
            quest_noNewQuestsTextView.visibility = View.VISIBLE
            quest_noNewQuestsTextView.text = getString(R.string.NoDailyQuests)
        } else {
            quest_noNewQuestsTextView.visibility = View.GONE
            quest_listView.adapter = QuestAdapter(this)
        }

    }

    //ListView adapter for Quests
    inner class QuestAdapter(context: Context): BaseAdapter(){

        private val mContext = context

        override fun getCount(): Int = quests.size

        override fun getItem(position: Int): Any? = null

        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val questView = layoutInflater.inflate(R.layout.quest_questview, parent, false)

            //displaying information in Textviews and setting up button's onClickListeners
            questView.findViewById<TextView>(R.id.quest_quest).text = "Collect %s %s.".format(quests[position]["Amount"].toString(), quests[position]["Currency"].toString())
            questView.findViewById<TextView>(R.id.quest_reward).text = "Reward: %s GOLD.".format(quests[position]["Reward"].toString())
            questView.findViewById<TextView>(R.id.quest_completionStage).text = "%s/%s".format(quests[position]["CompletionStage"].toString(), quests[position]["Amount"].toString())
            questView.findViewById<Button>(R.id.quest_rerollButton).setOnClickListener {
                rerollQuest(position)
            }
            if (rerolled) {
                questView.findViewById<Button>(R.id.quest_rerollButton).isEnabled = false
            }

            return questView
        }
    }

    //retrieving quest data from Firestore
    private fun getQuestData() {

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {
                    rerolled = it.get("Rerolled") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    quests = it.get("Quests") as MutableList<HashMap<String, Any>>
                    Log.d(tag, "[getQuestData] successfully retrieved Quests")
                    displayQuests()
                }
                .addOnFailureListener {
                    Log.d(tag, "[getQuestData] ${it.message.toString()}")
                    quest_noNewQuestsTextView.text = getString(R.string.DailyQuestsFailDownload)
                    quest_noNewQuestsTextView.visibility = View.VISIBLE
                }

    }

    //updating quest data in Firestore
    private fun setQuestData() {
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .update("Quests", quests,
                        "Rerolled", rerolled)
                .addOnSuccessListener {
                    Log.d(tag, "[setQuestData] Successfully updated Quests")
                    displayQuests()
                }
                .addOnFailureListener {
                    Log.d(tag, "[setQuestData] ${it.message.toString()}")
                    Toast.makeText(this, getString(R.string.DailyQuestsFailUpdate), Toast.LENGTH_LONG).show()
                }
    }

    //Function that rerolls a quest for a new one.
    private fun rerollQuest(index: Int) {
        val newQuest = HashMap<String, Any>()
        val amount = (3..6).shuffled().first()
        val currency = arrayListOf("QUID", "PENY", "DOLR", "SHIL").shuffled().first()
        val reward = arrayListOf(100, 150, 200, 300)[amount - 3]
        newQuest["Amount"] = amount
        newQuest["Currency"] = currency
        newQuest["Reward"] = reward
        newQuest["CompletionStage"] = 0
        quests[index] = newQuest
        rerolled = true
        setQuestData()
    }

    public override fun onStart() {
        super.onStart()
        getQuestData()
    }


}
