package com.example.e2e4test.presentation.model

sealed class State {
    object Loading : State()
    class Error(val message: String) : State()
    object Content : State()
    object Empty : State()
}
