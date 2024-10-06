package uk.akane.omni.ui.fragments

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import uk.akane.omni.R
import uk.akane.omni.logic.checkSensorAvailability
import uk.akane.omni.ui.MainActivity
import uk.akane.omni.ui.components.SpiritLevelView
import uk.akane.omni.ui.components.SwitchBottomSheet
import uk.akane.omni.ui.fragments.settings.MainSettingsFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.asin
import kotlin.math.sqrt


class LevelFragment : BaseFragment(), SensorEventListener {

    private var mainActivity: MainActivity? = null

    private var sensorManager: SensorManager? = null

    private var rotationVectorSensor: Sensor? = null

    private lateinit var sheetMaterialButton: MaterialButton
    private lateinit var settingsMaterialButton: MaterialButton

    private lateinit var levelView: SpiritLevelView

    private var doNotHaveSensor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = requireActivity() as MainActivity

        sensorManager = ContextCompat.getSystemService(requireContext(), SensorManager::class.java)

        rotationVectorSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (!sensorManager!!.checkSensorAvailability(Sensor.TYPE_ROTATION_VECTOR)) {
            mainActivity!!.postComplete()
            doNotHaveSensor = true
        } else {
            sensorManager!!.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }

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
        val rootView = inflater.inflate(R.layout.fragment_spirit_level, container, false)

        sheetMaterialButton = rootView.findViewById(R.id.sheet_btn)!!
        settingsMaterialButton = rootView.findViewById(R.id.settings_btn)!!

        levelView = rootView.findViewById(R.id.level_view)!!

        settingsMaterialButton.setOnClickListener {
            mainActivity!!.startFragment(MainSettingsFragment())
        }

        sheetMaterialButton.setOnClickListener {
            SwitchBottomSheet(SwitchBottomSheet.CallFragmentType.SPIRIT_LEVEL).show(parentFragmentManager, "switch_bottom_sheet")
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
        val rotationVector = floatArrayOf(event.values[0], event.values[1], event.values[2])

        val rotationMatrix = FloatArray(16)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val displayRotation = ContextCompat.getDisplayOrDefault(requireContext()).rotation
        val remappedRotationMatrix = remapRotationMatrix(rotationMatrix, displayRotation)

        val orientationInRadians = FloatArray(3)
        SensorManager.getOrientation(remappedRotationMatrix, orientationInRadians)

        val pitchInRadians = Math.toDegrees(orientationInRadians[1].toDouble())
        val rollInRadians = Math.toDegrees(orientationInRadians[2].toDouble())
        var balanceFactor: Float = sqrt(
            remappedRotationMatrix[8] * remappedRotationMatrix[8]
                    + remappedRotationMatrix[9] * remappedRotationMatrix[9]
        )
        balanceFactor = (if (balanceFactor == 0f) 0f else remappedRotationMatrix[8] / balanceFactor)

        val balance = Math.toDegrees(asin(balanceFactor).toDouble()).toFloat()

        levelView.updatePitchAndRollAndBalance(pitchInRadians.toFloat(), rollInRadians.toFloat(), balance)
    }

    private fun remapRotationMatrix(rotationMatrix: FloatArray, displayRotation: Int?): FloatArray {
        val (newX, newY) = when (displayRotation) {
            Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
            Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
            Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Y)
        }

        val remappedRotationMatrix = FloatArray(16)
        SensorManager.remapCoordinateSystem(rotationMatrix, newX, newY, remappedRotationMatrix)
        return remappedRotationMatrix
    }

}