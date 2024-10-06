package uk.akane.omni.ui.fragments.settings

import android.os.Bundle
import androidx.preference.Preference
import uk.akane.omni.R
import uk.akane.omni.ui.fragments.BasePreferenceFragment
import uk.akane.omni.ui.fragments.BaseSettingFragment

class MainSettingsFragment : BaseSettingFragment(
    R.string.settings,
    { MainSettingsTopFragment() })

class MainSettingsTopFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_top, rootKey)
        val versionPrefs = findPreference<Preference>("version")
        versionPrefs!!.summary = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
    }

}