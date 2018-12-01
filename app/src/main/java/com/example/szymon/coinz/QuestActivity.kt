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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_quest.*

class QuestActivity : AppCompatActivity() {

    private var tag = "QuestActivity"

    private var Rerolled = false
    private var Quests: MutableList<HashMap<String, Any>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)
    }

    private fun displayQuests() {
        quest_listView.adapter = QuestAdapter(this)
    }

    inner class QuestAdapter(context: Context): BaseAdapter(){

        private val mContext = context

        override fun getCount(): Int = Quests.size

        override fun getItem(position: Int): Any? = null

        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val questView = layoutInflater.inflate(R.layout.quest_questview, parent, false)

            questView.findViewById<TextView>(R.id.quest_quest).text = "Collect %s %s.".format(Quests[position]["Amount"].toString(), Quests[position]["Currency"].toString())
            questView.findViewById<TextView>(R.id.quest_reward).text = "Reward: %s GOLD.".format(Quests[position]["Reward"].toString())
            questView.findViewById<TextView>(R.id.quest_completionStage).text = "%s/%s".format(Quests[position]["CompletionStage"].toString(), Quests[position]["Amount"].toString())
            questView.findViewById<Button>(R.id.quest_rerollButton).setOnClickListener {
                rerollQuest(position)
            }
            if (Rerolled) {
                questView.findViewById<Button>(R.id.quest_rerollButton).isEnabled = false
            }

            return questView
        }
    }

    private fun getQuestData() {

        FirebaseFirestore.getInstance().collection("Coinz").document(FirebaseAuth.getInstance().currentUser?.email!!)
                .get()
                .addOnSuccessListener {
                    Rerolled = it.get("Rerolled") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    Quests = it.get("Quests") as MutableList<HashMap<String, Any>>
                    Log.d(tag, "[getQuestData] successfully retrieved Quests")
                    displayQuests()
                }
                .addOnFailureListener {
                    Log.d(tag, "[getQuestData] ${it.message.toString()}")
                }

    }

    private fun setQuestData() {
        FirebaseFirestore.getInstance().collection("Coinz").document(FirebaseAuth.getInstance().currentUser?.email!!)
                .update("Quests", Quests,
                        "Rerolled", Rerolled)
                .addOnSuccessListener {
                    Log.d(tag, "[setQuestData] Successfully updated Quests")
                    displayQuests()
                }
                .addOnFailureListener {
                    Log.d(tag, "[setQuestData] ${it.message.toString()}")
                }
    }

    private fun rerollQuest(index: Int) {
        val newQuest = HashMap<String, Any>()
        val Amount = (3..6).shuffled().first()
        val Currency = arrayListOf("QUID", "PENY", "DOLR", "SHIL").shuffled().first()
        val Reward = arrayListOf(100, 150, 200, 300)[Amount - 3]
        newQuest.put("Amount", Amount)
        newQuest.put("Currency", Currency)
        newQuest.put("Reward", Reward)
        newQuest.put("CompletionStage", 0)
        Quests.set(index, newQuest)
        Rerolled = true
        setQuestData()

    }

    public override fun onStart() {
        super.onStart()
        getQuestData()
    }


}
