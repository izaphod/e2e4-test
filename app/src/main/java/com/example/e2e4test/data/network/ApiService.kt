package com.example.e2e4test.data.network

import com.example.e2e4test.data.network.model.PlaceResponseList
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("place/nearbysearch/json?")
    fun searchNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("key") key: String
    ): Single<PlaceResponseList>
}