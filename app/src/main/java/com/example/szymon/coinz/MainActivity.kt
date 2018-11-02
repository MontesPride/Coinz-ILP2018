package com.example.szymon.coinz


import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.CamcorderProfile
import android.os.AsyncTask
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.Snackbar
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point

import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode


import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private var mAuth: FirebaseAuth? = null

    private val tag = "MainActivity"

    private var downloadError = false
    private var downloadDate = "" // YYYY/MM/DD
    private var currentDate = ""
    private var preferencesFile = "MyPrefsFile" // for storing preferences

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var coinzMapUrlPrefix = "http://homepages.inf.ed.ac.uk/stg/coinz/"
    private var coinzMapUrlSufix = "/coinzmap.geojson"
    private var coinzMapData: String = ""

    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin

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
        }

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        goToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        signOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Snackbar.make(it, "${FirebaseAuth.getInstance().currentUser?.uid}", Snackbar.LENGTH_LONG).show()
        }

        displayMarkersButton.setOnClickListener { displayMarkers() }

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }


    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            Log.d(tag, "[onMapReady] mapboxMap is ready")
            map = mapboxMap



            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            enableLocation()
            displayMarkers()
        }
    }

    private fun enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag, "Permissions are granted")
            initaliseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun displayMarkers() {
        /*var features = FeatureCollection.fromJson(coinzMapData).features()
        var feature = features!![0]
        var featureGeometry = feature.geometry().takeIf { it?.type() == "Point" }
        //Log.d(tag, features.toString())
        Log.d(tag, featureGeometry.toString())
        var pnt = featureGeometry as Point
        Log.d(tag, "$feature")
        //Log.d(tag, "${pnt.coordinates().}")
        var coords = LatLng(pnt.coordinates()[1], pnt.coordinates()[0])
        var properties = feature.properties()!!["id"]
        var currencyName = feature.properties()!!["currency"]
        var currencyValue = feature.properties()!!["value"]
        Log.d(tag, properties.toString())

        map?.addMarker(MarkerOptions().position(coords).title(currencyName.toString()).snippet(currencyValue.toString()))*/

        val featureList = FeatureCollection.fromJson(coinzMapData).features()
        for (feature in featureList!!) {
            val featureGeometry = feature.geometry().takeIf { it?.type() == "Point" } as Point
            val featureProperties = feature.properties()
            val coordinatesAsList = featureGeometry.coordinates()
            val coordinatesAsLatLng = LatLng(coordinatesAsList[1], coordinatesAsList[0])
            val currencyName = featureProperties!!["currency"].asString
            val currencyValue = featureProperties["value"].asString
            val markerColor = featureProperties["marker-color"].asString
            //Log.d(tag, "markerColor = $markerColor")
            //Log.d(tag, "$currencyName $currencyValue")
            var icon: Icon
            when (markerColor) {
                "#ff0000" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.mapbox_marker_icon_default)
                "#008000" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.green_marker)
                "#0000ff" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.blue_marker)
                "#ffdf00" -> icon = IconFactory.getInstance(this).fromResource(R.drawable.yellow_marker)
                else -> icon = IconFactory.getInstance(this).fromResource(R.drawable.purple_marker)
            }

            map?.addMarker(MarkerOptions()
                    .position(coordinatesAsLatLng)
                    .title(currencyName)
                    .snippet(currencyValue)
                    .icon(icon))
        }

    }



    @SuppressWarnings("MissingPermission")
    private fun initaliseLocationEngine() {
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
        if (mapView == null) {
            Log.d(tag, "mapView is null")
        } else {
            if (map == null) {
                Log.d(tag, "map is null")
            } else {
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
        }
    }

    @SuppressWarnings("MissingPermissions")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag, "Permissions: $permissionsToExplain")
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
        //Log.d(tag, coinzMapData)
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

        currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        Log.d(tag, "[onCreate] Current date is: $currentDate")
        if (currentDate != downloadDate) {
            val coinzMapUrl = coinzMapUrlPrefix + currentDate + coinzMapUrlSufix
            DownloadFileTask(DownloadCompleteRunner).execute(coinzMapUrl)
        } else {
            Log.d(tag, "[onStart] geoJSON maps are up to date")
            coinzMapData = applicationContext.openFileInput("coinzmap.geojson").bufferedReader().use { it.readText() }
            //Log.d(tag, coinzMapData)
            Log.d(tag, getFilesDir().absolutePath)
            //displayMarkers()
        }

        mapView?.onStart()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    public override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser?.uid == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        mapView?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onStop() {
        super.onStop()

        Log.d(tag, "[onStop] Storing lastDownloadDate of '$downloadDate'")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
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
