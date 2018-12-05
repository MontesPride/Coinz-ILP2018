package com.example.szymon.coinz

import android.content.Context
import android.graphics.Typeface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_ranking.*

class RankingActivity : AppCompatActivity() {

    private var tag = "RankingActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private var userData: MutableList<HashMap<String, Any>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        //Initialising Firebase Instances
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        //Adding first rows of ListView to the userData
        val userDataDescription = HashMap<String, Any>()
        userDataDescription["Username"] = "Username"
        userDataDescription["Gold"] = "GOLD"
        userData.add(userDataDescription)

    }

    //function that displays ListView of players
    private fun displayRanking() {

        if (userData.size <= 0) {
            ranking_noPlayers.text = getString(R.string.RankingNoPlayers)
            ranking_noPlayers.visibility = View.VISIBLE
        } else {
            ranking_listView.adapter = RankingAdapter(this)
            ranking_noPlayers.visibility = View.GONE
        }
    }

    //Adapter for ListView of players
    inner class RankingAdapter(context: Context): BaseAdapter(){

        private val mContext = context

        override fun getCount(): Int = userData.size

        override fun getItem(position: Int): Any? = null

        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val rankingView = layoutInflater.inflate(R.layout.ranking_rankview, parent, false)

            //just displaying information on the screen
            if (position != 0) {
                rankingView.findViewById<TextView>(R.id.ranking_username).text = "%s. %s".format(position.toString(), userData[position]["Username"].toString())
                rankingView.findViewById<TextView>(R.id.ranking_gold).text = "%.1f".format(userData[position]["Gold"].toString().toDouble())

                if (userData[position]["ID"] == mAuth.currentUser?.email) {
                    rankingView.findViewById<TextView>(R.id.ranking_username).typeface = Typeface.DEFAULT_BOLD
                    rankingView.findViewById<TextView>(R.id.ranking_gold).typeface = Typeface.DEFAULT_BOLD
                }

            } else {
                rankingView.findViewById<TextView>(R.id.ranking_username).text = userData[position]["Username"].toString()
                rankingView.findViewById<TextView>(R.id.ranking_gold).text = userData[position]["Gold"].toString()
            }

         return rankingView

        }


    }

    public override fun onStart() {
        super.onStart()
        //retrieving players data from the Firestore
        mStore.collection("Coinz")
                .get()
                .addOnSuccessListener { data ->
                    Log.d(tag, "[onStart] Successfully retrieved data")
                    val userDataTempList: MutableList<HashMap<String, Any>> = arrayListOf()
                    for (document in data.documents) {
                        Log.d(tag, "[onStart] ${document.get("Username")}, ${document.get("GOLD")}")
                        val userDataTemp = HashMap<String, Any>()
                        userDataTemp["Username"] = document.get("Username")!!
                        userDataTemp["Gold"] = document.get("GOLD")!!
                        userDataTemp["ID"] = document.id
                        userDataTempList.add(userDataTemp)
                    }
                    userDataTempList.sortByDescending{it["Gold"].toString().toDouble()}
                    userData.addAll(userDataTempList)
                    displayRanking()
                }
                .addOnFailureListener {
                    Log.d(tag, "[onStart] ${it.message.toString()}")
                    displayRanking()
                }
    }


}
