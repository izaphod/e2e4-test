package com.example.e2e4test.presentation.model

import com.example.e2e4test.domain.model.PlaceModel

data class PlacesViewModel(
    val state: State,
    val places: List<PlaceModel>
)

