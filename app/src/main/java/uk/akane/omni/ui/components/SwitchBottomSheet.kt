package uk.akane.omni.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import uk.akane.omni.R
import uk.akane.omni.ui.fragments.CompassFragment
import uk.akane.omni.ui.fragments.FlashlightFragment
import uk.akane.omni.ui.fragments.LevelFragment
import uk.akane.omni.ui.fragments.RulerFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.color.utilities.ColorUtils

class SwitchBottomSheet(
    private val callFragmentType : CallFragmentType
) : BottomSheetDialogFragment() {

    enum class CallFragmentType {
        COMPASS,
        SPIRIT_LEVEL,
        BAROMETER,
        RULER,
        FLASHLIGHT
    }

    private lateinit var compassMaterialButton: MaterialButton
    private lateinit var spiritLevelMaterialButton: MaterialButton
    private lateinit var barometerMaterialButton: MaterialButton
    private lateinit var rulerMaterialButton: MaterialButton
    private lateinit var flashlightMaterialButton: MaterialButton

    private lateinit var targetMaterialButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.switch_bottom_sheet, container, false)

        compassMaterialButton = rootView.findViewById(R.id.compass_btn)!!
        spiritLevelMaterialButton = rootView.findViewById(R.id.spirit_leveler_btn)!!
        barometerMaterialButton = rootView.findViewById(R.id.barometer_btn)!!
        rulerMaterialButton = rootView.findViewById(R.id.ruler_btn)!!
        flashlightMaterialButton = rootView.findViewById(R.id.flashlight_btn)!!

        targetMaterialButton = when (callFragmentType) {
            CallFragmentType.COMPASS -> compassMaterialButton
            CallFragmentType.SPIRIT_LEVEL -> spiritLevelMaterialButton
            CallFragmentType.BAROMETER -> barometerMaterialButton
            CallFragmentType.RULER -> rulerMaterialButton
            CallFragmentType.FLASHLIGHT -> flashlightMaterialButton
        }

        targetMaterialButton.isChecked = true

        setOnClickListener()

        return rootView
    }

    private fun setOnClickListener() {
        compassMaterialButton.setOnClickListener {
            if (targetMaterialButton != compassMaterialButton) {
                val fm = requireActivity().supportFragmentManager
                fm.commit {
                    hide(fm.fragments.last())
                    replace(R.id.container, CompassFragment())
                }
                dismiss()
            }
        }
        spiritLevelMaterialButton.setOnClickListener {
            if (targetMaterialButton != spiritLevelMaterialButton) {
                val fm = requireActivity().supportFragmentManager
                fm.commit {
                    hide(fm.fragments.last())
                    replace(R.id.container, LevelFragment())
                }
                dismiss()
            }
        }
        rulerMaterialButton.setOnClickListener {
            if (targetMaterialButton != rulerMaterialButton) {
                val fm = requireActivity().supportFragmentManager
                fm.commit {
                    hide(fm.fragments.last())
                    replace(R.id.container, RulerFragment())
                }
                dismiss()
            }
        }
        flashlightMaterialButton.setOnClickListener {
            if (targetMaterialButton != flashlightMaterialButton) {
                val fm = requireActivity().supportFragmentManager
                fm.commit {
                    hide(fm.fragments.last())
                    replace(R.id.container, FlashlightFragment())
                }
                dismiss()
            }
        }
    }

}