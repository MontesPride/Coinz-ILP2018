package com.example.szymon.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.synthetic.main.activity_bank.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"
    private var preferencesFile = "MyPrefsFile"

    private var coinzMapData: String = ""
    private var coinzData: MutableList<HashMap<String, Any>> = arrayListOf()
    private var GOLD: Double = 0.0
    private var CoinzExchanged = 0
    private var CollectedID: MutableList<String> = arrayListOf()
    private var LastDate: String = ""
    private var LastTimestamp: Long = 0
    private var currentDate = ""
    private var rates = JSONObject()
    private var exchangeCoinzPrefix = "You can exchange "
    private var exchangeCoinzSufix = " Coinz"
    private var noCoinzCollected = "Go get some Coinz"
    private var viewSwitcher: ViewSwitcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        viewSwitcher = bank_viewSwitcher as ViewSwitcher

        bank_exchangeButton.setOnClickListener{
            bank_exchangeButton.isEnabled = false
            bank_transferButton.isEnabled = true
            displayExchangeCoinz()
            if (CoinzExchanged < 25) {
                bank_exchangedTextView.text = exchangeCoinzPrefix + (25 - CoinzExchanged).toString() + exchangeCoinzSufix
            } else {
                viewSwitcher?.showNext()
                bank_exchangedTextView.text = getString(R.string.NoExchangesLeft)
            }
        }

        bank_transferButton.setOnClickListener {
            bank_exchangeButton.isEnabled = true
            bank_transferButton.isEnabled = false
            displayTransferCoinz()
            if (CoinzExchanged < 25) {
                bank_exchangedTextView.text = getString(R.string.SomeExchangesLeft)
            } else {
                viewSwitcher?.showNext()
            }
        }

    }

    private fun displayExchangeCoinz() {

        bank_coinzGridView.adapter = coinzExchangeAdapter(this)

        if (coinzData.size <= 0) {
            bank_noCoinzCollected.text = noCoinzCollected
            bank_noCoinzCollected.visibility = View.VISIBLE
        } else {
            bank_noCoinzCollected.visibility = View.GONE
        }

    }

    private fun displayTransferCoinz() {

        bank_coinzGridView.adapter = coinzTransferAdapter(this)

        if (coinzData.size <= 0) {
            bank_noCoinzCollected.text = noCoinzCollected
            bank_noCoinzCollected.visibility = View.VISIBLE
        } else {
            bank_noCoinzCollected.visibility = View.GONE
        }

    }

    inner class coinzExchangeAdapter(context: Context): BaseAdapter() {

        private val mContext: Context = context

        override fun getCount(): Int = coinzData.size
        override fun getItem(position: Int): Any? = null


        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            /*val imageView: ImageView
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = ImageView(mContext)
                imageView.layoutParams = ViewGroup.LayoutParams(85, 85)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setPadding(8, 8, 8, 8)
            } else {
                imageView = convertView as ImageView
            }

            imageView.setImageResource(mThumbIds[position])
            return imageView*/

            /*val textView = TextView(mContext)
            textView.text = "%.2f".format(coinzData[position]["Value"].toString().toDouble())
            textView.gravity = 11
            return textView*/

            val layoutInflater = LayoutInflater.from(mContext)
            val coinView = layoutInflater.inflate(R.layout.bank_coinview, parent, false)

            coinView.findViewById<TextView>(R.id.coinCurrency).text = coinzData[position]["Currency"].toString()
            coinView.findViewById<TextView>(R.id.coinValue).text = "%.4f".format(coinzData[position]["Value"].toString().toDouble())

            if (CoinzExchanged >= 25) {
                coinView.findViewById<Button>(R.id.coinExchange).isEnabled = false
            }

            coinView.findViewById<Button>(R.id.coinExchange).setOnClickListener {
                Log.d(tag, "[onClickListener] Exchange Button clicked")
                if (CoinzExchanged < 25) {
                    Log.d(tag, "[onClickListener] value: ${coinzData[position]["Value"].toString()} exchRate: ${rates.get(coinzData[position]["Currency"].toString())}")
                    GOLD += coinzData[position]["Value"].toString().toDouble() * rates.get(coinzData[position]["Currency"].toString()).toString().toDouble()
                    Log.d(tag, "$GOLD")
                    coinzData.removeAt(position)
                    Log.d(tag, CollectedID.toString())
                    Log.d(tag, coinzData.toString())
                    CoinzExchanged += 1
                    Log.d(tag, "${FirebaseAuth.getInstance().currentUser?.email}")
                    setCoinzData("exchange")
                    displayExchangeCoinz()
                }

            }
            return coinView

        }

    }

    inner class coinzTransferAdapter(context: Context): BaseAdapter() {

        private val mContext: Context = context

        override fun getCount(): Int = coinzData.size
        override fun getItem(position: Int): Any? = null


        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            /*val imageView: ImageView
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = ImageView(mContext)
                imageView.layoutParams = ViewGroup.LayoutParams(85, 85)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setPadding(8, 8, 8, 8)
            } else {
                imageView = convertView as ImageView
            }

            imageView.setImageResource(mThumbIds[position])
            return imageView*/

            /*val textView = TextView(mContext)
            textView.text = "%.2f".format(coinzData[position]["Value"].toString().toDouble())
            textView.gravity = 11
            return textView*/

            val layoutInflater = LayoutInflater.from(mContext)
            val coinView = layoutInflater.inflate(R.layout.bank_coinview, parent, false)

            coinView.findViewById<TextView>(R.id.coinCurrency).text = coinzData[position]["Currency"].toString()
            coinView.findViewById<TextView>(R.id.coinValue).text = "%.4f".format(coinzData[position]["Value"].toString().toDouble())
            coinView.findViewById<Button>(R.id.coinExchange).text =getString(R.string.Transfer)

            if (CoinzExchanged < 25) {
                coinView.findViewById<Button>(R.id.coinExchange).isEnabled = false
            }

            coinView.findViewById<Button>(R.id.coinExchange).setOnClickListener {
                Log.d(tag, "[onClickListener] Exchange Button clicked")
                if (CoinzExchanged >= 25) {
                    Log.d(tag, "[onClickListener] Transfering coinz")
                    /*GOLD += coinzData[position]["Value"].toString().toDouble() * rates.get(coinzData[position]["Currency"].toString()).toString().toDouble()
                    Log.d(tag, "$GOLD")
                    coinzData.removeAt(position)
                    Log.d(tag, CollectedID.toString())
                    Log.d(tag, coinzData.toString())
                    CoinzExchanged += 1
                    Log.d(tag, "${FirebaseAuth.getInstance().currentUser?.email}")*/
                    //setCoinzData()

                    val rate = rates.get(coinzData[position]["Currency"].toString()).toString().toDouble()
                    val value = coinzData[position]["Value"].toString().toDouble()
                    coinzData.removeAt(position)
                    transferCoinz(bank_transferEmail.text.toString(), rate, value)
                    displayTransferCoinz()
                }
            }
            return coinView
        }
    }

    public override fun onStart() {
        super.onStart()
        currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        getCoinzData()
        coinzMapData = applicationContext.openFileInput("coinzmap.geojson").bufferedReader().use { it.readText() }
        GOLD = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE).getFloat("GOLD", 0.0.toFloat()).toDouble()
        //Log.d(tag, coinzMapData)
        rates = JSONObject(coinzMapData).get("rates") as JSONObject
        bank_QUIDvalue.text = "%.4f".format(rates.get("QUID"))
        bank_PENYvalue.text = "%.4f".format(rates.get("PENY"))
        bank_DOLRvalue.text = "%.4f".format(rates.get("DOLR"))
        bank_SHILvalue.text = "%.4f".format(rates.get("SHIL"))
        bank_GOLDvalue.text = "%.4f".format(GOLD)
        //Log.d(tag, rates.get("SHIL").toString())
    }

    private fun getCoinzData() {
        FirebaseFirestore.getInstance().collection("Coinz").document(FirebaseAuth.getInstance().currentUser?.email!!)
                .get()
                .addOnSuccessListener {

                    GOLD = it.get("GOLD").toString().toDouble()
                    coinzData = it.get("CollectedCoinz") as MutableList<HashMap<String, Any>>
                    LastDate = it.get("LastDate") as String
                    LastTimestamp = it.get("LastTimestamp") as Long
                    Log.d(tag, "[getCoinzData] ${Timestamp.now().seconds}, $LastTimestamp")
                    if (LastDate != currentDate && Timestamp.now().seconds < LastTimestamp) {
                        CollectedID  = arrayListOf()
                    } else {
                        CollectedID = it.get("CollectedID") as MutableList<String>
                    }

                    if (LastDate != currentDate && Timestamp.now().seconds >= LastTimestamp) {
                        CoinzExchanged = 0
                    } else {
                        CoinzExchanged = it.get("CoinzExchanged").toString().toInt()
                    }

                    //CoinzDataDowloaded = true

                    Log.d(tag, "[getCoinzData] Size of CollectedID: ${CollectedID.size}, LastDate: $LastDate, currentDate: $currentDate")
                    for (ID in CollectedID) {
                        Log.d(tag, "[getCoinzData] $ID")
                    }

                    bank_GOLDvalue.text = "%.4f".format(GOLD)
                    if (CoinzExchanged < 25) {
                        bank_exchangedTextView.text = exchangeCoinzPrefix + (25 - CoinzExchanged).toString() + exchangeCoinzSufix
                    } else {
                        bank_exchangedTextView.text = getString(R.string.NoExchangesLeft)
                    }
                    Log.d(tag, "[getCoinzData] Successfully downloaded coinzData")
                    displayExchangeCoinz()

                }
                .addOnFailureListener {
                    Log.d(tag, "[getCoinzData] ${it.message.toString()}")
                }
    }

    private fun setCoinzData(Operation: String) {
        val userData = HashMap<String, Any>()
        /*userData.put("QUID", 1)
        userData.put("PENY", 1)
        userData.put("DOLR", 1)
        userData.put("SHIL", 1)*/
        userData.put("GOLD", GOLD)
        //userData.put("CollectedID", Arrays.asList(""))
        userData.put("CollectedID", CollectedID)
        //userData.put("CollectedCoinz", listOf(""))
        userData.put("CollectedCoinz", coinzData)
        userData.put("LastDate", currentDate)
        userData.put("LastTimestamp", Timestamp.now().seconds)
        userData.put("CoinzExchanged", CoinzExchanged)
        for (ID in CollectedID) {
            Log.d(tag, "[setCoinzData] $ID")
        }
        FirebaseFirestore.getInstance().collection("Coinz").document(FirebaseAuth.getInstance().currentUser?.email!!)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(tag, "[setCoinzData] Successfully added to Firestore")
                    if (Operation == "exchange") {
                        displayExchangeCoinz()
                    } else if (Operation == "transfer") {
                        displayTransferCoinz()
                    }

                    bank_GOLDvalue.text = "%.4f".format(GOLD)
                    if (CoinzExchanged < 25) {
                        bank_exchangedTextView.text = exchangeCoinzPrefix + (25 - CoinzExchanged).toString() + exchangeCoinzSufix
                    } else {
                        bank_exchangedTextView.text = getString(R.string.NoExchangesLeft)
                    }
                }
                .addOnFailureListener {
                    Log.d(tag, "[setCoinzData] ${it.message.toString()}")
                }
    }

    private fun transferCoinz(targetEmail: String, coinzTransferRate: Double, coinzValue: Double) {
        if (targetEmail == FirebaseAuth.getInstance().currentUser?.email) {
            bank_transferEmail.error = getString(R.string.TransferSameEmail)
            bank_transferEmail.requestFocus()
            return
        }

        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(targetEmail)
                .addOnSuccessListener {
                    Log.d(tag, "[transferCoinz] ${it.signInMethods.toString()}")
                    if (it.signInMethods!!.size <= 0) {
                        bank_transferEmail.error = getString(R.string.error_incorrect_email)
                        bank_transferEmail.requestFocus()
                        return@addOnSuccessListener
                    } else {
                        FirebaseFirestore.getInstance().collection("Coinz").document(targetEmail)
                                .get()
                                .addOnSuccessListener {
                                    var GOLD = it.get("GOLD").toString().toDouble()
                                    GOLD += coinzTransferRate*coinzValue
                                    FirebaseFirestore.getInstance().collection("Coinz").document(targetEmail)
                                            .update("GOLD", GOLD)
                                            .addOnSuccessListener {
                                                Log.d(tag, "[transferCoinz] Coin successfully transfered")
                                                setCoinzData("transfer")
                                            }
                                            .addOnFailureListener {
                                                Log.d(tag, "[transferCoinz] ${it.message.toString()}")
                                            }
                                }
                                .addOnFailureListener {
                                    Log.d(tag, "[transferCoinz] ${it.message.toString()}")
                                }
                    }

                }
                .addOnFailureListener {
                    Log.d(tag, "[transferCoinz] ${it.message.toString()}")
                }
    }

    public override fun onResume() {
        super.onResume()
        getCoinzData()
    }

}
