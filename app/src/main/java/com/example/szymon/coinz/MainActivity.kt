package com.example.szymon.coinz


import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.CamcorderProfile
import android.os.AsyncTask
import android.os.Build.ID
import android.os.Bundle
import android.os.PersistableBundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Feature
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private val tag = "MainActivity"

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
    private var CoinzDataDowloaded = false

    private var locationServicesDisabledSnackbar: Snackbar? = null
    private var isItStart = true
    private var fromStop = false

    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin
    private var locationEnabled = false

    private var QUID: Double? = null
    private var PENY: Double? = null
    private var DOLR: Double? = null
    private var SHIL: Double? = null
    private var GOLD: Double? = null
    private var CollectedCoinz: List<HashMap<String, Any>> = listOf()
    private var CollectedID: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)



        //fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //           .setAction("Action", null).show()
        //}

        fab.setOnClickListener {view ->
            Snackbar.make(view, "${FirebaseAuth.getInstance().currentUser?.displayName}", Snackbar.LENGTH_LONG).show()
        }//bottom middle

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }//bottom left

        goToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }//bottom right

        signOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Snackbar.make(it, "Signed out, uid: ${FirebaseAuth.getInstance().currentUser?.uid}", Snackbar.LENGTH_LONG).show()
        }//top left

        displayMarkersButton.setOnClickListener {
            getCoinzData()
        } // top middle

        showCoinz.setOnClickListener {
            Snackbar.make(it, "Q:$QUID, P:$PENY, S:$SHIL, D:$DOLR, G:$GOLD", Snackbar.LENGTH_LONG).show()
            Log.d(tag, "[showCoinz] Size of CollectedID: ${CollectedID.size}")
            for (ID in CollectedID) {
                Log.d(tag, "[showCoinz] $ID")
            }
            //Log.d(tag, markers.toString())
        }   //center right

        addData.setOnClickListener {
            /*val userData = HashMap<String, Any>()
            userData.put("QUID", 0)
            userData.put("PENY", 2)
            userData.put("DOLR", 0)
            userData.put("SHIL", 5)
            userData.put("GOLD", 0)
            userData.put("COLLECTED", Arrays.asList(""))
            FirebaseFirestore.getInstance().collection("Coinz").document(FirebaseAuth.getInstance().currentUser?.uid!!)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d(tag, "[addData] Successfully added data to Firestore")
                    }
                    .addOnFailureListener {
                        Log.d(tag, "[addData] ${it.message.toString()}")
                    }*/
            setCoinzData(QUID!!, PENY!!, DOLR!!, SHIL!!, GOLD!!, CollectedID, CollectedCoinz)
        }//top right

        addGOLD.setOnClickListener {
            GOLD = GOLD?.plus(1)
        } //center left

        //getCoinzData()

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            mapReady = true
            Log.d(tag, "[onMapReady] mapboxMap is ready")
            map = mapboxMap
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
            //displayMarkers()
            if (!markersDisplyed && CoinzDataDowloaded) {
                Log.d(tag, "[onMapReady] Displaying markers")
                markersDisplyed = true
                displayMarkers()
            }
        }
    }

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
                            val routeResponse = response ?: return
                            val body = routeResponse.body() ?: return
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

    private fun enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag, "[enableLocation] Permissions are granted")
            initaliseLocationEngine()
            initialiseLocationLayer()
            locationEnabled = true
        } else {
            Log.d(tag, "[enableLocation] Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun displayMarkers() {
        val featureList = FeatureCollection.fromJson(coinzMapData).features()
        for (feature in featureList!!) {
            val featureGeometry = feature.geometry().takeIf { it?.type() == "Point" } as Point
            val featureProperties = feature.properties()
            val coordinatesAsList = featureGeometry.coordinates()
            val coordinatesAsLatLng = LatLng(coordinatesAsList[1], coordinatesAsList[0])
            val currencyName = featureProperties!!["currency"].asString
            val currencyValue = featureProperties["value"].asString
            val markerColor = featureProperties["marker-color"].asString
            var icon: Icon
            when (markerColor) {
                "#ff0000" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.mapbox_marker_icon_default)
                "#008000" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker)
                "#0000ff" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker)
                "#ffdf00" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker)
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

    private fun getCoinzData() {
        FirebaseFirestore.getInstance().collection("Coinz").document(FirebaseAuth.getInstance().currentUser?.uid!!)
                .get()
                .addOnSuccessListener {
                    Log.d(tag, "[getData] Successfully retrieved data from Firestore")
                    //Log.d(tag, it.toString())

                    QUID = it.get("QUID").toString().toDouble()
                    PENY = it.get("PENY").toString().toDouble()
                    DOLR = it.get("DOLR").toString().toDouble()
                    SHIL = it.get("SHIL").toString().toDouble()
                    GOLD = it.get("GOLD").toString().toDouble()
                    CollectedID = it.get("CollectedID") as List<String>
                    CollectedCoinz = it.get("CollectedCoinz") as List<HashMap<String, Any>>
                    CoinzDataDowloaded = true

                    Log.d(tag, "[getCoinzData] Size of CollectedID: ${CollectedID.size}")
                    for (ID in CollectedID) {
                        Log.d(tag, "[getCoinzData] $ID")
                    }

                    if (!markersDisplyed && mapReady) {
                        Log.d(tag, "[getCoinzData] Displaying markers")
                        markersDisplyed = true
                        displayMarkers()
                    }
                }
                .addOnFailureListener {
                    Log.d(tag, "[getCoinzData] ${it.message.toString()}")
                }
    }

    private fun setCoinzData(Quid: Double, Peny: Double, Dolr: Double, Shil: Double, Gold: Double, CollectedID: List<String>, CollectedCoinz: List<HashMap<String, Any>>) {
        val userData = HashMap<String, Any>()
        userData.put("QUID", 1)
        userData.put("PENY", 1)
        userData.put("DOLR", 1)
        userData.put("SHIL", 1)
        userData.put("GOLD", Gold)
        //userData.put("CollectedID", Arrays.asList(""))
        userData.put("CollectedID", CollectedID)
        //userData.put("CollectedCoinz", listOf(""))
        userData.put("CollectedCoinz", CollectedCoinz)
        Log.d(tag, "[setCoinzData]$QUID,$PENY,$DOLR,$SHIL,$GOLD")
        Log.d(tag, "[setCoinzData] Size of CollectedID: ${CollectedID.size}")
        for (ID in CollectedID) {
            Log.d(tag, "[setCoinzData] $ID")
        }
        FirebaseFirestore.getInstance().collection("Coinz").document(FirebaseAuth.getInstance().currentUser?.uid!!)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(tag, "[setCoinzData] Successfully added to Firestore")
                }
                .addOnFailureListener {
                    Log.d(tag, "[setCoinzData] ${it.message.toString()}")
                }
    }

    private fun distanceBetweenCoordinates(Lat1:Double, Long1:Double, Lat2:Double, Long2:Double): Double{
        val radius = 6371
        val difLat = Lat2 - Lat1
        val difLong = Long2 - Long1
        val result = 1000*2*radius*Math.asin(Math.sqrt( ((1 - Math.cos(difLat)) / 2)  + Math.cos(Lat1)*Math.cos(Lat2)*((1 - Math.cos(difLong)) / 2)  ))
        return result
    }

    private fun checkCoinz() {
        if (this::originLocation.isInitialized && markersDisplyed) {
            Log.d(tag, "[checkCoinz] Checking if there are coinz within 25 meters")
            val featureList = FeatureCollection.fromJson(coinzMapData).features()
            for (feature in featureList!!) {
                val featureGeometry = feature.geometry() as Point
                val coordinatesAsList = featureGeometry.coordinates()
                val coinzLatitude = Math.toRadians(coordinatesAsList[1])
                val coinzLongitude = Math.toRadians(coordinatesAsList[0])
                val originLatitude = Math.toRadians(originLocation.latitude)
                val originLongitude = Math.toRadians(originLocation.longitude)
                val distance = distanceBetweenCoordinates(coinzLatitude, coinzLongitude, originLatitude, originLongitude)
                if (distance <= 10) {
                    Log.d(tag, "[checkCoinz] dist(in meters): $distance, coinzLatLong: $coinzLatitude|$coinzLongitude, originLatLong: $originLatitude|$originLongitude")
                    if (feature.properties()!!["id"].asString !in CollectedID) {
                        Log.d(tag, "[checkCoinz] Feature not in collected")
                        //CollectedCoinz += feature
                        val Coin = HashMap<String, Any>()
                        Coin.put("Currency", feature.properties()!!["currency"].asString)
                        Coin.put("Value", feature.properties()!!["value"].asDouble)
                        CollectedCoinz += Coin
                        Log.d(tag, "[checkCoinz] Added Coin to collectedCoinz")
                        CollectedID += feature.properties()!!["id"].asString
                        Log.d(tag, "[checkCoinz] Added CoinID to collectedID")
                        Log.d(tag, "[checkCoinz] Size of CollectedID: ${CollectedID.size}")
                        for (ID in CollectedID) {
                            Log.d(tag, "[checkCoinz] $ID")
                        }
                        //Log.d(tag, "[checkCoinz] featureID: ${feature.properties()!!["id"]}")
                        //Log.d(tag, "[checkCoinz]$QUID,$PENY,$DOLR,$SHIL,$GOLD,${CollectedID}")
                        //setCoinzData(QUID!!, PENY!!, DOLR!!, SHIL!!, GOLD!!, CollectedID)
                        //Log.d(tag, "[checkCoinz] Sent data to Firestore")
                        for (marker in markers) {
                            //Log.d(tag, "[checkCoinz] I am in for loop")
                            if (marker?.position == LatLng(coordinatesAsList[1], coordinatesAsList[0]) && marker.title == feature.properties()!!["currency"].asString && marker.snippet == feature.properties()!!["value"].asString) {
                                Log.d(tag, "[checkCoinz] Removing marker")
                                map?.removeMarker(marker)
                                Log.d(tag, "[checkCoinz] Marker removed")
                            }
                        }
                    }
                }
            }
            /*for (feature in CollectedCoinz) {
                Log.d(tag, "[checkCoinz] ${feature.properties()!!["currency"].asString}, ${feature.properties()!!["value"].asString}")
            }*/

        } else {
            Log.d(tag, "[checkCoinz] originLocation not initialized")
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
        if (FirebaseAuth.getInstance().currentUser?.uid == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        downloadDate = settings.getString("lastDownloadDate", "")!!
        Log.d(tag, "[onStart] Recalled lastDownloadDate is '$downloadDate'")
        GOLD = settings.getFloat("GOLD", 0.0.toFloat()).toDouble()
        Log.d(tag, "[onStart] Recalled GOLD is '$GOLD'")

        currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        if (currentDate != downloadDate) {
            val coinzMapUrl = coinzMapUrlPrefix + currentDate + coinzMapUrlSufix
            DownloadFileTask(DownloadCompleteRunner).execute(coinzMapUrl)
        } else {
            Log.d(tag, "[onStart] geoJSON maps are up to date")
            coinzMapData = applicationContext.openFileInput("coinzmap.geojson").bufferedReader().use { it.readText() }
        }
        getCoinzData()
        mapView?.onStart()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
        if (fromStop) {
            Log.d(tag, "[onResume] Asking for permission")
            fromStop = false
            permissionsManager.requestLocationPermissions(this)
        }

        if(PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag, "[onResume] Permissions are granted")
            if (!locationEnabled) {
                enableLocation()
            }
            //initaliseLocationEngine()
            //initialiseLocationLayer()
            if (locationServicesDisabledSnackbar?.isShown == true) {
                locationServicesDisabledSnackbar?.dismiss()
            }
        } else {
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
        if (FirebaseAuth.getInstance().currentUser?.uid == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

    }

    public override fun onPause() {
        super.onPause()
        Log.d(tag, "[onPause] App is onPause")
        if (locationServicesDisabledSnackbar?.isShown == true) {
            locationServicesDisabledSnackbar?.dismiss()
        }
        mapView?.onPause()
    }

    public override fun onStop() {
        super.onStop()
        Log.d(tag, "[onStop] App is onStop")
        fromStop = true
        Log.d(tag, "[onStop] Storing lastDownloadDate of '$downloadDate'")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        Log.d(tag, "[onStop] Storing GOLD of '$GOLD'")
        editor.putFloat("GOLD", GOLD!!.toFloat())
        editor.apply()

        mapView?.onStop()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

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
            Log.d("MainActivity", "[DownloadFileTask] Trying to download file")
            loadFileFromNetwork(urls[0])
        } catch (e: IOException) {
            downloadError = true
            "$e Unable to load content, Check your network connection"
        }

        private fun loadFileFromNetwork(urlString: String): String {
            val stream : InputStream = downloadUrl(urlString)
            // Read input from stream, build result as a string
            val result = stream.bufferedReader().use { it.readText() }
            return result
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
            if(!downloadError) {
                downloadDate = currentDate
            }
            caller.downloadComplete(result)
            onSuccessfulDownload()
        }

    } //end class DownloadFileTask




}