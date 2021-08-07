package com.example.e2e4test.data.network.model

import com.google.gson.annotations.SerializedName

data class PlaceResponseList(
    @SerializedName("data") val data: List<PlaceResponse>
)
