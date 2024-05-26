package uk.akane.omni.ui.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import uk.akane.omni.R
import uk.akane.omni.ui.MainActivity
import uk.akane.omni.ui.components.CompassView
import kotlin.math.abs

class CompassFragment : Fragment(), SensorEventListener {

    private var mainActivity: MainActivity? = null

    private var sensorManager: SensorManager? = null
    // private var locationManager: LocationManager? = null

    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private var lastDegree = 0f

    private lateinit var compassView: CompassView
    private lateinit var textIndicatorTextView: TextView
    private lateinit var sheetMaterialButton: MaterialButton
    private lateinit var settingsMaterialButton: MaterialButton
    // private lateinit var latitudeTextView: TextView
    // private lateinit var longitudeTextView: TextView

    private lateinit var directionStringList: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        sensorManager = mainActivity!!.fetchSensorManager()
        // locationManager = mainActivity!!.fetchLocationManager()
        gyroscope = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onResume() {
        super.onResume()

        val rotationVectorSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val magneticFieldSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager!!.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager!!.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL)

        /*
        if (mainActivity!!.isLocationPermissionGranted()) {
            try {
                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, this)

                val lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
         */
    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
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
        // latitudeTextView = rootView.findViewById(R.id.latitude)
        // longitudeTextView = rootView.findViewById(R.id.longitude)

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

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            sheetMaterialButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + sheetMaterialButton.marginBottom
            }

            settingsMaterialButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + settingsMaterialButton.marginBottom
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
    }

    private fun updateCompass(event: SensorEvent) {
        val rotationVector = floatArrayOf(event.values[0], event.values[1], event.values[2])

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val displayRotation = getDisplayCompat()?.rotation
        val remappedRotationMatrix = remapRotationMatrix(rotationMatrix, displayRotation)

        val orientationInRadians = FloatArray(3)
        SensorManager.getOrientation(remappedRotationMatrix, orientationInRadians)

        val azimuthInRadians = orientationInRadians[0]
        val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

        val adjustedAzimuth = if (azimuthInDegrees < 0) 360f + azimuthInDegrees else azimuthInDegrees
        if (lastDegree == 0f) lastDegree = adjustedAzimuth

        updateCompassViewWithAzimuth(adjustedAzimuth)
    }

    private fun getDisplayCompat(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireContext().display
        } else {
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay
        }
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
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            lastDegree = degree
        }
    }

    // override fun onLocationChanged(location: Location) {
    //     longitudeTextView.text = location.longitude.toString()
    //     latitudeTextView.text = location.latitude.toString()
    // }

}