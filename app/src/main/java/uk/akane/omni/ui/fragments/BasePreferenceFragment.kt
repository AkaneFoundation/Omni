package uk.akane.omni.ui.fragments

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uk.akane.omni.R
import uk.akane.omni.logic.allowDiskAccessInStrictMode
import uk.akane.omni.logic.dpToPx
import uk.akane.omni.logic.enableEdgeToEdgePaddingListener

/**
 * BasePreferenceFragment:
 *   A base fragment for all SettingsTopFragment. It
 * is used to make overlapping color easier.
 *
 * @author AkaneTan
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(MaterialColors.getColor(view, android.R.attr.colorBackground))
        view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view).apply {
            setPadding(paddingLeft, paddingTop + 12.dpToPx(context), paddingRight, paddingBottom)
            enableEdgeToEdgePaddingListener()
        }
    }

    override fun setPreferencesFromResource(preferencesResId: Int, key: String?) {
        allowDiskAccessInStrictMode { super.setPreferencesFromResource(preferencesResId, key) }
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(ColorDrawable(Color.TRANSPARENT))
    }

    override fun setDividerHeight(height: Int) {
        super.setDividerHeight(0)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        // Work around b/331383944: PreferenceFragmentCompat permanently mutates activity theme (enables vertical scrollbars)
        requireContext().theme.applyStyle(R.style.Theme_Omni, true)
        super.onDestroy()
    }

}