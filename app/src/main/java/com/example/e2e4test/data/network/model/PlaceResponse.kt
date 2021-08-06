package com.example.e2e4test.data.network.model

import com.example.e2e4test.domain.model.PlaceModel
import com.google.gson.annotations.SerializedName

data class PlaceResponse(
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

fun PlaceResponse.asDomain(): PlaceModel = PlaceModel(name, latitude, longitude)

fun List<PlaceResponse>.asDomain(): List<PlaceModel> = map { it.asDomain() }