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
    private var gold: Double = 0.0
    private var coinzExchanged = 0
    private var collectedID: MutableList<String> = arrayListOf()
    private var lastDate: String = ""
    private var lastTimestamp: Long = 0
    private var currentDate = ""
    private var username = ""
    private var coinzReceived = 0
    private var quests: MutableList<HashMap<String, Any>> = arrayListOf()
    private var rerolled = false
    private var transferHistory: MutableList<HashMap<String, Any>> = arrayListOf()
    private var allCollectedToday = false
    private var wager = HashMap<String, Any>()
    private var wageredToday = false
    private var rates = JSONObject()
    private var exchangeCoinzPrefix = "You can exchange "
    private var exchangeCoinzSufix = " Coinz"
    private var noCoinzCollected = "Go get some Coinz"
    private var viewSwitcher: ViewSwitcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        //Initialising Firebase instances
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        //Inistalising viewSwitcher between exchanging and transferring
        viewSwitcher = bank_viewSwitcher as ViewSwitcher

        //Setting up onClickListeners for Exchange and Transfer menu buttons
        bank_exchangeButton.setOnClickListener{
            bank_exchangeButton.isEnabled = false
            bank_transferButton.isEnabled = true
            displayExchangeCoinz()
            if (coinzExchanged < 25) {
                bank_exchangedTextView.text = String.format(getString(R.string.ExchangeCoinz), 25 - coinzExchanged)
            } else {
                viewSwitcher?.showNext()
                bank_exchangedTextView.text = getString(R.string.NoExchangesLeft)
            }
        }

        bank_transferButton.setOnClickListener {
            bank_exchangeButton.isEnabled = true
            bank_transferButton.isEnabled = false
            displayTransferCoinz()
            if (coinzExchanged < 25) {
                bank_exchangedTextView.text = getString(R.string.SomeExchangesLeft)
            } else {
                viewSwitcher?.showNext()
            }
        }
    }

    //Email validation function
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

    //displaying GridView of exchangeable coinz
    private fun displayExchangeCoinz() {

        bank_coinzGridView.adapter = CoinzExchangeAdapter(this)

        if (coinzData.size <= 0) {
            bank_noCoinzCollected.text = noCoinzCollected
            bank_noCoinzCollected.visibility = View.VISIBLE
        } else {
            bank_noCoinzCollected.visibility = View.GONE
        }

    }

    //displaying GridView of transferable coinz
    private fun displayTransferCoinz() {

        bank_coinzGridView.adapter = CoinzTransferAdapter(this)

        if (coinzData.size <= 0) {
            bank_noCoinzCollected.text = noCoinzCollected
            bank_noCoinzCollected.visibility = View.VISIBLE
        } else {
            bank_noCoinzCollected.visibility = View.GONE
        }
    }

    //Creating Exchange Adapter for GridView
    inner class CoinzExchangeAdapter(context: Context): BaseAdapter() {

        private val mContext: Context = context

        override fun getCount(): Int = coinzData.size
        override fun getItem(position: Int): Any? = null


        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val coinView = layoutInflater.inflate(R.layout.bank_coinview, parent, false)

            //Setting up TextViews and button onClickListener
            coinView.findViewById<TextView>(R.id.coinCurrency).text = coinzData[position]["Currency"].toString()
            coinView.findViewById<TextView>(R.id.coinValue).text = "%.2f".format(coinzData[position]["Value"].toString().toDouble())

            if (coinzExchanged >= 25) {
                coinView.findViewById<Button>(R.id.coinExchange).isEnabled = false
            }

            coinView.findViewById<Button>(R.id.coinExchange).setOnClickListener {
                Log.d(tag, "[onClickListener] Exchange Button clicked")
                if (coinzExchanged < 25) {
                    //If possible, exchanging coinz for gold
                    Log.d(tag, "[onClickListener] value: ${coinzData[position]["Value"].toString()} exchRate: ${rates.get(coinzData[position]["Currency"].toString())}")
                    gold += coinzData[position]["Value"].toString().toDouble() * rates.get(coinzData[position]["Currency"].toString()).toString().toDouble()
                    Log.d(tag, "$gold")
                    coinzData.removeAt(position)
                    Log.d(tag, collectedID.toString())
                    Log.d(tag, coinzData.toString())
                    coinzExchanged += 1
                    Log.d(tag, "${mAuth.currentUser?.email}")
                    setCoinzData("exchange")
                    displayExchangeCoinz()
                }

            }
            return coinView

        }

    }
    //Creating Transfer Adapter for GridView
    inner class CoinzTransferAdapter(context: Context): BaseAdapter() {

        private val mContext: Context = context

        override fun getCount(): Int = coinzData.size
        override fun getItem(position: Int): Any? = null


        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val coinView = layoutInflater.inflate(R.layout.bank_coinview, parent, false)

            //Setting up TextViews and button onClickListener
            coinView.findViewById<TextView>(R.id.coinCurrency).text = coinzData[position]["Currency"].toString()
            coinView.findViewById<TextView>(R.id.coinValue).text = "%.2f".format(coinzData[position]["Value"].toString().toDouble())
            coinView.findViewById<Button>(R.id.coinExchange).text =getString(R.string.Transfer)

            if (coinzExchanged < 25) {
                coinView.findViewById<Button>(R.id.coinExchange).isEnabled = false
            }

            coinView.findViewById<Button>(R.id.coinExchange).setOnClickListener {
                Log.d(tag, "[onClickListener] Transfer Button clicked")

                //if possible, transferring coinz
                if (!isEmailValid(bank_transferEmail.text.toString())) {
                    Log.d(tag, "[onClickListener] Invalid email")
                    bank_transferEmail.error = getString(R.string.error_invalid_email)
                    bank_transferEmail.requestFocus()
                } else {
                    if (coinzExchanged >= 25) {
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

    //retrieving and displaying some basic information, more info will be displayed after data from Firestore gets retrieved
    public override fun onStart() {
        super.onStart()
        currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        getCoinzData()
        coinzMapData = applicationContext.openFileInput("coinzmap.geojson").bufferedReader().use { it.readText() }
        gold = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE).getFloat("GOLD", 0.0.toFloat()).toDouble()
        rates = JSONObject(coinzMapData).get("rates") as JSONObject
        bank_QUIDvalue.text = String.format(getString(R.string.RatesValue), rates.get("QUID"))
        bank_PENYvalue.text = String.format(getString(R.string.RatesValue), rates.get("PENY"))
        bank_DOLRvalue.text = String.format(getString(R.string.RatesValue), rates.get("DOLR"))
        bank_SHILvalue.text = String.format(getString(R.string.RatesValue), rates.get("SHIL"))
        bank_GOLDvalue.text = String.format(getString(R.string.GoldAmount), gold)
    }

    //retrieving data from Firestore and displaying it
    private fun getCoinzData() {
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {

                    gold = it.get("GOLD").toString().toDouble()
                    @Suppress("UNCHECKED_CAST")
                    coinzData = it.get("CollectedCoinz") as MutableList<HashMap<String, Any>>
                    @Suppress("UNCHECKED_CAST")
                    collectedID = it.get("CollectedID") as MutableList<String>
                    lastDate = it.get("LastDate") as String
                    lastTimestamp = it.get("LastTimestamp") as Long
                    username = it.get("Username") as String
                    coinzReceived = it.get("CoinzReceived").toString().toInt()
                    coinzExchanged = it.get("CoinzExchanged").toString().toInt()
                    @Suppress("UNCHECKED_CAST")
                    quests = it.get("Quests") as MutableList<HashMap<String, Any>>
                    rerolled = it.get("Rerolled") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    transferHistory = it.get("TransferHistory") as MutableList<HashMap<String, Any>>
                    allCollectedToday = it.get("AllCollectedToday") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    wager = it.get("Wager") as HashMap<String, Any>
                    wageredToday = it.get("WageredToday") as Boolean


                    Log.d(tag, "[getCoinzData] ${Timestamp.now().seconds}, $lastTimestamp")

                    bank_GOLDvalue.text = "%.1f".format(gold)

                    if (coinzExchanged < 25) {
                        bank_exchangedTextView.text = String.format(getString(R.string.ExchangeCoinz), 25 - coinzExchanged)
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
                    Toast.makeText(this, getString(R.string.DownloadDataFail), Toast.LENGTH_LONG).show()
                }
    }

    //updating logged in user's data and displaying it
    private fun setCoinzData(Operation: String) {
        val userData = HashMap<String, Any>()
        userData["GOLD"] = gold
        userData["CollectedID"] = collectedID
        userData["CollectedCoinz"] = coinzData
        userData["LastDate"] = currentDate
        userData["LastTimestamp"] = Timestamp.now().seconds
        userData["CoinzExchanged"] = coinzExchanged
        userData["Username"] = username
        userData["CoinzReceived"] = coinzReceived
        userData["Quests"] = quests
        userData["Rerolled"] = rerolled
        userData["TransferHistory"] = transferHistory
        userData["AllCollectedToday"] = allCollectedToday
        userData["Wager"] = wager
        userData["WageredToday"] = wageredToday

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(tag, "[setCoinzData] Successfully added to Firestore")
                    if (Operation == "exchange") {
                        displayExchangeCoinz()
                    } else if (Operation == "transfer") {
                        displayTransferCoinz()
                    }

                    bank_GOLDvalue.text = "%.1f".format(gold)
                    if (coinzExchanged < 25) {
                        bank_exchangedTextView.text = exchangeCoinzPrefix + (25 - coinzExchanged).toString() + exchangeCoinzSufix
                    } else {
                        bank_exchangedTextView.text = getString(R.string.NoExchangesLeft)
                    }
                }
                .addOnFailureListener {
                    Log.d(tag, "[setCoinzData] ${it.message.toString()}")
                    Toast.makeText(this, getString(R.string.UpdateDataFail), Toast.LENGTH_LONG).show()
                    getCoinzData()
                }
    }

    //transferring coinz to another player
    private fun transferCoinz(targetEmail: String, coinzTransferRate: Double, coinzValue: Double) {
        if (targetEmail == mAuth.currentUser?.email) {
            bank_transferEmail.error = getString(R.string.TransferSameEmail)
            bank_transferEmail.requestFocus()
            return
        }

        //checking if user with given email address exists
        mAuth.fetchSignInMethodsForEmail(targetEmail)
                .addOnSuccessListener {
                    Log.d(tag, "[transferCoinz] ${it.signInMethods.toString()}")
                    if (it.signInMethods!!.size <= 0) {
                        bank_transferEmail.error = getString(R.string.error_incorrect_email)
                        bank_transferEmail.requestFocus()
                        getCoinzData()
                        return@addOnSuccessListener
                    } else {
                        //if that user exists, transfer gold to his account, create a mention in his transfer history and update logged in user's data
                        mStore.collection("Coinz").document(targetEmail)
                                .get()
                                .addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val targetTransferHistory = document.get("TransferHistory") as MutableList<HashMap<String, Any>>
                                    var targetGOLD = document.get("GOLD").toString().toDouble()
                                    var targetCoinzReceived = document.get("CoinzReceived").toString().toInt()

                                    if (targetCoinzReceived >= 25) {
                                        Log.d(tag, "[transferCoinz] Traget player already received 25 coinz today.")
                                        getCoinzData()
                                    } else {
                                        targetGOLD += coinzTransferRate*coinzValue
                                        targetCoinzReceived += 1
                                        val targetTransferData = HashMap<String, Any>()
                                        targetTransferData["From"] = username
                                        targetTransferData["Amount"] = coinzTransferRate*coinzValue
                                        targetTransferHistory.add(targetTransferData)
                                        mStore.collection("Coinz").document(targetEmail)
                                                .update("GOLD", targetGOLD,
                                                        "CoinzReceived", targetCoinzReceived,
                                                        "TransferHistory", targetTransferHistory)
                                                .addOnSuccessListener {
                                                    Log.d(tag, "[transferCoinz] Coin successfully transfered")
                                                    setCoinzData("transfer")
                                                    Toast.makeText(this, getString(R.string.SuccessfulTransfer), Toast.LENGTH_LONG).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.d(tag, "[transferCoinz] ${e.message.toString()}")
                                                    Toast.makeText(this, getString(R.string.FaieldTransferGOLD), Toast.LENGTH_LONG).show()
                                                }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.d(tag, "[transferCoinz] ${e.message.toString()}")
                                    Toast.makeText(this, getString(R.string.FaieldTransferGOLD), Toast.LENGTH_LONG).show()
                                }
                    }
                }
                .addOnFailureListener {
                    Log.d(tag, "[transferCoinz] ${it.message.toString()}")
                    Toast.makeText(this, getString(R.string.FaieldTransferGOLD), Toast.LENGTH_LONG).show()
                }
    }

    public override fun onResume() {
        super.onResume()
        getCoinzData()
    }

}
