package uk.akane.omni.ui.viewmodels

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OmniViewModel : ViewModel() {
    private val _lastKnownLocation = MutableLiveData<Location?>()
    val lastKnownLocation: LiveData<Location?> get() = _lastKnownLocation

    fun setLastKnownLocation(location: Location) {
        _lastKnownLocation.value = location
    }
}
