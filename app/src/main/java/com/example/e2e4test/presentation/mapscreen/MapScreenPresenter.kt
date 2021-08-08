package com.example.e2e4test.presentation.mapscreen

import android.util.Log
import com.example.e2e4test.BuildConfig
import com.example.e2e4test.data.network.ApiService
import com.example.e2e4test.data.network.model.asDomain
import com.example.e2e4test.domain.model.PlaceModel
import com.example.e2e4test.presentation.model.PlacesViewModel
import com.example.e2e4test.presentation.model.State
import com.mapbox.mapboxsdk.geometry.LatLng
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import moxy.MvpPresenter
import javax.inject.Inject

class MapScreenPresenter @Inject constructor(private val apiService: ApiService) :
    MvpPresenter<MapScreenView>() {

    private var placesViewModel = PlacesViewModel(State.Loading, emptyList())
    private val placesSubject: BehaviorSubject<PlacesViewModel> = BehaviorSubject.create()

    private var loadPlacesDisposable: Disposable? = null
    private var currentLocation = LatLng()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadPlacesDisposable?.dispose()
    }

    fun observePlacesViewModel(): Observable<PlacesViewModel> = placesSubject

    fun onLocationUpdated(latitude: Double?, longitude: Double?) {
        Log.d(TAG, "onLocationChanged: $latitude, $longitude")
        if (latitude != null && longitude != null) {
            val lastLocation = LatLng(latitude, longitude)
            if (lastLocation.distanceTo(currentLocation) > LOCATION_THRESHOLD_IN_METERS) {
                currentLocation = lastLocation
                loadPlaces(currentLocation)
            }
        }
    }

    private fun loadPlaces(latLng: LatLng) {
        loadPlacesDisposable?.dispose()
        val location = "${latLng.latitude},${latLng.longitude}"
        loadPlacesDisposable = apiService
            .searchNearbyPlaces(location, SEARCH_RADIUS, BuildConfig.PLACES_API_KEY)
            .subscribeOn(Schedulers.io())
            .map { it.results.asDomain() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { places -> updatePlaces(places) },
                { throwable ->
                    updatePlacesViewModel {
                        copy(state = State.Error(throwable.message.toString()))
                    }
                }
            )
    }

    private fun updatePlaces(places: List<PlaceModel>) {
        if (places.isEmpty()) {
            updatePlacesViewModel {
                copy(
                    state = State.Empty,
                    places = emptyList()
                )
            }
        } else {
            updatePlacesViewModel {
                copy(
                    state = State.Content,
                    places = places
                )
            }
        }
    }

    private fun updatePlacesViewModel(mapper: PlacesViewModel.() -> PlacesViewModel = { this }) {
        placesViewModel = placesViewModel.mapper()
        placesSubject.onNext(placesViewModel)
    }

    companion object {
        private const val TAG = "MapScreenPresenter"
        private const val SEARCH_RADIUS = 10000
        private const val LOCATION_THRESHOLD_IN_METERS = 50
    }
}