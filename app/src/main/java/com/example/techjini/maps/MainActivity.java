package com.example.techjini.maps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main);

        // Construct a GeoDataClient.
        Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.

        // Construct a FusedLocationProviderClient.

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        //getDeviceLocation();
        getCurrentLocation(this);
    }

    private GoogleAPIService googleAPIService;

    private void getCurrentLocation(final Context context) {
        googleAPIService = GoogleAPICall.getGoogleAPI(this);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .alpha(0.5f));
                Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
                try {
                  List<Address> addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
                  makeGeoCodeAPI(addressList,context);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                //getPlaceDetails();
            }
        });

    }

    public void makePlacesAPI(PlacesModel model, Context context){
        googleAPIService.getPlacesDetails(model.getResults().get(0).getPlaceId(),
                context.getString(R.string.google_maps_key))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Place>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("Place",e.getMessage());
                    }

                    @Override
                    public void onNext(Place places) {
                        if(places != null){
                            updateBottomBar(places);
                        }
                    }
                });
    }

    private void updateBottomBar(Place place) {
        findViewById(R.id.card).setVisibility(View.VISIBLE);
        if(place.getResult().getBusinessName() != null && !place.getResult().getBusinessName().isEmpty())
        {
            findViewById(R.id.title).setVisibility(View.VISIBLE);
            TextView view  = findViewById(R.id.title);
            view.setText(place.getResult().getBusinessName());
        }
        if(place.getResult().getFormattedAddress()!= null && !place.getResult().getFormattedAddress().isEmpty()){
            findViewById(R.id.snippet).setVisibility(View.VISIBLE);
            TextView view  = findViewById(R.id.snippet);
            view.setText(place.getResult().getFormattedAddress());
        }
        if(place.getResult().getFomattedNumber()!= null && !place.getResult().getFomattedNumber().isEmpty()){
            findViewById(R.id.phone).setVisibility(View.VISIBLE);
            TextView view  = findViewById(R.id.phone);
            view.setText(place.getResult().getFomattedNumber());
            findViewById(R.id.dialler).setVisibility(View.VISIBLE);
        }
    }

    public void makeGeoCodeAPI(List<Address> addressList, final Context context){
        googleAPIService.getGeoCode(addressList.get(0).getAddressLine(0),
                context.getString(R.string.google_maps_key))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<PlacesModel>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("PlacesModel",e.getMessage());
                    }

                    @Override
                    public void onNext(PlacesModel placesModel) {
                        if(placesModel != null ){
                            makePlacesAPI(placesModel,context);
                        }
                    }
                });
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
