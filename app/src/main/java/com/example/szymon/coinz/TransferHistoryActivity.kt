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
import com.google.firebase.auth.FirebaseAuthSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_transfer_history.*

class TransferHistoryActivity : AppCompatActivity() {

    private var tag = "TransferHistoryActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private var TransferHistory: MutableList<HashMap<String, Any>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_history)

        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

    }

    private fun displayTransferHistory() {
        if (TransferHistory.size <= 0) {
            transferHistory_noTransfers.visibility = View.VISIBLE
        } else {
            transferHistory_noTransfers.visibility = View.GONE
            transferHistory_listView.adapter = TransferHistoryAdapter(this)
        }
    }


    inner class TransferHistoryAdapter(context: Context): BaseAdapter(){

        private val mContext = context

        override fun getCount(): Int = TransferHistory.size

        override fun getItem(position: Int): Any? = null

        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val transferHistoryView = layoutInflater.inflate(R.layout.transferhistory_transferhistoryview, parent, false)

            transferHistoryView.findViewById<TextView>(R.id.transferHistory_From).text = "From: %s".format(TransferHistory[position]["From"].toString())
            transferHistoryView.findViewById<TextView>(R.id.transferHistory_Amount).text = "Amount: %.2f".format(TransferHistory[position]["Amount"].toString().toDouble())
            transferHistoryView.findViewById<Button>(R.id.transferHistory_DeleteAllButton).setOnClickListener {
                TransferHistory = arrayListOf()
                displayTransferHistory()
                setTransferHistoryData()
            }
            transferHistoryView.findViewById<Button>(R.id.transferHistory_DeleteButton).setOnClickListener {
                TransferHistory.removeAt(position)
                displayTransferHistory()
                setTransferHistoryData()
            }



            return transferHistoryView
        }

    }


    private fun getTransferHistoryData() {

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {
                    @Suppress("UNCHECKED_CAST")
                    TransferHistory = it.get("TransferHistory") as MutableList<HashMap<String, Any>>
                    Log.d(tag, "[getTransferHistoryData] Successfully retrieved TransferHistory")
                    displayTransferHistory()
                }
                .addOnFailureListener {
                    Log.d(tag, "[getTransferHistoryData] ${it.message.toString()}")
                }

    }

    private fun setTransferHistoryData() {

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .update("TransferHistory", TransferHistory)
                .addOnSuccessListener {
                    Log.d(tag, "[setTransferHistoryData] Successfully updated TransferHistory")
                    displayTransferHistory()
                }
                .addOnFailureListener {
                    Log.d(tag, "[setTransferHistoryData] ${it.message.toString()}")
                    getTransferHistoryData()
                }

    }

    public override fun onStart() {
        super.onStart()

        getTransferHistoryData()
    }



}
