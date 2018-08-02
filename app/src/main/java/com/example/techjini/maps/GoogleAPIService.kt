package com.example.techjini.maps


import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

interface GoogleAPIService {

    @GET("geocode/json")
    fun getGeoCode(
            @Query("address") addr: String,
            @Query("key") googlekey: String): Observable<PlacesModel>


    @GET("place/details/json")
    fun getPlacesDetails(
            @Query("placeid") placeid: String,
            @Query("key") googlekey: String): Observable<Place>
}
