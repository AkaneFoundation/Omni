package uk.akane.omni.ui.fragments

import android.Manifest
import android.content.SharedPreferences
import android.hardware.GeomagneticField
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
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.akane.omni.R
import uk.akane.omni.logic.checkSensorAvailability
import uk.akane.omni.logic.doIHavePermission
import uk.akane.omni.logic.fadInAnimation
import uk.akane.omni.logic.fadOutAnimation
import uk.akane.omni.logic.isLocationPermissionGranted
import uk.akane.omni.logic.setTextAnimation
import uk.akane.omni.ui.MainActivity
import uk.akane.omni.ui.components.CompassView
import uk.akane.omni.ui.components.SwitchBottomSheet
import uk.akane.omni.ui.fragments.settings.MainSettingsFragment
import uk.akane.omni.ui.viewmodels.OmniViewModel
import java.util.Locale
import kotlin.math.abs


class CompassFragment : BaseFragment(), SensorEventListener, LocationListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val omniViewModel: OmniViewModel by activityViewModels()

    private var mainActivity: MainActivity? = null

    private var sensorManager: SensorManager? = null
    private var locationManager: LocationManager? = null

    private var rotationVectorSensor: Sensor? = null

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

    private lateinit var prefs: SharedPreferences

    private var isAnimating: Boolean = false
    private var doNotHaveSensor: Boolean = false
    private var hapticFeedback: Boolean = true
    private var trueNorth: Boolean = false
    private var useDms: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = requireActivity() as MainActivity

        sensorManager = ContextCompat.getSystemService(requireContext(), SensorManager::class.java)
        locationManager = ContextCompat.getSystemService(requireContext(), LocationManager::class.java)

        rotationVectorSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (!sensorManager!!.checkSensorAvailability(Sensor.TYPE_ROTATION_VECTOR)) {
            mainActivity!!.postComplete()
            doNotHaveSensor = true
        }

        geocoder = Geocoder(requireContext(), Locale.getDefault())

        if (requireContext().isLocationPermissionGranted) {
            requestLocationUpdates()
        }

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted && requireContext().isLocationPermissionGranted) {
                    requestLocationUpdates()
                    showLongitudeLatitude()
                }
            }

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.registerOnSharedPreferenceChangeListener(this)

        hapticFeedback = prefs.getBoolean("haptic_feedback", true)
        trueNorth = prefs.getBoolean("true_north", false)
        useDms = prefs.getBoolean("coordinate", false)
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
        if (!useDms) {
            latitudeTextView.text = String.format(Locale.getDefault(), "%.4f", location.latitude)
            longitudeTextView.text = String.format(Locale.getDefault(), "%.4f", location.longitude)
        } else {
            val listLatitudeDMS = Location.convert(location.latitude, Location.FORMAT_SECONDS).split(':')
            val longitudeDMS = Location.convert(location.longitude, Location.FORMAT_SECONDS).split(':')
            val regex = """[.,٫]\d+""".toRegex()
            latitudeTextView.text = getString(
                R.string.dms_format,
                listLatitudeDMS[0],
                listLatitudeDMS[1],
                listLatitudeDMS[2].replace(regex, "")
            )
            longitudeTextView.text = getString(
                R.string.dms_format,
                longitudeDMS[0],
                longitudeDMS[1],
                longitudeDMS[2].replace(regex, "")
            )
        }
        updateCity(location)
    }

    override fun onResume() {
        super.onResume()
        if (sensorManager!!.checkSensorAvailability(Sensor.TYPE_ROTATION_VECTOR)) {
            sensorManager!!.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }


    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_compass, container, false)

        compassView = rootView.findViewById(R.id.compass_view)!!
        textIndicatorTextView = rootView.findViewById(R.id.text_indicator)!!
        sheetMaterialButton = rootView.findViewById(R.id.sheet_btn)!!
        settingsMaterialButton = rootView.findViewById(R.id.settings_btn)!!
        latitudeTextView = rootView.findViewById(R.id.latitude)!!
        longitudeTextView = rootView.findViewById(R.id.longitude)!!
        latitudeDescTextView = rootView.findViewById(R.id.latitude_desc)!!
        longitudeDescTextView = rootView.findViewById(R.id.longitude_desc)!!
        cityTextView = rootView.findViewById(R.id.city)!!
        notActiveMaterialButton = rootView.findViewById(R.id.not_available_btn)!!

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

        sheetMaterialButton.setOnClickListener {
            SwitchBottomSheet(SwitchBottomSheet.CallFragmentType.COMPASS).show(parentFragmentManager, "switch_bottom_sheet")
        }

        omniViewModel.lastKnownLocation.value?.let {
            updateLocationStatus(it)
        }
        if (requireContext().isLocationPermissionGranted) {
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

        if (omniViewModel.lastDegree.value != null) {
            updateCompassViewWithAzimuth(omniViewModel.lastDegree.value!!)
        }

        if (!requireContext().doIHavePermission(Manifest.permission.POST_NOTIFICATIONS) &&
            !prefs.getBoolean("isPostNotificationPromptShown" , false) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.compass_prompt_title))
                .setMessage(resources.getString(R.string.compass_prompt_desc))
                .setIcon(R.drawable.ic_notifications)
                .setNegativeButton(resources.getString(R.string.decline)) { _, _ ->
                    prefs.edit().putBoolean("isPostNotificationPromptShown", true).apply()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { _, _ ->
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    prefs.edit().putBoolean("isPostNotificationPromptShown", true).apply()
                }
                .show()
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

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
        if (omniViewModel.lastDegree.value == null) omniViewModel.setLastDegree(adjustedAzimuth)

        if (trueNorth && omniViewModel.lastKnownLocation.value != null) {
            val magneticDeclination = getMagneticDeclination(omniViewModel.lastKnownLocation.value!!)
            val trueAzimuth = adjustedAzimuth.plus(magneticDeclination)
            updateCompassViewWithAzimuth(trueAzimuth)
        } else {
            updateCompassViewWithAzimuth(adjustedAzimuth)
        }
    }

    private fun getMagneticDeclination(location: Location): Float {
        val latitude = location.latitude.toFloat()
        val longitude = location.longitude.toFloat()
        val altitude = location.altitude.toFloat()
        val time = location.time
        val geomagneticField = GeomagneticField(latitude, longitude, altitude, time)
        return geomagneticField.declination
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
        if (hapticFeedback) {
            checkAndVibrate(azimuthInDegrees)
        }
    }

    private fun updateTextIndicatorWithAzimuth(degree: Float) {
        textIndicatorTextView.text = directionStringList[((degree + 22.5f) / 45).toInt() % 8]
    }

    private fun checkAndVibrate(degree: Float) {
        val threshold = 2f

        if (omniViewModel.lastDegree.value != null &&
            abs(degree - omniViewModel.lastDegree.value!!) > threshold) {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            omniViewModel.setLastDegree(degree)
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "haptic_feedback" -> {
                hapticFeedback = prefs.getBoolean("haptic_feedback", true)
            }
            "true_north" -> {
                trueNorth = prefs.getBoolean("true_north", false)
            }
            "coordinate" -> {
                useDms = prefs.getBoolean("coordinate", false)
            }
        }
    }

}