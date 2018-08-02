package com.example.techjini.maps

import com.google.gson.annotations.SerializedName

class PlacesModel{

    @SerializedName("results")
    var results : List<Result> ? = null

    @SerializedName("status")
    var status : String ? = null
}

class Result{

    // Opening hours, photo, reviews and other information we can query here

    @SerializedName("formatted_address")
    var formattedAddress : String ? = null

    @SerializedName("place_id")
    var placeId : String ? = null

    @SerializedName("international_phone_number")
    var fomattedNumber : String ? = null

    @SerializedName("name")
    var businessName : String ? = null


}

class Place{

    @SerializedName("result")
    var result : Result ? = null
}



