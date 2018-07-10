package com.example.techjini.maps;


import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface GoogleAPIService {

    @GET("geocode/json")
    Observable<PlacesModel> getGeoCode(
            @Query("address") String addr,
            @Query("key") String googlekey);


    @GET("place/details/json")
    Observable<Place> getPlacesDetails(
            @Query("placeid") String placeid,
            @Query("key") String googlekey);
}
