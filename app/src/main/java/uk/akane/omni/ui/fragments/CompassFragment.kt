package uk.akane.omni.ui.fragments

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.akane.omni.R
import uk.akane.omni.logic.checkSensorAvailability
import uk.akane.omni.logic.fadInAnimation
import uk.akane.omni.logic.fadOutAnimation
import uk.akane.omni.logic.isLocationPermissionGranted
import uk.akane.omni.logic.setTextAnimation
import uk.akane.omni.ui.MainActivity
import uk.akane.omni.ui.components.CompassView
import uk.akane.omni.ui.fragments.settings.MainSettingsFragment
import uk.akane.omni.ui.viewmodels.OmniViewModel
import java.util.Locale
import kotlin.math.abs


class CompassFragment : BaseFragment(), SensorEventListener, LocationListener {

    private val omniViewModel: OmniViewModel by activityViewModels()

    private var mainActivity: MainActivity? = null

    private var sensorManager: SensorManager? = null
    private var locationManager: LocationManager? = null

    private var rotationVectorSensor: Sensor? = null

    private var lastDegree = 0f

    private lateinit var compassView: CompassView
    private lateinit var textIndicatorTextView: TextView
    private lateinit var sheetMaterialButton: MaterialButton
    private lateinit var settingsMaterialButton: MaterialButton
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var latitudeDescTextView: TextView
    private lateinit var longitudeDescTextView: TextView
    private lateinit var cityTextView: TextView
    private lateinit var geocoder: Geocoder
    private lateinit var notActiveMaterialButton: MaterialButton

    private lateinit var directionStringList: List<String>

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private var isAnimating: Boolean = false
    private var doNotHaveSensor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = requireActivity() as MainActivity

        sensorManager = ContextCompat.getSystemService(requireContext(), SensorManager::class.java)
        locationManager = ContextCompat.getSystemService(requireContext(), LocationManager::class.java)

        rotationVectorSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (!sensorManager!!.checkSensorAvailability(Sensor.TYPE_ROTATION_VECTOR)) {
            mainActivity!!.postComplete()
            doNotHaveSensor = true
        } else {
            sensorManager!!.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }

        geocoder = Geocoder(requireContext(), Locale.getDefault())

        if (requireContext().isLocationPermissionGranted) {
            requestLocationUpdates()
        }

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    requestLocationUpdates()
                    showLongitudeLatitude()
                }
            }

        mutableListOf(1, 2, 3).sort()

    }

    private fun requestLocationUpdates() {
        val providers = locationManager!!.getProviders(true)
        for (provider in providers) {
            try {
                locationManager!!.requestLocationUpdates(provider, 5000, 0f, this)
                val location = locationManager!!.getLastKnownLocation(provider)
                if (location != null) {
                    omniViewModel.setLastKnownLocation(location)
                    return
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        Log.e("CompassFragment", "No valid location provider found")
    }

    private fun updateLocationStatus(location: Location) {
        latitudeTextView.text = String.format(Locale.getDefault(), "%.4f", location.latitude)
        longitudeTextView.text = String.format(Locale.getDefault(), "%.4f", location.longitude)
        updateCity(location)
    }

    override fun onDestroy() {
        sensorManager!!.unregisterListener(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_compass, container, false)

        compassView = rootView.findViewById(R.id.compass_view)
        textIndicatorTextView = rootView.findViewById(R.id.text_indicator)
        sheetMaterialButton = rootView.findViewById(R.id.sheet_btn)
        settingsMaterialButton = rootView.findViewById(R.id.settings_btn)
        latitudeTextView = rootView.findViewById(R.id.latitude)
        longitudeTextView = rootView.findViewById(R.id.longitude)
        latitudeDescTextView = rootView.findViewById(R.id.latitude_desc)
        longitudeDescTextView = rootView.findViewById(R.id.longitude_desc)
        cityTextView = rootView.findViewById(R.id.city)
        notActiveMaterialButton = rootView.findViewById(R.id.not_available_btn)

        directionStringList = listOf(
            getString(R.string.north),
            getString(R.string.northeast),
            getString(R.string.east),
            getString(R.string.southeast),
            getString(R.string.south),
            getString(R.string.southwest),
            getString(R.string.west),
            getString(R.string.northwest)
        )

        notActiveMaterialButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.perm_dialog_title))
                .setMessage(resources.getString(R.string.perm_dialog_text))
                .setIcon(R.drawable.ic_location_on)
                .setNegativeButton(resources.getString(R.string.decline), null)
                .setPositiveButton(resources.getString(R.string.accept)) { _, _ ->
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .show()
        }

        settingsMaterialButton.setOnClickListener {
            mainActivity!!.startFragment(MainSettingsFragment())
        }

        omniViewModel.lastKnownLocation.value?.let {
            updateLocationStatus(it)
            showLongitudeLatitude(false)
        }

        if (doNotHaveSensor) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.warning_dialog_title))
                .setMessage(resources.getString(R.string.warning_dialog_text))
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(resources.getString(R.string.dismiss), null)
                .show()
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            sheetMaterialButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.sprt_btn_marginBottom)
            }

            settingsMaterialButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.sprt_btn_marginBottom)
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> updateCompass(event)
        }
        if (mainActivity?.isInflationStarted() == false) {
            mainActivity!!.postComplete()
        }
    }

    private fun updateCompass(event: SensorEvent) {
        if (!this::latitudeTextView.isInitialized) return
        val rotationVector = floatArrayOf(event.values[0], event.values[1], event.values[2])

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val displayRotation = ContextCompat.getDisplayOrDefault(requireContext()).rotation
        val remappedRotationMatrix = remapRotationMatrix(rotationMatrix, displayRotation)

        val orientationInRadians = FloatArray(3)
        SensorManager.getOrientation(remappedRotationMatrix, orientationInRadians)

        val azimuthInRadians = orientationInRadians[0]
        val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

        val adjustedAzimuth = if (azimuthInDegrees < 0) 360f + azimuthInDegrees else azimuthInDegrees
        if (lastDegree == 0f) lastDegree = adjustedAzimuth

        updateCompassViewWithAzimuth(adjustedAzimuth)
    }

    private fun remapRotationMatrix(rotationMatrix: FloatArray, displayRotation: Int?): FloatArray {
        val (newX, newY) = when (displayRotation) {
            Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
            Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
            Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Y)
        }

        val remappedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(rotationMatrix, newX, newY, remappedRotationMatrix)
        return remappedRotationMatrix
    }

    private fun updateCompassViewWithAzimuth(azimuthInDegrees: Float) {
        compassView.rotate(-azimuthInDegrees)
        updateTextIndicatorWithAzimuth(azimuthInDegrees)
        checkAndVibrate(azimuthInDegrees)
    }

    private fun updateTextIndicatorWithAzimuth(degree: Float) {
        textIndicatorTextView.text = directionStringList[((degree + 22.5f) / 45).toInt() % 8]
    }

    private fun checkAndVibrate(degree: Float) {
        val threshold = 2f

        if (abs(degree - lastDegree) > threshold) {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastDegree = degree
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!this::latitudeTextView.isInitialized) return
        updateLocationStatus(location)
        if (longitudeTextView.visibility != View.VISIBLE && !isAnimating) {
            showLongitudeLatitude(false)
        }
    }

    private fun updateCity(location: Location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            ) { addresses ->
                if (addresses.isNotEmpty()) {
                    cityTextView.setTextAnimation(getLocationString(addresses[0]), fadeInInterpolator = ENTER_INTERPOLATOR, fadeOutInterpolator = EXIT_INTERPOLATOR)
                } else {
                    cityTextView.setTextAnimation(getString(R.string.unknown_location), fadeInInterpolator = ENTER_INTERPOLATOR, fadeOutInterpolator = EXIT_INTERPOLATOR)
                }
            }
        } else {
            fetchCityFallback(location)
        }
    }

    private fun getLocationString(address: Address): String {
        val countryName = address.countryName ?: ""
        val adminArea = address.adminArea ?: ""
        val subAdminArea = address.subAdminArea ?: ""
        val locality = address.locality ?: ""
        val subLocality = address.subLocality ?: ""
        val delimiter = getString(R.string.delimiter)
        val adminAreaDelimiter = if (adminArea.isNotEmpty()) delimiter else ""
        val subAdminAreaDelimiter = if (subAdminArea.isNotEmpty()) delimiter else ""
        val localityDelimiter = if (locality.isNotEmpty()) delimiter else ""
        val subLocalityDelimiter = if (subLocality.isNotEmpty()) delimiter else ""
        
        return getString(
            R.string.location_format,
            subLocality,
            subLocalityDelimiter,
            locality,
            localityDelimiter,
            subAdminArea,
            subAdminAreaDelimiter,
            adminArea,
            adminAreaDelimiter,
            countryName
        )
    }

    private fun fetchCityFallback(location: Location) {
        lifecycle.coroutineScope.launch(Dispatchers.Default) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                cityTextView.setTextAnimation(
                    if (!addresses.isNullOrEmpty()) {
                        getLocationString(addresses[0])
                    } else {
                        getString(R.string.unknown_location)
                    },
                    fadeInInterpolator = ENTER_INTERPOLATOR,
                    fadeOutInterpolator = EXIT_INTERPOLATOR
                )
            } catch (e: Exception) {
                cityTextView.setTextAnimation(getString(R.string.unknown_location), fadeInInterpolator = ENTER_INTERPOLATOR, fadeOutInterpolator = EXIT_INTERPOLATOR)
            }
        }
    }

    private fun showLongitudeLatitude(animate: Boolean = true) {
        if (animate) {
            isAnimating = true
            hideNoPermIndicator(animate = true) {
                longitudeDescTextView.fadInAnimation(interpolator = ENTER_INTERPOLATOR)
                latitudeDescTextView.fadInAnimation(interpolator = ENTER_INTERPOLATOR)
                longitudeTextView.fadInAnimation(interpolator = ENTER_INTERPOLATOR)
                latitudeTextView.fadInAnimation(interpolator = ENTER_INTERPOLATOR, completion = {
                    isAnimating = false
                })
            }
        } else {
            longitudeDescTextView.visibility = View.VISIBLE
            latitudeDescTextView.visibility = View.VISIBLE
            longitudeTextView.visibility = View.VISIBLE
            latitudeTextView.visibility = View.VISIBLE
            hideNoPermIndicator(false)
        }
    }

    private fun hideNoPermIndicator(animate: Boolean = true, completion: (() -> Unit)? = null) {
        if (animate) {
            notActiveMaterialButton.fadOutAnimation(interpolator = EXIT_INTERPOLATOR) {
                completion?.let {
                    it()
                }
            }
        } else {
            notActiveMaterialButton.visibility = View.GONE
        }
    }

    companion object {
        val ENTER_INTERPOLATOR = PathInterpolator(0f, 0f, 0f, 1f)
        val EXIT_INTERPOLATOR = PathInterpolator(0.3f, 0f, 1f, 1f)
    }

}