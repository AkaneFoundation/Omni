/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package uk.akane.omni.ui.fragments.settings

import android.os.Bundle
import androidx.preference.Preference
import uk.akane.omni.BuildConfig
import uk.akane.omni.R
import uk.akane.omni.ui.fragments.BasePreferenceFragment
import uk.akane.omni.ui.fragments.BaseSettingFragment
import uk.akane.omni.ui.fragments.CompassFragment

class MainSettingsFragment : BaseSettingFragment(
    R.string.settings,
    { MainSettingsTopFragment() })

class MainSettingsTopFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_top, rootKey)
        val versionPrefs = findPreference<Preference>("version")
        versionPrefs!!.summary = BuildConfig.VERSION_NAME
    }

}