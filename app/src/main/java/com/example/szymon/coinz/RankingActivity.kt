package com.example.szymon.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_ranking.*

class RankingActivity : AppCompatActivity() {

    private var tag = "RankingActivity"

    private var userData: MutableList<HashMap<String, Any>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)
        val userDataDescription = HashMap<String, Any>()
        userDataDescription["Username"] = "Username"
        userDataDescription["Gold"] = "GOLD"
        userData.add(userDataDescription)

        //displayRanking()



    }


    private fun displayRanking() {
        ranking_listView.adapter = RankingAdapter(this)
    }

    inner class RankingAdapter(context: Context): BaseAdapter(){

        private val mContext = context

        override fun getCount(): Int = userData.size

        override fun getItem(position: Int): Any? = null

        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val rankingView = layoutInflater.inflate(R.layout.ranking_rankview, parent, false)

            if (position != 0) {
                rankingView.findViewById<TextView>(R.id.ranking_username).text = "%s. %s".format(position.toString(), userData[position]["Username"].toString())
                rankingView.findViewById<TextView>(R.id.ranking_gold).text = "%.1f".format(userData[position]["Gold"].toString().toDouble())
            } else {
                rankingView.findViewById<TextView>(R.id.ranking_username).text = userData[position]["Username"].toString()
                rankingView.findViewById<TextView>(R.id.ranking_gold).text = userData[position]["Gold"].toString()
            }

         return rankingView

        }


    }

    public override fun onStart() {
        super.onStart()

        FirebaseFirestore.getInstance().collection("Coinz")
                .get()
                .addOnSuccessListener {
                    val userDataTempList: MutableList<HashMap<String, Any>> = arrayListOf()
                    for (document in it.documents) {
                        Log.d(tag, "[onStart] ${document.get("Username")}, ${document.get("GOLD")}")
                        val userDataTemp = HashMap<String, Any>()
                        userDataTemp.put("Username", document.get("Username")!!)
                        userDataTemp.put("Gold", document.get("GOLD")!!)
                        userDataTempList.add(userDataTemp)
                    }
                    userDataTempList.sortByDescending{it["Gold"].toString().toDouble()}
                    userData.addAll(userDataTempList)
                    displayRanking()
                }
    }


}
