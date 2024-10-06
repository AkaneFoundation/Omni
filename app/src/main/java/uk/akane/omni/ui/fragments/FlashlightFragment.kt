package uk.akane.omni.ui.fragments

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import uk.akane.omni.R
import uk.akane.omni.ui.MainActivity
import uk.akane.omni.ui.components.SwitchBottomSheet
import uk.akane.omni.ui.fragments.settings.MainSettingsFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider


class FlashlightFragment : BaseFragment() {

    private lateinit var sheetMaterialButton: MaterialButton
    private lateinit var settingsMaterialButton: MaterialButton
    private lateinit var flashlightSlider: Slider
    private lateinit var cameraManager: CameraManager
    private var keyCameraId: String? = null
    private var maximumFlashlightLevel: Int? = null
    private var notSupported: Boolean = true
    private var isUserTouching: Boolean = false
    private var maximumBrightnessLevelThreshold: Int = 0
    private var pastValue: Float = 0f

    private var mainActivity: MainActivity? = null

    private lateinit var prefs: SharedPreferences

    private var torchListener: CameraManager.TorchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (!isUserTouching && !notSupported &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                maximumFlashlightLevel != null && maximumFlashlightLevel!! > 1) {
                flashlightSlider.value = if (enabled) cameraManager.getTorchStrengthLevel(cameraId)
                    .toFloat() else 0.0f
                if (this@FlashlightFragment::flashlightSlider.isInitialized) {
                    switchTrackColor(flashlightSlider.value)
                }
            } else if (
                !isUserTouching && !notSupported &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                flashlightSlider.value = if (enabled) 1.0f else 0.0f
            }
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            if (!isUserTouching && !notSupported &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                maximumFlashlightLevel != null && maximumFlashlightLevel!! > 1) {
                flashlightSlider.value = cameraManager.getTorchStrengthLevel(cameraId).toFloat()
                if (this@FlashlightFragment::flashlightSlider.isInitialized) {
                    switchTrackColor(flashlightSlider.value)
                }
            } else if (
                !isUserTouching && !notSupported &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                flashlightSlider.value = if (newStrengthLevel == 1) 1.0f else 0.0f
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = requireActivity() as MainActivity
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        cameraManager = ContextCompat.getSystemService(requireContext(), CameraManager::class.java)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (i in 0 until cameraManager.cameraIdList.size) {
                val cameraCharacteristics =
                    cameraManager.getCameraCharacteristics(cameraManager.cameraIdList[i])

                val isFlashlightAvailable =
                    cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                maximumFlashlightLevel =
                    cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)

                if (maximumFlashlightLevel != null && maximumFlashlightLevel!! > 1) {
                    maximumBrightnessLevelThreshold =
                        (maximumFlashlightLevel!! * 0.9).toInt()
                }

                if (isFlashlightAvailable == true && maximumFlashlightLevel != null && maximumFlashlightLevel!! > 0) {
                    keyCameraId = cameraManager.cameraIdList[i]
                    notSupported = false
                    break
                }
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_flashlight, container, false)

        sheetMaterialButton = rootView.findViewById(R.id.sheet_btn)!!
        settingsMaterialButton = rootView.findViewById(R.id.settings_btn)!!

        flashlightSlider = rootView.findViewById(R.id.flashlight_slider)!!

        settingsMaterialButton.setOnClickListener {
            mainActivity!!.startFragment(MainSettingsFragment())
        }

        sheetMaterialButton.setOnClickListener {
            SwitchBottomSheet(SwitchBottomSheet.CallFragmentType.FLASHLIGHT).show(parentFragmentManager, "switch_bottom_sheet")
        }

        if (notSupported) {
            flashlightSlider.isEnabled = false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            flashlightSlider.stepSize = 1.0f
            flashlightSlider.valueTo = maximumFlashlightLevel!!.toFloat()
            flashlightSlider.addOnChangeListener { _, value, _ ->
                if (maximumBrightnessLevelThreshold > 0 &&
                        value >= maximumBrightnessLevelThreshold &&
                        !prefs.getBoolean("flashlight_acknowledged", false)
                    ) {
                    flashlightSlider.isEnabled = false
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(resources.getString(R.string.flashlight_dialog_title))
                        .setMessage(resources.getString(R.string.flashlight_dialog_text))
                        .setIcon(R.drawable.ic_warning)
                        .setNegativeButton(resources.getString(R.string.decline)) { _, _ ->
                            flashlightSlider.isEnabled = true
                            if (pastValue < maximumBrightnessLevelThreshold) {
                                flashlightSlider.value = pastValue
                            } else {
                                flashlightSlider.value = 0f
                            }
                        }
                        .setPositiveButton(resources.getString(R.string.accept)) { _, _ ->
                            flashlightSlider.isEnabled = true
                            prefs.edit()
                                .putBoolean("flashlight_acknowledged", true)
                                .apply()
                            turnOnTorch(value)
                            switchTrackColor(value)
                        }
                        .setOnDismissListener {
                            flashlightSlider.isEnabled = true
                            if (pastValue < maximumBrightnessLevelThreshold &&
                                !prefs.getBoolean("flashlight_acknowledged", false)) {
                                flashlightSlider.value = pastValue
                            } else if (!prefs.getBoolean("flashlight_acknowledged", false)) {
                                flashlightSlider.value = 0f
                            }
                        }
                        .show()
                    return@addOnChangeListener
                }

                switchTrackColor(value)
                turnOnTorch(value)

                pastValue = value
            }
            flashlightSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    isUserTouching = true
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    isUserTouching = false
                }

            })
            cameraManager.registerTorchCallback(torchListener, null)
        }
        return rootView
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun turnOnTorch(value: Float) {
        if (maximumFlashlightLevel == 1) {
            cameraManager.setTorchMode(
                keyCameraId!!,
                if (value >= 0.5f) true else false
            )
        } else if (value >= 0.5){
            cameraManager.turnOnTorchWithStrengthLevel(keyCameraId!!, value.toInt())
        } else {
            cameraManager.setTorchMode(keyCameraId!!, false)
        }
    }

    private fun switchTrackColor(value: Float) {
        if (value >= maximumBrightnessLevelThreshold && maximumBrightnessLevelThreshold > 0) {
            setHighlightTrackColor()
        } else if (maximumBrightnessLevelThreshold > 0) {
            setDefaultTrackColor()
        }
    }

    private fun setHighlightTrackColor() {
        flashlightSlider.thumbTintList =
            ColorStateList.valueOf(
                MaterialColors.getColor(
                    flashlightSlider,
                    com.google.android.material.R.attr.colorError
                )
            )
        flashlightSlider.trackActiveTintList =
            ColorStateList.valueOf(
                MaterialColors.getColor(
                    flashlightSlider,
                    com.google.android.material.R.attr.colorError
                )
            )
    }

    private fun setDefaultTrackColor() {
        flashlightSlider.thumbTintList =
            ColorStateList.valueOf(
                MaterialColors.getColor(
                    flashlightSlider,
                    com.google.android.material.R.attr.colorPrimary
                )
            )
        flashlightSlider.trackActiveTintList =
            ColorStateList.valueOf(
                MaterialColors.getColor(
                    flashlightSlider,
                    com.google.android.material.R.attr.colorPrimary
                )
            )
    }

    override fun onDestroy() {
        cameraManager.unregisterTorchCallback(torchListener)
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            settingsMaterialButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.sprt_btn_marginBottom)
            }

            flashlightSlider.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }
    }

}