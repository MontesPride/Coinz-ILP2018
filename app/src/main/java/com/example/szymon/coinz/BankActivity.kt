package com.example.szymon.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_bank.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"
    private var preferencesFile = "MyPrefsFile"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private var coinzMapData: String = ""
    private var coinzData: MutableList<HashMap<String, Any>> = arrayListOf()
    private var GOLD: Double = 0.0
    private var CoinzExchanged = 0
    private var CollectedID: MutableList<String> = arrayListOf()
    private var LastDate: String = ""
    private var LastTimestamp: Long = 0
    private var currentDate = ""
    private var Username = ""
    private var CoinzReceived = 0
    private var Quests: MutableList<HashMap<String, Any>> = arrayListOf()
    private var Rerolled = false
    private var TransferHistory: MutableList<HashMap<String, Any>> = arrayListOf()
    private var AllCollectedToday = false
    private var Wager = HashMap<String, Any>()
    private var WageredToday = false
    private var rates = JSONObject()
    private var exchangeCoinzPrefix = "You can exchange "
    private var exchangeCoinzSufix = " Coinz"
    private var noCoinzCollected = "Go get some Coinz"
    private var viewSwitcher: ViewSwitcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        viewSwitcher = bank_viewSwitcher as ViewSwitcher

        bank_exchangeButton.setOnClickListener{
            bank_exchangeButton.isEnabled = false
            bank_transferButton.isEnabled = true
            displayExchangeCoinz()
            if (CoinzExchanged < 25) {
                bank_exchangedTextView.text = String.format(getString(R.string.ExchangeCoinz), 25 - CoinzExchanged)
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

            val layoutInflater = LayoutInflater.from(mContext)
            val coinView = layoutInflater.inflate(R.layout.bank_coinview, parent, false)

            coinView.findViewById<TextView>(R.id.coinCurrency).text = coinzData[position]["Currency"].toString()
            coinView.findViewById<TextView>(R.id.coinValue).text = "%.2f".format(coinzData[position]["Value"].toString().toDouble())

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
                    Log.d(tag, "${mAuth.currentUser?.email}")
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

            val layoutInflater = LayoutInflater.from(mContext)
            val coinView = layoutInflater.inflate(R.layout.bank_coinview, parent, false)

            coinView.findViewById<TextView>(R.id.coinCurrency).text = coinzData[position]["Currency"].toString()
            coinView.findViewById<TextView>(R.id.coinValue).text = "%.2f".format(coinzData[position]["Value"].toString().toDouble())
            coinView.findViewById<Button>(R.id.coinExchange).text =getString(R.string.Transfer)

            if (CoinzExchanged < 25) {
                coinView.findViewById<Button>(R.id.coinExchange).isEnabled = false
            }

            coinView.findViewById<Button>(R.id.coinExchange).setOnClickListener {
                Log.d(tag, "[onClickListener] Transfer Button clicked")

                if (!isEmailValid(bank_transferEmail.text.toString())) {
                    Log.d(tag, "[onClickListener] Invalid email")
                    bank_transferEmail.error = getString(R.string.error_invalid_email)
                    bank_transferEmail.requestFocus()
                } else {
                    if (CoinzExchanged >= 25) {
                        Log.d(tag, "[onClickListener] Transfering coinz")
                        val rate = rates.get(coinzData[position]["Currency"].toString()).toString().toDouble()
                        val value = coinzData[position]["Value"].toString().toDouble()
                        coinzData.removeAt(position)
                        transferCoinz(bank_transferEmail.text.toString(), rate, value)
                        displayTransferCoinz()
                    }
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
        bank_QUIDvalue.text = String.format(getString(R.string.RatesValue), rates.get("QUID"))
        bank_PENYvalue.text = String.format(getString(R.string.RatesValue), rates.get("PENY"))
        bank_DOLRvalue.text = String.format(getString(R.string.RatesValue), rates.get("DOLR"))
        bank_SHILvalue.text = String.format(getString(R.string.RatesValue), rates.get("SHIL"))
        bank_GOLDvalue.text = String.format(getString(R.string.GoldAmount), GOLD)
        //Log.d(tag, rates.get("SHIL").toString())
    }

    private fun getCoinzData() {
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {

                    GOLD = it.get("GOLD").toString().toDouble()
                    @Suppress("UNCHECKED_CAST")
                    coinzData = it.get("CollectedCoinz") as MutableList<HashMap<String, Any>>
                    @Suppress("UNCHECKED_CAST")
                    CollectedID = it.get("CollectedID") as MutableList<String>
                    LastDate = it.get("LastDate") as String
                    LastTimestamp = it.get("LastTimestamp") as Long
                    Username = it.get("Username") as String
                    CoinzReceived = it.get("CoinzReceived").toString().toInt()
                    CoinzExchanged = it.get("CoinzExchanged").toString().toInt()
                    @Suppress("UNCHECKED_CAST")
                    Quests = it.get("Quests") as MutableList<HashMap<String, Any>>
                    Rerolled = it.get("Rerolled") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    TransferHistory = it.get("TransferHistory") as MutableList<HashMap<String, Any>>
                    AllCollectedToday = it.get("AllCollectedToday") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    Wager = it.get("Wager") as HashMap<String, Any>
                    WageredToday = it.get("WageredToday") as Boolean


                    Log.d(tag, "[getCoinzData] ${Timestamp.now().seconds}, $LastTimestamp")

                    bank_GOLDvalue.text = "%.1f".format(GOLD)

                    if (CoinzExchanged < 25) {
                        bank_exchangedTextView.text = String.format(getString(R.string.ExchangeCoinz), 25 - CoinzExchanged)
                    } else {
                        bank_exchangedTextView.text = getString(R.string.NoExchangesLeft)
                    }
                    Log.d(tag, "[getCoinzData] Successfully downloaded coinzData")

                    Log.d(tag, "[getCoinzData] exchange button_enabled: ${bank_exchangeButton.isEnabled}")

                    if (!bank_exchangeButton.isEnabled) {
                        displayExchangeCoinz()
                    } else {
                        Log.d(tag, "displaying transfer coinz")
                        displayTransferCoinz()
                    }

                }
                .addOnFailureListener {
                    Log.d(tag, "[getCoinzData] ${it.message.toString()}")
                }
    }

    private fun setCoinzData(Operation: String) {
        val userData = HashMap<String, Any>()
        userData["GOLD"] = GOLD
        userData["CollectedID"] = CollectedID
        userData["CollectedCoinz"] = coinzData
        userData["LastDate"] = currentDate
        userData["LastTimestamp"] = Timestamp.now().seconds
        userData["CoinzExchanged"] = CoinzExchanged
        userData["Username"] = Username
        userData["CoinzReceived"] = CoinzReceived
        userData["Quests"] = Quests
        userData["Rerolled"] = Rerolled
        userData["TransferHistory"] = TransferHistory
        userData["AllCollectedToday"] = AllCollectedToday
        userData["Wager"] = Wager
        userData["WageredToday"] = WageredToday

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(tag, "[setCoinzData] Successfully added to Firestore")
                    if (Operation == "exchange") {
                        displayExchangeCoinz()
                    } else if (Operation == "transfer") {
                        displayTransferCoinz()
                    }

                    bank_GOLDvalue.text = "%.1f".format(GOLD)
                    if (CoinzExchanged < 25) {
                        bank_exchangedTextView.text = exchangeCoinzPrefix + (25 - CoinzExchanged).toString() + exchangeCoinzSufix
                    } else {
                        bank_exchangedTextView.text = getString(R.string.NoExchangesLeft)
                    }
                }
                .addOnFailureListener {
                    Log.d(tag, "[setCoinzData] ${it.message.toString()}")
                    getCoinzData()
                }
    }

    private fun transferCoinz(targetEmail: String, coinzTransferRate: Double, coinzValue: Double) {
        if (targetEmail == mAuth.currentUser?.email) {
            bank_transferEmail.error = getString(R.string.TransferSameEmail)
            bank_transferEmail.requestFocus()
            return
        }

        mAuth.fetchSignInMethodsForEmail(targetEmail)
                .addOnSuccessListener {
                    Log.d(tag, "[transferCoinz] ${it.signInMethods.toString()}")
                    if (it.signInMethods!!.size <= 0) {
                        bank_transferEmail.error = getString(R.string.error_incorrect_email)
                        bank_transferEmail.requestFocus()
                        getCoinzData()
                        return@addOnSuccessListener
                    } else {
                        mStore.collection("Coinz").document(targetEmail)
                                .get()
                                .addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val TargetTransferHistory = document.get("TransferHistory") as MutableList<HashMap<String, Any>>
                                    var TargetGOLD = document.get("GOLD").toString().toDouble()
                                    var TargetCoinzReceived = document.get("CoinzReceived").toString().toInt()

                                    if (TargetCoinzReceived >= 25) {
                                        Log.d(tag, "[transferCoinz] Traget player already received 25 coinz today.")
                                        getCoinzData()
                                    } else {
                                        TargetGOLD += coinzTransferRate*coinzValue
                                        TargetCoinzReceived += 1
                                        val TargetTransferData = HashMap<String, Any>()
                                        TargetTransferData["From"] = Username
                                        TargetTransferData["Amount"] = coinzTransferRate*coinzValue
                                        TargetTransferHistory.add(TargetTransferData)
                                        mStore.collection("Coinz").document(targetEmail)
                                                .update("GOLD", TargetGOLD,
                                                        "CoinzReceived", TargetCoinzReceived,
                                                        "TransferHistory", TargetTransferHistory)
                                                .addOnSuccessListener {
                                                    Log.d(tag, "[transferCoinz] Coin successfully transfered")
                                                    setCoinzData("transfer")
                                                    Toast.makeText(this, getString(R.string.SuccessfulTransfer), Toast.LENGTH_LONG).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.d(tag, "[transferCoinz] ${e.message.toString()}")
                                                }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.d(tag, "[transferCoinz] ${e.message.toString()}")
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
