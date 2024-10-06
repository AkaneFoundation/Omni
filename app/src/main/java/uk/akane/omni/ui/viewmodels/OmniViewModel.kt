package uk.akane.omni.ui.viewmodels

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OmniViewModel : ViewModel() {
    private val _lastKnownLocation = MutableLiveData<Location?>()
    private val _lastDegree = MutableLiveData<Float>()
    val lastKnownLocation: LiveData<Location?> get() = _lastKnownLocation
    val lastDegree: LiveData<Float> get() = _lastDegree

    fun setLastKnownLocation(location: Location) {
        _lastKnownLocation.value = location
    }

    fun setLastDegree(degree: Float) {
        _lastDegree.value = degree
    }
}
