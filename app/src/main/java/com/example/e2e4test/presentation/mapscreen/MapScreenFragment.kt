package com.example.e2e4test.presentation.mapscreen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.e2e4test.BuildConfig
import com.example.e2e4test.R
import com.example.e2e4test.TestApplication
import com.example.e2e4test.databinding.FragmentMapScreenBinding
import com.example.e2e4test.domain.model.PlaceModel
import com.example.e2e4test.presentation.model.PlacesViewModel
import com.example.e2e4test.presentation.model.State
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Provider

class MapScreenFragment :
    MvpAppCompatFragment(R.layout.fragment_map_screen),
    OnMapReadyCallback,
    MapScreenView {

    @Inject
    lateinit var presenterProvider: Provider<MapScreenPresenter>
    private val presenter: MapScreenPresenter by moxyPresenter { presenterProvider.get() }

    private var _binding: FragmentMapScreenBinding? = null
    private val binding get() = _binding!!

    private var viewModelDisposable: Disposable? = null

    private lateinit var map: MapboxMap
    private lateinit var locationEngine: LocationEngine
    private var markerViewManager: MarkerViewManager? = null

    // TODO: 8/6/21 Разобраться с permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                binding.mapView.getMapAsync(this)
                Toast.makeText(context, getString(R.string.permission_granted), Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(context, getString(R.string.permission_denied), Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private val locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            presenter.onLocationUpdated(
                result!!.lastLocation?.latitude,
                result.lastLocation?.longitude
            )
            Log.d(TAG, "locationEngineCallback.onSuccess: ${result.lastLocation.toString()}")
        }

        override fun onFailure(exception: Exception) {
            Log.e(TAG, "locationEngineCallback.onFailure: ", exception)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        setupMap()
        Log.d(TAG, "onMapReady")
    }

    private fun setupMap() {
        if (requestLocationAccess()) {
            map.setStyle(Style.OUTDOORS) {
                initLocationEngine()
                enableLocationComponent(it)
                updateCamera()
                subscribeToViewModel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(requireContext())
        val request = LocationEngineRequest.Builder(LOCATION_UPDATE_INTERVAL)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(MAX_WAIT_TIME)
            .build()
        locationEngine.requestLocationUpdates(request, locationEngineCallback, getMainLooper())
        locationEngine.getLastLocation(locationEngineCallback)
        Log.d(TAG, "initLocationEngine")
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(mapStyle: Style) {
        val locationComponent = map.locationComponent
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(requireContext(), mapStyle).build()
        )
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS
        Log.d(TAG, "enableLocationComponent")
    }

    @SuppressLint("MissingPermission")
    private fun updateCamera() {
        val location = map.locationComponent.lastKnownLocation!!
        val position = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(CAMERA_ZOOM)
            .bearing(0.0)
            .tilt(0.0)
            .build()
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(position),
            CAMERA_ANIMATE_DURATION
        )
        Log.d(TAG, "updateCamera")
    }

    private fun updateMarkers(placesViewModel: PlacesViewModel) {
        with(placesViewModel) {
            binding.loading.isVisible = (state is State.Loading)
            when (state) {
                is State.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.ok)) { }
                        .show()
                    Log.e(TAG, "updateMarkers.Error: ${state.message}")
                }
                is State.Empty -> {
                    Toast.makeText(context, getString(R.string.empty), Toast.LENGTH_SHORT)
                        .show()
                    Log.d(TAG, "updateMarkers.Empty")
                }
                is State.Content -> {
                    map.addOnCameraIdleListener { addMarkers(places) }
                    Log.d(TAG, "updateMarkers.Content")
                }
                is State.Loading -> {
                    Log.d(TAG, "updateMarkers.Loading")
                }
            }
        }
    }

    private fun addMarkers(places: List<PlaceModel>) {
        if (markerViewManager == null) {
            markerViewManager = MarkerViewManager(binding.mapView, map)
        }
        markerViewManager?.let {
            places.forEach { place ->
                val markerIcon = CustomMarkerView().create(requireContext()) {
                    Toast.makeText(context, place.name, Toast.LENGTH_SHORT)
                        .show()
                }
                it.addMarker(MarkerView(LatLng(place.latitude, place.longitude), markerIcon))
            }
        }
    }

    // TODO: 8/7/21 subscribe to viewModel after view recreated and location permission granted
    private fun subscribeToViewModel() {
        if (viewModelDisposable == null) {
            viewModelDisposable = presenter
                .observePlacesViewModel()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { Log.d(TAG, "subscribeToViewModel.doOnSubscribe") }
                .subscribe(
                    { placesViewModel ->
                        Log.d(TAG,
                            "subscribeToViewModel.doOnNext: Marker names = ${
                                placesViewModel.places.joinToString(
                                    "\n",
                                    "\n"
                                ) { it.name }
                            }")
                        updateMarkers(placesViewModel)
                    },
                    { Log.e(TAG, "subscribeToViewModel.doOnError:", it) }
                )
        }
    }

    private fun unsubscribeFromViewModel() {
        viewModelDisposable?.dispose()
        Log.d(TAG, "unsubscribeFromViewModel")
    }

    private fun requestLocationAccess(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED -> {
                return true
            }
            shouldShowRequestPermissionRationale(LOCATION_PERMISSION) -> {
                showPermissionRationale()
                return false
            }
            !shouldShowRequestPermissionRationale(LOCATION_PERMISSION) -> {
                Toast.makeText(context, getString(R.string.do_not_ask), Toast.LENGTH_LONG).show()
                return false
            }
            else -> {
                requestPermissionLauncher.launch(LOCATION_PERMISSION)
                return false
            }
        }
    }

    private fun showPermissionRationale() {
        Snackbar.make(
            binding.root,
            getString(R.string.permission_required),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(R.string.ok)) { requestPermissionLauncher.launch(LOCATION_PERMISSION) }
            .show()
    }

    private fun initListeners() {
        binding.myLocationButton.setOnClickListener {
            if (!map.locationComponent.isLocationComponentActivated) {
                setupMap()
            } else {
                if (requestLocationAccess()) {
                    updateCamera()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireContext(), BuildConfig.MAPBOX_API_TOKEN)
        Log.d(TAG, "onCreate")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        TestApplication.instance.appComponent?.injectFragment(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapScreenBinding.inflate(inflater, container, false)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        Log.d(TAG, "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        Log.d(TAG, "onViewCreated")
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        unsubscribeFromViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationEngine.removeLocationUpdates(locationEngineCallback)
        markerViewManager = null
        binding.mapView.onDestroy()
        Log.d(TAG, "onDestroyView")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val TAG = "MapScreenFragment"
        private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val CAMERA_ZOOM = 15.0
        private const val CAMERA_ANIMATE_DURATION = 3000
        private const val LOCATION_UPDATE_INTERVAL = 1000L
        private const val MAX_WAIT_TIME = LOCATION_UPDATE_INTERVAL * 5
    }
}