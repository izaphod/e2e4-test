package com.example.e2e4test.data.network.model

import com.example.e2e4test.domain.model.PlaceModel
import com.google.gson.annotations.SerializedName

data class PlaceResponse(
    @SerializedName("geometry") val geometry: GeometryResponse,
    @SerializedName("name") val name: String
) {
    data class GeometryResponse(
        @SerializedName("location") val location: LocationResponse
    )

    data class LocationResponse(
        @SerializedName("lat") val lat: Double,
        @SerializedName("lng") val lng: Double
    )
}

fun PlaceResponse.asDomain(): PlaceModel =
    PlaceModel(name, geometry.location.lat, geometry.location.lng)

fun List<PlaceResponse>.asDomain(): List<PlaceModel> = map { it.asDomain() }