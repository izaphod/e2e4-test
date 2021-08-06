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
import com.example.e2e4test.BuildConfig
import com.example.e2e4test.R
import com.example.e2e4test.TestApplication
import com.example.e2e4test.databinding.FragmentMapScreenBinding
import com.example.e2e4test.presentation.model.PlacesViewModel
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

    private var isLocationGranted = false
    private var shouldShowRationale = false

    // TODO: 8/6/21 Разобраться с permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isLocationGranted = true
                Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                shouldShowRationale = true
                Toast.makeText(context, "Location permission declined", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private val locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            presenter.onLocationUpdated(
                result!!.lastLocation?.latitude,
                result.lastLocation?.longitude
            )
        }
        override fun onFailure(exception: Exception) {
            Log.e(TAG, "locationEngineCallback.onFailure: ", exception)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        Log.d(TAG, "onMapReady")
        map = mapboxMap
        map.setStyle(Style.OUTDOORS) {
            requestLocationAccess()
            if (isLocationGranted) {
                initLocationEngine()
                enableLocationComponent(it)
                updateCamera()
            }
        }
    }

    private fun updateCamera() {
        val location = map.locationComponent.lastKnownLocation!!
        val position = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(12.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000)
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(requireContext())
        val request = LocationEngineRequest.Builder(1000L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(5000L)
            .build()
        locationEngine.requestLocationUpdates(request, locationEngineCallback, getMainLooper())
        locationEngine.getLastLocation(locationEngineCallback)
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
    }

    private fun subscribeToViewModel() {
        if (viewModelDisposable == null) {
            viewModelDisposable = presenter
                .observePlacesViewModel()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { placesViewModel ->
                    Log.d(TAG,
                        "subscribeToViewModel: Marker names = ${
                            placesViewModel.places.joinToString(
                                "\n",
                                "\n"
                            ) { it.name }
                        }")
                    updateMarkers(placesViewModel)
                }
        }
    }

    private fun unsubscribeFromViewModel() {
        viewModelDisposable?.dispose()
    }

    private fun updateMarkers(placesViewModel: PlacesViewModel) {
        map.setStyle(Style.MAPBOX_STREETS) { _ ->
            markerViewManager = MarkerViewManager(binding.mapView, map)
            markerViewManager?.let {
                placesViewModel.places.forEach { placeModel ->
                    val markerIcon = CustomMarkerView().create(requireContext(), placeModel)
                    val markerView = MarkerView(
                        LatLng(placeModel.latitude, placeModel.longitude),
                        markerIcon
                    )
                    it.addMarker(markerView)
                }
                updateCamera()
            }
        }
    }

    private fun requestLocationAccess() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED -> {
                isLocationGranted = true
            }
            shouldShowRequestPermissionRationale(LOCATION_PERMISSION) -> {
                showPermissionRationaleActionSnackbar()
            }
            else -> {
                requestPermissionLauncher.launch(LOCATION_PERMISSION)
            }
        }
    }

    private fun showPermissionRationaleActionSnackbar() {
        Snackbar.make(
            binding.root,
            getString(R.string.permission_required),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(R.string.ok)) {
                requestPermissionLauncher.launch(LOCATION_PERMISSION)
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireContext(), BuildConfig.MAPBOX_API_TOKEN)
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
        subscribeToViewModel()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        subscribeToViewModel()
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
        binding.mapView.onDestroy()
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
    }
}