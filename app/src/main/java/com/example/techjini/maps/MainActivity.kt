package com.example.techjini.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView

import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import java.io.IOException
import java.util.Locale

import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var mLocationPermissionGranted: Boolean = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var mLastKnownLocation: Location? = null

    private var googleAPIService: GoogleAPIService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main)

        // Construct a GeoDataClient.
        Places.getGeoDataClient(this)

        // Construct a PlaceDetectionClient.

        // Construct a FusedLocationProviderClient.

        // Build the map.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap?.cameraPosition)
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation)
            super.onSaveInstanceState(outState)
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(map: GoogleMap) {
        mMap = map
        getLocationPermission()

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        //getDeviceLocation();
        getCurrentLocation(this)
    }

    private fun getCurrentLocation(context: Context) {
        googleAPIService = GoogleAPICall.getGoogleAPI(this)
        mMap?.setOnMapClickListener(GoogleMap.OnMapClickListener { latLng ->
            mMap?.clear()
            mMap?.addMarker(latLng?.let {
                MarkerOptions()
                        .position(it)
                        .alpha(0.5f)
            })
            val geocoder = Geocoder(context, Locale.ENGLISH)
            try {
                val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addressList != null && addressList.size > 0 && !addressList[0].getAddressLine(0).isEmpty()) {
                    makeGeoCodeAPI(addressList, context)
                } else {
                    findViewById<View>(R.id.card).visibility = View.INVISIBLE
                    return@OnMapClickListener
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            //getPlaceDetails();
        } /*latLng ->
            mMap?.clear()
            mMap?.addMarker(MarkerOptions()
                    .position(latLng)
                    .alpha(0.5f))
            val geocoder = Geocoder(context, Locale.ENGLISH)
            try {
                val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addressList != null && addressList.size > 0 && !addressList[0].getAddressLine(0).isEmpty()) {
                    makeGeoCodeAPI(addressList, context)
                } else {
                    findViewById<View>(R.id.card).setVisibility(View.INVISIBLE)
                    return@OnMapClickListener
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            //getPlaceDetails();
        }*/)

    }

    fun makePlacesAPI(model: PlacesModel?, context: Context) {
        model?.results?.get(0)?.placeId?.let {
            googleAPIService?.getPlacesDetails(it,
                context.getString(R.string.google_maps_key))
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeOn(Schedulers.io())
                ?.subscribe(object : Subscriber<Place>() {
                    override fun onCompleted() {

                    }

                    override fun onError(e: Throwable) {
                        Log.e("Place", e.message)
                    }

                    override fun onNext(places: Place?) {
                        if (places != null) {
                            updateBottomBar(places)
                        }
                    }
                })
        }
    }

    private fun updateBottomBar(place: Place) {
        if (place.result?.businessName != null && place.result?.formattedAddress != null
                && place.result?.fomattedNumber != null) {
            findViewById<View>(R.id.card).visibility = View.INVISIBLE
            return
        }

        findViewById<View>(R.id.card).visibility = View.VISIBLE
        if (place.result?.businessName != null && place.result?.businessName?.isEmpty()==false) {
            findViewById<View>(R.id.title).visibility = View.VISIBLE
            val view = findViewById<TextView>(R.id.title)
            view.text = place.result?.businessName
        }
        if (place.result?.formattedAddress != null && place.result?.formattedAddress?.isEmpty()== false) {
            findViewById<View>(R.id.snippet).visibility = View.VISIBLE
            val view = findViewById<TextView>(R.id.snippet)
            view.text = place.result?.formattedAddress
        }
        if (place.result?.fomattedNumber != null && place.result?.fomattedNumber?.isEmpty()==false) {
            findViewById<View>(R.id.phone).visibility = View.VISIBLE
            val view = findViewById<TextView>(R.id.phone)
            view.text = place.result?.fomattedNumber
            findViewById<View>(R.id.dialler).visibility = View.VISIBLE
        }
    }

    private fun makeGeoCodeAPI(addressList: List<Address>?, context: Context) {
        addressList?.get(0)?.getAddressLine(0)?.let {
            googleAPIService?.getGeoCode(it,
                context.getString(R.string.google_maps_key))
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeOn(Schedulers.io())
                ?.subscribe(object : Subscriber<PlacesModel>() {
                    override fun onCompleted() {

                    }

                    override fun onError(e: Throwable) {
                        Log.e("PlacesModel", e.message)
                    }

                    override fun onNext(placesModel: PlacesModel?) {
                        if (placesModel != null) {
                            makePlacesAPI(placesModel, context)
                        }
                    }
                })
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }


    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (mLocationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                mLastKnownLocation = null
            }
        } catch (e: Exception) {
            Log.e("Exception: %s", e.message)
        }

    }

    companion object {

        private val DEFAULT_ZOOM = 15
        private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        private val KEY_CAMERA_POSITION = "camera_position"
        private val KEY_LOCATION = "location"
    }
}
