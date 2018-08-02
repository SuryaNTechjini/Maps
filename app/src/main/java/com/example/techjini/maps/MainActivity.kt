package com.example.techjini.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.example.techjini.maps.databinding.ActivityMainBinding
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMapClickListener {
    override fun onMapClick(latLng: LatLng?) {
        mMap?.clear()
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
        mMap?.addMarker(latLng?.let {
            MarkerOptions()
                    .position(it)
        })
        binding?.card?.visibility = View.VISIBLE
        binding?.titleCancel?.visibility = View.GONE
        binding?.snippet?.visibility = View.INVISIBLE
        latLng?.let { makeAPI(it, this) }
    }

    private fun makeAPI(latLng: LatLng, context: Context) {

        val geocoder = Geocoder(context, Locale.ENGLISH)
        try {
            val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            binding?.card?.visibility = View.VISIBLE
            binding?.progressBar?.visibility = View.VISIBLE
            this.latLng = latLng
            if (addressList != null && addressList.size > 0 && !addressList[0].getAddressLine(0).isEmpty()) {
                makeGeoCodeAPI(addressList, context)
                binding?.directions?.visibility = View.VISIBLE
            } else {
                binding?.progressBar?.visibility = View.GONE
                binding?.directions?.visibility = View.GONE
                binding?.phone?.visibility = View.GONE
                binding?.dialler?.visibility = View.GONE
                binding?.titleCancel?.visibility = View.VISIBLE
                binding?.title?.visibility = View.VISIBLE
                binding?.snippet?.visibility = View.VISIBLE
                binding?.title?.setText(R.string.unknown_header)
                binding?.snippet?.setText(R.string.unknown_message_pinned)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.directions -> {
                if (place?.result?.businessName?.isEmpty() == false) {
                    startDirection()
                }
            }
            R.id.cancel_card -> {
                binding?.card?.visibility = View.GONE
                mMap?.clear()
            }
            R.id.dialler -> {
                if (place?.result?.fomattedNumber?.isEmpty() == false) {
                    startCallIntent()
                }
            }
        }
    }

    private fun startDirection() {
        //preferences.storeBoolean(Constants.GET_DIRECTIONS, true)
        binding?.progressBar?.visibility = View.VISIBLE
        binding?.titleCancel?.visibility = View.GONE

    }

    private fun startCallIntent() {
        binding?.titleCancel?.visibility = View.GONE
        binding?.progressBar?.visibility = View.VISIBLE
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:" + place?.result?.fomattedNumber)
        binding?.progressBar?.visibility = View.GONE
        binding?.titleCancel?.visibility = View.VISIBLE
        startActivity(intent)
    }


    private var mMap: GoogleMap? = null
    private var mLocationPermissionGranted: Boolean = false

    private var latLng: LatLng? = null
    private var place: Place? = null

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var mLastKnownLocation: Location? = null

    private var googleAPIService: GoogleAPIService? = null

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
        }

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
        })

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
                            if (places?.result != null) {
                                updateBottomBar(places)
                            } else {
                                binding?.card?.visibility = View.GONE
                            }
                        }
                    })
        }
    }

    private fun updateBottomBar(places: Place) {
        this.place = places
        binding?.progressBar?.visibility = View.GONE
        binding?.titleCancel?.visibility = View.VISIBLE
        if (places.result?.businessName != null && places.result?.businessName?.isEmpty() == false) {
            binding?.title?.visibility = View.VISIBLE
            binding?.title?.text = places.result?.businessName
        } else {
            binding?.title?.setText(R.string.unknown_header)
        }
        if (places.result?.formattedAddress != null && places.result?.formattedAddress?.isEmpty() == false) {
            binding?.snippet?.visibility = View.VISIBLE
            binding?.snippet?.text = places.result?.formattedAddress
        } else {
            binding?.snippet?.setText(R.string.unknown_message_pinned)
        }
        if (places.result?.fomattedNumber != null && places.result?.fomattedNumber?.isEmpty() == false) {
            binding?.phone?.visibility = View.VISIBLE
            binding?.phone?.text = places.result?.fomattedNumber
            binding?.dialler?.visibility = View.VISIBLE
        } else {
            binding?.phone?.visibility = View.GONE
            binding?.dialler?.visibility = View.GONE
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
                            if (placesModel?.results != null) {
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
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }
}
