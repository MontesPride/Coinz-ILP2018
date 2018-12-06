package com.example.szymon.coinz


import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.Timestamp.now
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point

import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute


import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private val tag = "MainActivity"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    private var downloadError = false
    private var downloadDate = "" // YYYY/MM/DD
    private var currentDate = ""
    private var preferencesFile = "MyPrefsFile" // for storing preferences

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var currentDestination: Point? = null
    private var coinzMapUrlPrefix = "http://homepages.inf.ed.ac.uk/stg/coinz/"
    private var coinzMapUrlSufix = "/coinzmap.geojson"
    private var coinzMapData: String = ""
    private var marker: Marker? = null
    private var markers = ArrayList<Marker?>()
    private var markersDisplyed = false
    private var mapReady = false
    private var coinzDataDownloaded = false

    private var locationServicesDisabledSnackbar: Snackbar? = null
    private var isItStart = true
    private var fromStop = false

    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin
    private var locationEnabled = false

    private var gold: Double? = null
    private var username: String? = null
    private var collectedCoinz: MutableList<HashMap<String, Any>> = arrayListOf()
    private var collectedID: MutableList<String> = arrayListOf()
    private var lastDate: String = ""
    private var lastTimestamp: Long = 0
    private var coinzExchanged: Int = 0
    private var coinzReceived = 0
    private var quests: MutableList<HashMap<String, Any>> = arrayListOf()
    private var rerolled = false
    private var newQuestAdded = false
    private var transferHistory: MutableList<HashMap<String, Any>> = arrayListOf()
    private var allCollectedToday = false
    private var wageredToday = false
    private var wager = HashMap<String, Any>()
    private lateinit var wagerTimer: CountDownTimer

    private var invalidDateAndTimeSnackbar: Snackbar? = null

    private var vibratorService: Vibrator? = null

    private var visibleButtons = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initalising Firebase instances
        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()

        //Setting up buttons' onClickListeners
        main_OpenMenu.setOnClickListener { showOrHideButtons() }

        main_Ranking.setOnClickListener {
            showOrHideButtons()
            startActivity(Intent(this, RankingActivity::class.java))
        }

        main_Race.setOnClickListener {
            showOrHideButtons()
            startActivity(Intent(this, RaceActivity::class.java))
        }

        main_Bank.setOnClickListener {
            showOrHideButtons()
            startActivity(Intent(this, BankActivity::class.java))
        }

        main_Transfer.setOnClickListener {
            showOrHideButtons()
            startActivity(Intent(this, TransferHistoryActivity::class.java))
        }

        main_Quest.setOnClickListener {
            showOrHideButtons()
            startActivity(Intent(this, QuestActivity::class.java))
        }

        main_SignOut.setOnClickListener {
            mAuth.signOut()
            finish()
        }

        vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        invalidDateAndTimeSnackbar = Snackbar.make(mapboxMapView, getString(R.string.error_invalid_date_and_time), Snackbar.LENGTH_INDEFINITE)

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun showOrHideButtons() {
        if (visibleButtons) {
            main_Ranking.hide()
            main_Race.hide()
            main_Bank.hide()
            main_Transfer.hide()
            main_Quest.hide()
            main_SignOut.hide()
        } else {
            main_Ranking.show()
            main_Race.show()
            main_Bank.show()
            main_Transfer.show()
            main_Quest.show()
            main_SignOut.show()
        }
        visibleButtons = !visibleButtons
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            mapReady = true
            Log.d(tag, "[onMapReady] mapboxMap is ready")
            map = mapboxMap
            //Setting up onMarkerClickListeners to display routing between player and a marker
            map?.setOnMarkerClickListener { marker ->

                if (this::originLocation.isInitialized) {
                    Log.d(tag, "[onMarkerClick] originLocation initalized")
                    val originLocationAsPoint = Point.fromLngLat(originLocation.longitude, originLocation.latitude)
                    val destinationLocationAsPoint = Point.fromLngLat(marker.position.longitude, marker.position.latitude)
                    getRoute(originLocationAsPoint, destinationLocationAsPoint)
                } else {
                    Log.d(tag, "[onMarkerClick] originLocation not initalized")
                }
                false
            }
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            enableLocation()

            if (!markersDisplyed && coinzDataDownloaded && !downloadError) {
                Log.d(tag, "[onMapReady] Displaying markers")
                markersDisplyed = true
                displayMarkers()
            }
        }
    }

    //Retrieving route between player and a marker
    private fun getRoute(origin: Point, destination: Point) {
        if (destination == currentDestination && navigationMapRoute != null) {
            navigationMapRoute?.removeRoute()
            currentDestination = null
        } else {
            currentDestination = destination

            NavigationRoute.builder()
                    .accessToken(getString(R.string.access_token))
                    .origin(origin)
                    .destination(destination)
                    .build()
                    .getRoute(object: Callback<DirectionsResponse> {
                        override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                            val body = response.body() ?: return
                            if (body.routes().count() == 0) {
                                Log.d(tag, "[getRoute] No routes found")
                                return
                            }
                            if (navigationMapRoute != null) {
                                navigationMapRoute?.removeRoute()
                            } else {
                                navigationMapRoute = NavigationMapRoute(null, mapView!!, map!!)
                            }
                            navigationMapRoute?.addRoute(body.routes().first())
                        }
                        override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                            Log.d(tag, "[getRoute onFailure] Error: ${t.message}")
                        }
                    })
        }
    }

    //Asking user to enable location services
    private fun enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag, "[enableLocation] Permissions are granted")
            initaliseLocationEngine()
            initialiseLocationLayer()
            permissionsManager = PermissionsManager(this)
            locationEnabled = true
        } else {
            Log.d(tag, "[enableLocation] Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    //Displaying all the markers, skipping collected ones
    private fun displayMarkers() {
        Log.d(tag, "[displayMarkers] $coinzMapData")
        val featureList = FeatureCollection.fromJson(coinzMapData).features()
        for (feature in featureList!!) {

            if (feature.properties()!!["id"].asString in collectedID) continue
            val featureGeometry = feature.geometry().takeIf { it?.type() == "Point" } as Point
            val featureProperties = feature.properties()
            val coordinatesAsList = featureGeometry.coordinates()
            val coordinatesAsLatLng = LatLng(coordinatesAsList[1], coordinatesAsList[0])
            val currencyName = featureProperties!!["currency"].asString
            val currencyValue = featureProperties["value"].asString
            val markerColor = featureProperties["marker-color"].asString
            val markerSymbol = featureProperties["marker-symbol"].asString
            var icon: Icon

            when (markerColor) {
                "#ff0000" -> when (markerSymbol) {
                    "0" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_0)
                    "1" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_1)
                    "2" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_2)
                    "3" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_3)
                    "4" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_4)
                    "5" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_5)
                    "6" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_6)
                    "7" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_7)
                    "8" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_8)
                    "9" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker_9)
                    else -> icon = IconFactory.getInstance(this).fromResource(R.drawable.red_marker)
                }
                "#008000" -> when (markerSymbol) {
                    "0" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_0)
                    "1" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_1)
                    "2" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_2)
                    "3" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_3)
                    "4" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_4)
                    "5" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_5)
                    "6" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_6)
                    "7" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_7)
                    "8" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_8)
                    "9" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker_9)
                    else -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker)
                }
                "#0000ff" -> when (markerSymbol) {
                    "0" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_0)
                    "1" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_1)
                    "2" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_2)
                    "3" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_3)
                    "4" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_4)
                    "5" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_5)
                    "6" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_6)
                    "7" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_7)
                    "8" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_8)
                    "9" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker_9)
                    else -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker)
                }
                "#ffdf00" -> when (markerSymbol) {
                    "0" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_0)
                    "1" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_1)
                    "2" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_2)
                    "3" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_3)
                    "4" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_4)
                    "5" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_5)
                    "6" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_6)
                    "7" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_7)
                    "8" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_8)
                    "9" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker_9)
                    else -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker)
                }
                else -> icon = IconFactory.getInstance(this).fromResource(R.drawable.purple_marker)
            }
            marker = map?.addMarker(MarkerOptions()
                    .position(coordinatesAsLatLng)
                    .title(currencyName)
                    .snippet(currencyValue)
                    .icon(icon))
            markers.add(marker)
        }
        checkCoinz()
    }

    //Retrieving user Data from Firestore
    private fun getCoinzData() {
        if (mAuth.currentUser?.uid == null) {
            finish()
        }

        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .get()
                .addOnSuccessListener {
                    Log.d(tag, "[getData] Successfully retrieved data from Firestore")
                    Log.d(tag, "uid: ${mAuth.currentUser?.email}")

                    gold = it.get("GOLD").toString().toDouble()
                    @Suppress("UNCHECKED_CAST")
                    collectedCoinz = it.get("CollectedCoinz") as MutableList<HashMap<String, Any>>
                    lastDate = it.get("LastDate") as String
                    lastTimestamp = it.get("LastTimestamp") as Long
                    username = it.get("Username") as String
                    @Suppress("UNCHECKED_CAST")
                    quests = it.get("Quests") as MutableList<HashMap<String, Any>>
                    rerolled = it.get("Rerolled") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    transferHistory = it.get("TransferHistory") as MutableList<HashMap<String, Any>>
                    allCollectedToday = it.get("AllCollectedToday") as Boolean
                    @Suppress("UNCHECKED_CAST")
                    wager = it.get("Wager") as HashMap<String, Any>
                    wageredToday = it.get("WageredToday") as Boolean

                    //If there was a wager going on, and player has run out of time, take gold from his account
                    if (!wager.isEmpty()) {
                        if (wager["Time"].toString().toInt() + wager["Start"].toString().toInt() - Timestamp.now().seconds <= 0) {
                            gold = gold!! - wager["Reward"].toString().toInt()
                            wager = HashMap()
                            displayFinishedWager("Failure")
                            mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                                    .update("GOLD", gold,
                                            "Wager", wager)
                                    .addOnSuccessListener {
                                        Log.d(tag, "[getCoinzData] Successfully updated Wager")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d(tag, "[getCoinzData] ${e.message.toString()}")
                                        Toast.makeText(this, getString(R.string.UpdateDataFail), Toast.LENGTH_LONG).show()
                                    }
                        }
                    }

                    //If there is a new day, reset some of the user's variables and add 2 new quests
                    if (lastDate != currentDate && Timestamp.now().seconds >= lastTimestamp) {
                        Log.d(tag, "[getCoinzData] New Day!")
                        collectedID  = arrayListOf()
                        coinzExchanged = 0
                        coinzReceived = 0
                        rerolled = false
                        allCollectedToday = false
                        wageredToday = false

                        for (i in (0..1)) {
                            if (quests.size < 10) {
                                val amount = (3..6).shuffled().first()
                                val currency = arrayListOf("QUID", "PENY", "DOLR", "SHIL").shuffled().first()
                                val reward = arrayListOf(100, 150, 200, 300)[amount - 3]
                                val quest = HashMap<String, Any>()

                                quest["Amount"] = amount
                                quest["Currency"] = currency
                                quest["Reward"] = reward
                                quest["CompletionStage"] = 0
                                quests.add(quest)
                                newQuestAdded = true
                            }
                        }
                        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                                .update("CollectedID", collectedID,
                                        "CoinzExchanged", coinzExchanged,
                                        "CoinzReceived", coinzReceived,
                                        "LastDate", currentDate,
                                        "Quests", quests,
                                        "Rerolled", rerolled,
                                        "AllCollectedToday", allCollectedToday,
                                        "WageredToday", wageredToday)
                                .addOnSuccessListener {
                                    if (newQuestAdded) {
                                        newQuestAdded = !newQuestAdded
                                        Toast.makeText(this, getString(R.string.NewDailyQuest), Toast.LENGTH_LONG).show()
                                    }
                                    Log.d(tag, "[getCoinzData] New day, some values successfully reset")
                                }
                                .addOnFailureListener { e ->
                                    Log.d(tag, "[getCoinzData] ${e.message.toString()}")
                                    Toast.makeText(this, getString(R.string.UpdateDataFail), Toast.LENGTH_LONG).show()
                                }
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        collectedID = it.get("CollectedID") as MutableList<String>
                        coinzExchanged = it.get("CoinzExchanged").toString().toInt()
                        coinzReceived = it.get("CoinzReceived").toString().toInt()
                    }
                    coinzDataDownloaded = true

                    //If time and date isn't correctly set up on the user's phone, don't display markers and ask him to set it up correctly
                    if (!markersDisplyed && mapReady && Timestamp.now().seconds >= lastTimestamp && !downloadError) {
                        Log.d(tag, "[getCoinzData] Displaying markers")
                        markersDisplyed = true
                        displayMarkers()
                    }
                    if (Timestamp.now().seconds < lastTimestamp) {
                        invalidDateAndTimeSnackbar?.show()
                    }

                    //If there is a wager going on, and players hasn't run out of time, display a timer
                    if(!wager.isEmpty()) {
                        main_WagerTextView.visibility = View.VISIBLE
                        setWagerTimer()
                    }
                }
                .addOnFailureListener {
                    Log.d(tag, "[getCoinzData] ${it.message.toString()}")
                    Toast.makeText(this, getString(R.string.DownloadDataFail), Toast.LENGTH_LONG).show()
                }
    }

    //Update user data
    private fun setCoinzData() {
        val userData = HashMap<String, Any>()
        userData["GOLD"] = gold!!
        userData["CollectedID"] = collectedID
        userData["CollectedCoinz"] = collectedCoinz
        userData["LastDate"] = currentDate
        userData["LastTimestamp"] = Timestamp.now().seconds
        userData["CoinzExchanged"] = coinzExchanged
        userData["CoinzReceived"] = coinzReceived
        userData["Username"] = username!!
        userData["Rerolled"] = rerolled
        userData["Quests"] = quests
        userData["TransferHistory"] = transferHistory
        userData["AllCollectedToday"] = allCollectedToday
        userData["Wager"] = wager
        userData["WageredToday"] = wageredToday

        Log.d(tag, "[setCoinzData] Size of CollectedID: ${collectedID.size}, LastDate: $lastDate, currentDate: $currentDate")

        if (mAuth.currentUser?.uid == null) {
            finish()
        }

        //If user has collected all coinz in a given day, give him extra 2500 gold
        if (!allCollectedToday && collectedID.size >= 50) {
            allCollectedToday = true
            gold = gold!! + 2500
        }

        //When user collects a coin and it is successfully added to Firestore, notify him of it by vibrating for 100 milliseconds
        mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(tag, "[setCoinzData] Successfully added to Firestore")
                    if (vibratorService!!.hasVibrator()) { // Vibrator availability checking
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                            // void vibrate (VibrationEffect vibe)
                            vibratorService?.vibrate(
                                    VibrationEffect.createOneShot(
                                            100,
                                            // The default vibration strength of the device.
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                            )
                        }else{
                            // This method was deprecated in API level 26
                            vibratorService?.vibrate(100)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.d(tag, "[setCoinzData] ${it.message.toString()}")
                    Toast.makeText(this, getString(R.string.UpdateDataFail), Toast.LENGTH_LONG).show()
                }
    }

    //Function calculating distance between a player and a coin, based on Haversine formula
    private fun distanceBetweenCoordinates(Lat1:Double, Long1:Double, Lat2:Double, Long2:Double): Double{
        val radius = 6371
        val difLat = Lat2 - Lat1
        val difLong = Long2 - Long1
        return 1000*2*radius*Math.asin(Math.sqrt(((1 - Math.cos(difLat))/2) + Math.cos(Lat1)*Math.cos(Lat2)*((1 - Math.cos(difLong))/2)))
    }

    //onLocationChanged, check if there are coinz in a 25 meters radius
    private fun checkCoinz() {
        if (this::originLocation.isInitialized && markersDisplyed) {

            Log.d(tag, "[checkCoinz] Checking if there are coinz within 25 meters")
            val featureList = FeatureCollection.fromJson(coinzMapData).features()

            for (feature in featureList!!) {

                if (feature.properties()!!["id"].asString in collectedID) continue
                val featureGeometry = feature.geometry() as Point
                val coordinatesAsList = featureGeometry.coordinates()
                val coinzLatitude = Math.toRadians(coordinatesAsList[1])
                val coinzLongitude = Math.toRadians(coordinatesAsList[0])
                val originLatitude = Math.toRadians(originLocation.latitude)
                val originLongitude = Math.toRadians(originLocation.longitude)
                val distance = distanceBetweenCoordinates(coinzLatitude, coinzLongitude, originLatitude, originLongitude)

                //there is a coin within 25 meters radius, add it to Firestore and remove its marker
                if (distance <= 25) {
                    Log.d(tag, "[checkCoinz] dist(in meters): $distance, coinzLatLong: $coinzLatitude|$coinzLongitude, originLatLong: $originLatitude|$originLongitude")

                    if (feature.properties()!!["id"].asString !in collectedID) {

                        val coin = HashMap<String, Any>()
                        coin["Currency"] = feature.properties()!!["currency"].asString
                        coin["Value"] = feature.properties()!!["value"].asDouble
                        coin["Time"] = Timestamp.now().seconds
                        collectedCoinz.add(coin)
                        Log.d(tag, "[checkCoinz] Added Coin to collectedCoinz")
                        collectedID.add(feature.properties()!!["id"].asString)
                        Log.d(tag, "[checkCoinz] Added CoinID to collectedID")
                        Log.d(tag, "[checkCoinz] Size of CollectedID: ${collectedID.size}")

                        //updating completion stage of a quest, if there is one
                        for (quest in quests) {
                            if (quest["Currency"].toString() == feature.properties()!!["currency"].asString) {
                                quest["CompletionStage"] = quest["CompletionStage"].toString().toInt() + 1
                                //if quest is completed, give player a promised reward
                                if (quest["CompletionStage"].toString().toInt() >= quest["Amount"].toString().toInt()) {
                                    gold = gold!! + quest["Reward"].toString().toDouble()
                                    Log.d(tag, "[checkCoinz] Quest completed ${quests.indexOf(quest)}, $gold")
                                    Toast.makeText(this, getString(R.string.QuestCompleted), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        //removing completed quests
                        quests.removeIf{it["CompletionStage"].toString().toInt() >= it["Amount"].toString().toInt()}

                        //if there is a wager going on, and players has collected all the necessary coinz, notify and reward him
                        if (!wager.isEmpty()) {
                            wager["CompletionStage"] = wager["CompletionStage"].toString().toInt() + 1

                            if (wager["CompletionStage"].toString().toInt() >= wager["Amount"].toString().toInt()) {
                                wagerTimer.cancel()
                                gold = gold!! + wager["Reward"].toString().toInt()
                                wager = HashMap()
                                displayFinishedWager("Success")
                            }
                        }

                        //udpate user's coinz data in Firestore
                        setCoinzData()

                        //remove collected markers from the map
                        for (marker in markers) {
                            if (marker?.position == LatLng(coordinatesAsList[1], coordinatesAsList[0]) && marker.title == feature.properties()!!["currency"].asString && marker.snippet == feature.properties()!!["value"].asString) {
                                Log.d(tag, "[checkCoinz] Removing marker")
                                map?.removeMarker(marker)
                                Log.d(tag, "[checkCoinz] Marker removed")
                            }
                        }
                    }
                }
            }
        } else {
            Log.d(tag, "[checkCoinz] originLocation not initialized or markers not displayed")
        }
    }

    //Generating new document for a ner user
    private fun createUserDocument(username: String): HashMap<String, Any> {

        val userData = HashMap<String, Any>()
        userData["GOLD"] = 0
        userData["CollectedCoinz"] = listOf<HashMap<String, Any>>()
        userData["CollectedID"] = listOf<String>()
        userData["LastDate"] = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        userData["LastTimestamp"] = Timestamp.now().seconds
        userData["CoinzExchanged"] = 0
        userData["CoinzReceived"] = 0
        userData["Username"] = username
        userData["Rerolled"] = false
        userData["TransferHistory"] = listOf<HashMap<String, Any>>()
        userData["AllCollectedToday"] = false
        val amount = (3..6).shuffled().first()
        val currency = arrayListOf("QUID", "PENY", "DOLR", "SHIL").shuffled().first()
        val reward = arrayListOf(100, 150, 200, 300)[amount - 3]
        val quests: MutableList<HashMap<String, Any>> = arrayListOf()
        val quest = HashMap<String, Any>()
        quest["Amount"] = amount
        quest["Currency"] = currency
        quest["Reward"] = reward
        quest["CompletionStage"] = 0
        quests.add(quest)
        userData["Quests"] = quests
        userData["Wager"] = HashMap<String, Any>()
        userData["WageredToday"] = false

        return userData
    }

    //creating Wager Timer as well as the function which takes player's gold if he fails to complete the challenge
    private fun setWagerTimer(){
        val timeLeft = wager["Time"].toString().toInt() - Timestamp.now().seconds + wager["Start"].toString().toInt()
        updateWagerTimer()
        wagerTimer = object: CountDownTimer(timeLeft*1000, 1000){
            override fun onFinish() {
                gold = gold!! - wager["Reward"].toString().toInt()
                wager = HashMap()
                mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                        .update("GOLD", gold,
                                "Wager", wager)
                        .addOnSuccessListener {
                            Log.d(tag, "[setWagerTimer] Successfully updated Wager")
                        }
                        .addOnFailureListener { e ->
                            Log.d(tag, "[setWagerTimer] ${e.message.toString()}")
                        }
                displayFinishedWager("Failure")
            }
            override fun onTick(millisUntilFinished: Long) {
                updateWagerTimer()
            }
        }
        wagerTimer.start()
    }

    //updating the timer
    private fun updateWagerTimer() {
        val timeLeft = wager["Time"].toString().toInt() - Timestamp.now().seconds + wager["Start"].toString().toInt()
        main_WagerTextView.text = String.format(getString(R.string.WagerTimer), TimeUnit.SECONDS.toMinutes(timeLeft), timeLeft - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(timeLeft)), wager["CompletionStage"], wager["Amount"])
    }

    //displaying appropriate message when player collects all necessary coinz or the time runs out
    private fun displayFinishedWager(result: String) {
        main_WagerTextView.visibility = View.VISIBLE
        if (result == "Success") {
            main_WagerTextView.text = getString(R.string.WagerSuccess)
            object: CountDownTimer(5000, 1000){
                override fun onFinish() {
                    main_WagerTextView.visibility = View.GONE
                }
                override fun onTick(millisUntilFinished: Long) {}

            }
                    .start()
        } else {
            main_WagerTextView.text = getString(R.string.WagerFailure)
            object: CountDownTimer(5000, 1000){
                override fun onFinish() {
                    main_WagerTextView.visibility = View.GONE
                }
                override fun onTick(millisUntilFinished: Long) {}
            }
                    .start()
        }
    }


    @SuppressWarnings("MissingPermission")
    private fun initaliseLocationEngine() {
        Log.d(tag, "[initaliseLocationEngine] Initalising Location Engine")
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        Log.d(tag, "[initialiseLocationLayer] Initalising Location Layer")
        if (mapView == null) {
            Log.d(tag, "mapView is null")
        } else {
            if (map == null) {
                Log.d(tag, "map is null")
            } else {
                Log.d(tag, "[initialiseLocationLayer] mapView and map are not null")
                locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
                lifecycle.addObserver(locationLayerPlugin)
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            Log.d(tag, "[onLocationChanged] location changed")
            originLocation = location
            setCameraPosition(originLocation)
            checkCoinz()
        }
    }

    @SuppressWarnings("MissingPermissions")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag, "[onExplanationNeeded] Permissions: $permissionsToExplain")
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if(granted) {
            enableLocation()
        } else {
            //Open a dialogue with an user
        }
    }

    private fun onSuccessfulDownload() {
        coinzMapData = DownloadCompleteRunner.result!!
        applicationContext.openFileOutput("coinzmap.geojson", Context.MODE_PRIVATE).use {
            it.write(coinzMapData.toByteArray())
        }
    }

    public override fun onStart() {
        super.onStart()
        mapView?.onStart()

        Log.d(tag, "[onStart] App is onStart")

        if (::locationEngine.isInitialized) {
                try {
                    locationEngine.requestLocationUpdates()
                    Log.d(tag, "[onStart] requesting Location updates")
                } catch (throwable: SecurityException) {
                    locationEngine.addLocationEngineListener(this)
                    Log.d(tag, "[onStart] Failed to request Location updates")
                }

        }

        //if user somehow enter this screen without being logged in, go back to SignUpActivity
        if (mAuth.currentUser?.uid == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        //retrieving some basic information from a saved file
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        downloadDate = settings.getString("lastDownloadDate", "")!!
        Log.d(tag, "[onStart] Recalled lastDownloadDate is '$downloadDate'")
        gold = settings.getFloat("GOLD", 0.0.toFloat()).toDouble()
        Log.d(tag, "[onStart] Recalled GOLD is '$gold'")

        //if there is a new day, download a new map
        currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        if (currentDate != downloadDate) {
            val coinzMapUrl = coinzMapUrlPrefix + currentDate + coinzMapUrlSufix
            DownloadFileTask(DownloadCompleteRunner).execute(coinzMapUrl)
        } else {
            Log.d(tag, "[onStart] geoJSON maps are up to date")
            coinzMapData = applicationContext.openFileInput("coinzmap.geojson").bufferedReader().use { it.readText() }
        }
        //retrieve user's data from Firestore, during testing I have found out, there is a possibility of registering
        //new user without creating his document in Firestore, so just in case I am trying to create it
        if (mAuth.currentUser?.uid != null) {

            mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                    .get()
                    .addOnSuccessListener { document ->
                        if(!document.exists()) {

                            val newUserData = createUserDocument(mAuth.currentUser?.displayName!!)

                            mStore.collection("Coinz").document(mAuth.currentUser?.email!!)
                                    .set(newUserData)
                                    .addOnSuccessListener {
                                        Log.d(tag, "[onStart] Successfully created newUser document")
                                        getCoinzData()
                                    }
                                    .addOnFailureListener {
                                        Log.d(tag, "[onStart] ${it.message.toString()}")
                                        Toast.makeText(this, getString(R.string.UpdateDataFail), Toast.LENGTH_LONG).show()
                                    }
                        } else {
                            getCoinzData()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d(tag, "[onStart] ${e.message.toString()}")
                        Toast.makeText(this, getString(R.string.DownloadDataFail), Toast.LENGTH_LONG).show()
                    }
        }

    }

    //instead of going back to Login or SignUp Activity, go to home screen
    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
        Log.d(tag, "[onResume] App is onResume")
        //If permissions are not granted, ask for them again
        if (fromStop) {
            Log.d(tag, "[onResume] Asking for permission")
            fromStop = false
            permissionsManager.requestLocationPermissions(this)
        }
        //If permissions are granted, make sure to enable location services again
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag, "[onResume] Permissions are granted")
            if (!locationEnabled) {
                enableLocation()
            }
            if (locationServicesDisabledSnackbar?.isShown == true) {
                locationServicesDisabledSnackbar?.dismiss()
            }
        } else {
            //if permissions are not granted, create a snackbar that will notify user
            Log.d(tag, "[onResume] Permissions are not granted")
            if (isItStart) {
                isItStart = false
            } else {
                locationServicesDisabledSnackbar = Snackbar.make(mapboxMapView, getString(R.string.error_location_services_disabled), Snackbar.LENGTH_INDEFINITE)
                if (locationServicesDisabledSnackbar?.isShown != true) {
                    locationServicesDisabledSnackbar?.show()
                }
            }
        }

        if (now().seconds >= lastTimestamp && invalidDateAndTimeSnackbar!!.isShown) {
            invalidDateAndTimeSnackbar?.dismiss()
        }

        if (mAuth.currentUser?.uid == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        if (::locationEngine.isInitialized) {
                try {
                    locationEngine.requestLocationUpdates()
                    Log.d(tag, "[onResume] requesting Location updates")
                } catch (throwable: SecurityException) {
                    locationEngine.addLocationEngineListener(this)
                    Log.d(tag, "[onResume] Failed to request Location updates")
                }
        }

    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
        Log.d(tag, "[onPause] App is onPause")

    }

    public override fun onStop() {
        super.onStop()
        mapView?.onStop()

        //this SIGSEGV fix caused my app to be unable to track user's location :(
        /*if(::locationEngine.isInitialized) {
                locationEngine.removeLocationEngineListener(this)
                locationEngine.removeLocationUpdates()

        }*/

        //onStop, save some necessary data to a file
        Log.d(tag, "[onStop] App is onStop")
        fromStop = true
        Log.d(tag, "[onStop] Storing lastDownloadDate of '$downloadDate'")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        Log.d(tag, "[onStop] Storing GOLD of '$gold'")
        editor.putFloat("GOLD", gold!!.toFloat())
        editor.apply()


    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    public override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    //override onSaveInstanceState

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //Just downloading a new map
    interface DownloadCompleteListener {
        fun downloadComplete(result: String)
    }

    object DownloadCompleteRunner : DownloadCompleteListener {
        var result : String? = null
        override fun downloadComplete(result: String) {
            this.result = result
            Log.d("MainActivity", "[downloadComplete] Download complete")
            // Log.d("MainActivity", this.result)
        }
    }

    inner class DownloadFileTask(private val caller : DownloadCompleteListener):
            AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg urls: String): String = try {
            Log.d(tag, "[DownloadFileTask] Trying to download file")
            loadFileFromNetwork(urls[0])
        } catch (e: IOException) {
            downloadError = true
            "$e Unable to load content, Check your network connection"
        }

        private fun loadFileFromNetwork(urlString: String): String {
            val stream : InputStream = downloadUrl(urlString)
            // Read input from stream, build result as a string
            return stream.bufferedReader().use { it.readText() }
        }

        @Throws(IOException::class)
        private fun downloadUrl(urlString: String): InputStream {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            return conn.inputStream
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            Log.d(tag, "[onPostExecute] $downloadError")
            if(!downloadError) {
                downloadDate = currentDate
                caller.downloadComplete(result)
                onSuccessfulDownload()
            } else {
                Log.d(tag, "[onPostExecute] Failed to download map")
                Snackbar.make(mapboxMapView, getString(R.string.DownloadMapDataFail), Snackbar.LENGTH_INDEFINITE).show()
            }

        }

    } //end class DownloadFileTask




}