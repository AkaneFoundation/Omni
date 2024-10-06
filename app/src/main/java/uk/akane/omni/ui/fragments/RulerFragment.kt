package uk.akane.omni.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import uk.akane.omni.R
import uk.akane.omni.ui.MainActivity
import uk.akane.omni.ui.components.SwitchBottomSheet
import uk.akane.omni.ui.fragments.settings.MainSettingsFragment
import com.google.android.material.button.MaterialButton

class RulerFragment : BaseFragment() {

    private lateinit var sheetMaterialButton: MaterialButton
    private lateinit var settingsMaterialButton: MaterialButton
    private lateinit var scaleTopView: View

    private var mainActivity: MainActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = requireActivity() as MainActivity

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_ruler, container, false)

        sheetMaterialButton = rootView.findViewById(R.id.sheet_btn)!!
        settingsMaterialButton = rootView.findViewById(R.id.settings_btn)!!
        scaleTopView = rootView.findViewById(R.id.card_layout_2)!!

        settingsMaterialButton.setOnClickListener {
            mainActivity!!.startFragment(MainSettingsFragment())
        }

        sheetMaterialButton.setOnClickListener {
            SwitchBottomSheet(SwitchBottomSheet.CallFragmentType.RULER).show(parentFragmentManager, "switch_bottom_sheet")
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            settingsMaterialButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.sprt_btn_marginBottom)
            }

            scaleTopView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top + resources.getDimensionPixelSize(R.dimen.ruler_cv_up)
            }

            WindowInsetsCompat.CONSUMED
        }
    }

}