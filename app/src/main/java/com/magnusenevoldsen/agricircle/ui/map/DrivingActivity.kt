package com.magnusenevoldsen.agricircle.ui.map

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.magnusenevoldsen.agricircle.AgriCircleBackend
import com.magnusenevoldsen.agricircle.LocalBackend
import com.magnusenevoldsen.agricircle.R
import com.squareup.picasso.Picasso
import kotlin.collections.ArrayList

class DrivingActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap : GoogleMap
    private val zoom : Float = 18.0f
    private var locationRequest : LocationRequest? = null
    private var fusedLocationClient : FusedLocationProviderClient? = null
    private var locationCallback : LocationCallback? = null
    private val MY_PERMISSION_FINE_LOCATION = 101
    private var speedCurrentLocation : LatLng? = null
    private var speedLastLocation : LatLng? = null
    private var speedCurrently : Double = 0.0
    private var lastLocationTime : Long? = null
    private var currentLocationTime : Long? = null
    private var context : Context? = null

    //Views
    var fieldPictureImageView : ImageView? = null
    var fieldNameTextView : TextView? = null
    var fieldWorkTextView : TextView? = null
    var yourSpeedTextView : TextView? = null
    var yourSpeedNumberTextView : TextView? = null
    var suggestedSpeedTextView : TextView? = null
    var yourTractorImageView : ImageView? = null
    var suggestedSpeedNumberTextView : TextView? = null
    var suggestedTractorImageView : ImageView? = null
    var finishFAB : ExtendedFloatingActionButton? = null
    var pauseFAB : ExtendedFloatingActionButton? = null

    //Track
    var trackArray : ArrayList<LatLng> = ArrayList()

    //Time
    private var playOrPause : Boolean = false
    var pauseOffset : Long = 0
    var timeTextView : Chronometer? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)

        context = this


        //Layout setup

        //Map setup
        val mapFragment = supportFragmentManager.findFragmentById(R.id.drivingMapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Field info
        fieldPictureImageView = findViewById(R.id.drivingIconImageView)
        fieldNameTextView = findViewById(R.id.drivingFieldTextView)
        fieldWorkTextView = findViewById(R.id.drivingWorkTextView)

        //Field progress
        yourSpeedTextView = findViewById(R.id.drivingCurrentSpeedTextView)
        yourSpeedNumberTextView = findViewById(R.id.drivingNumberCurrentSpeedTextView)
        suggestedSpeedTextView = findViewById(R.id.drivingSuggestedSpeedTextView)
        suggestedSpeedNumberTextView = findViewById(R.id.drivingNumberSuggestedSpeedTextView)

        //Time
        timeTextView = findViewById(R.id.drivingTimeTextView)
//        timeTextView!!.format = "Time: %s"

//        timeTextView!!.setOnChronometerTickListener {
//            timeTextView!!.base = SystemClock.elapsedRealtime()
//        }

        //Tractors
        yourTractorImageView = findViewById(R.id.drivingCurrentTractorImageView)
        suggestedTractorImageView = findViewById(R.id.drivingSuggestedTractorImageView)

        //Change Tractor Color
        val colorOrange = ResourcesCompat.getColor(resources, R.color.colorOrange, null)
        yourTractorImageView!!.setColorFilter(colorOrange)
        val colorGreen = ResourcesCompat.getColor(resources, R.color.colorGreen, null)
        suggestedTractorImageView!!.setColorFilter(colorGreen)

        //Buttons
        finishFAB = findViewById(R.id.finishFloatingActionButton)
        pauseFAB = findViewById(R.id.pauseFloatingActionButton)


        //Click Listeners
        finishFAB!!.setOnClickListener {
            finishSession()
        }

        pauseFAB!!.setOnClickListener {
            pauseSession()
        }

        //Get location
        locationRequest = LocationRequest()
        locationRequest!!.interval = 2000 // Find ud af hvor ofte der bør opdateres. pt 1 sek for test formål
        locationRequest!!.fastestInterval = 1000 //1 sec
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //Overvej at bruge HIGH ACCURACY istedet. / BALANCED

        //Location -> Hent den 1 gang når view åbner
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) // ????????????
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient!!.lastLocation.addOnSuccessListener {location ->
                if (location != null) {
                    //Update UI             --- Go to field 0 instead????
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoom))   //Brug animateCamera eller moveCamera
//                    mMap.addMarker(MarkerOptions().position(currentLocation))



                }
            }
        } else {
            //Request permission
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_FINE_LOCATION)
        }

        locationCallback = object : LocationCallback() {
            @TargetApi(Build.VERSION_CODES.O)
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                for (location in p0!!.locations) {
                    //Update UI
                    if (location != null && playOrPause) {
                        val currentLocation = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))






// -----------------------------------------------------------------------------------------------------------------------------------------
                        // Draw line
                        drawTrack(currentLocation)

// -----------------------------------------------------------------------------------------------------------------------------------------

                        val kmh = (location.speed * 3.6)
                        val kmhString = kmh.toString().substringBefore(".")

                        if (kmh > 45 && kmh < 55)
                            yourTractorImageView!!.setColorFilter(Color.GREEN)
                        else
                            yourTractorImageView!!.setColorFilter(Color.YELLOW)




                        yourSpeedNumberTextView!!.text = kmhString + " km/h"





                    }
                }
            }
        }


        setupUI()



        





    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
//        val campusLyngby = LatLng(55.785558, 12.521564)
//        mMap.addMarker(MarkerOptions().position(campusLyngby).title("Campus Lyngby"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLyngby, zoom))
    }

    fun setupUI() {
        //Update field name
        fieldNameTextView!!.text = LocalBackend.allFields[AgriCircleBackend.selectedField].name
        //Update workitem text
        //Update timer
        //Update speed
        //Update image
        var imageUrl : String = LocalBackend.allFields[AgriCircleBackend.selectedField].activeCropImageUrl



        fieldPictureImageView!!.setImageResource(R.drawable.stock_crop_image)
        if (!imageUrl.equals("null")){
            try {
                Picasso.get().load(imageUrl).into(fieldPictureImageView)
            } catch (e : IllegalArgumentException) {
                Log.d("", e.toString())
            }
        }




    }

    fun drawTrack(location : LatLng) {
        //Add location to array
        trackArray.add(location)

        //Add a marker
        addCustomMarker(location)

        //Get nr. of tracks
        var amountOfTracks = trackArray.size

        //Draw the track
        if (amountOfTracks >= 2){
            var poly : Polyline = mMap.addPolyline(
                PolylineOptions()
                    .clickable(false)
                    .add(trackArray[amountOfTracks-2],
                        trackArray[amountOfTracks-1])
            )
            poly.color = ContextCompat.getColor(this, R.color.colorPolygonDriving)
        }



    }

    fun addCustomMarker(location : LatLng) {
        var markerOverlay = bitmapDescriptorFromVector(context as DrivingActivity, R.drawable.ic_stop_red_24dp)?.let {
            GroundOverlayOptions()
                .image(it)
                .clickable(true)
                .position(location, 5f, 5f)
        }
        mMap.addGroundOverlay(markerOverlay)

    }

    fun finishSession () {
        //Save progress
        //Go to workspace view with info about the session

        //Currently just goes back
        super.onBackPressed() //Remove this
    }

    fun pauseSession () {
        //Pause session
        playOrPause = !playOrPause
        switchPlayPause()
        startStopChronometer()

    }

    fun switchPlayPause() {
        if (playOrPause) {  //Started
            pauseFAB!!.setIconResource(R.drawable.ic_pause_circle_outline_black_24dp)
            pauseFAB!!.text = "Pause"
        } else { //Stopped
            pauseFAB!!.setIconResource(R.drawable.ic_play_circle_outline_black_24dp)
            pauseFAB!!.text = "Start"
        }
    }

    fun startStopChronometer() {
        if (playOrPause) {  //Started
            timeTextView!!.base = SystemClock.elapsedRealtime() - pauseOffset
            timeTextView!!.start()
        } else {    //Stopped
            timeTextView!!.stop()
            pauseOffset = SystemClock.elapsedRealtime() - timeTextView!!.base;
        }
    }

    fun resetChronometer() {
        timeTextView!!.base = SystemClock.elapsedRealtime()
        pauseOffset = 0
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient!!.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient!!.removeLocationUpdates(locationCallback)
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }




}
