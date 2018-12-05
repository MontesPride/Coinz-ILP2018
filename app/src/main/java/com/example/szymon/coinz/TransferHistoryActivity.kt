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
import kotlinx.android.synthetic.main.activity_transfer_history.*

class TransferHistoryActivity : AppCompatActivity() {

    private var tag = "TransferHistoryActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private var transferHistory: MutableList<HashMap<String, Any>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_history)

        //Initialising Firebase instances
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

    }

    //displaying received transfers history
    private fun displayTransferHistory() {
        if (transferHistory.size <= 0) {
            transferHistory_noTransfers.text = getString(R.string.NoTransfers)
            transferHistory_noTransfers.visibility = View.VISIBLE
        } else {
            transferHistory_noTransfers.visibility = View.GONE
            transferHistory_listView.adapter = TransferHistoryAdapter(this)
        }
    }

    //adapter for ListView of received transfers
    inner class TransferHistoryAdapter(context: Context): BaseAdapter(){

        private val mContext = context

        override fun getCount(): Int = transferHistory.size

        override fun getItem(position: Int): Any? = null

        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val transferHistoryView = layoutInflater.inflate(R.layout.transferhistory_transferhistoryview, parent, false)

            //displaying information and setting up delete buttons
            transferHistoryView.findViewById<TextView>(R.id.transferHistory_From).text = "From: %s".format(transferHistory[position]["From"].toString())
            transferHistoryView.findViewById<TextView>(R.id.transferHistory_Amount).text = "Amount: %.2f".format(transferHistory[position]["Amount"].toString().toDouble())

            if (position == 0) {
                transferHistoryView.findViewById<Button>(R.id.transferHistory_DeleteAllButton).visibility = View.VISIBLE
            }

            transferHistoryView.findViewById<Button>(R.id.transferHistory_DeleteAllButton).setOnClickListener {
                transferHistory = arrayListOf()
                displayTransferHistory()
                setTransferHistoryData()
            }
            transferHistoryView.findViewById<Button>(R.id.transferHistory_DeleteButton).setOnClickListener {
                transferHistory.removeAt(position)
                displayTransferHistory()
                setTransferHistoryData()
            }
            return transferHistoryView
        }

    }

    //retrieving data from Firestore
    private fun getTransferHistoryData() {

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {
                    @Suppress("UNCHECKED_CAST")
                    transferHistory = it.get("TransferHistory") as MutableList<HashMap<String, Any>>
                    Log.d(tag, "[getTransferHistoryData] Successfully retrieved TransferHistory")
                    displayTransferHistory()
                }
                .addOnFailureListener {
                    Log.d(tag, "[getTransferHistoryData] ${it.message.toString()}")
                    transferHistory_noTransfers.text = getString(R.string.TransferHistoryFailDownload)
                    transferHistory_listView.visibility = View.VISIBLE
                }

    }

    //updating transfer data to Firestore
    private fun setTransferHistoryData() {

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .update("TransferHistory", transferHistory)
                .addOnSuccessListener {
                    Log.d(tag, "[setTransferHistoryData] Successfully updated TransferHistory")
                    displayTransferHistory()
                }
                .addOnFailureListener {
                    Log.d(tag, "[setTransferHistoryData] ${it.message.toString()}")
                    Toast.makeText(this, getString(R.string.TransferHistoryFailUpdate), Toast.LENGTH_LONG).show()
                    getTransferHistoryData()
                }

    }

    public override fun onStart() {
        super.onStart()
        getTransferHistoryData()
    }



}
