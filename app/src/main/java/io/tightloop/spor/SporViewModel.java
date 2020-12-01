package io.tightloop.spor;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SporViewModel extends ViewModel {

    public static class LocationData {
        final double lat;
        final double lng;
        final double alt;
        final long distanceInCm;
        final double speedInMetersPerSecond;
        final long durationNano;

        LocationData(double lat, double lng, double alt, long distanceInCm, double speedInMetersPerSecond, long durationNano) {
            this.lat = lat;
            this.lng = lng;
            this.alt = alt;
            this.distanceInCm = distanceInCm;
            this.speedInMetersPerSecond = speedInMetersPerSecond;
            this.durationNano = durationNano;
        }
    }

    private MutableLiveData<LocationData> locationData = new MutableLiveData<>();
    private MutableLiveData<Boolean> sporing = new MutableLiveData<>();

    public MutableLiveData<LocationData> getLocationData() {
        return locationData;
    }

    public void setLocationData(LocationData locationData) {
        this.locationData.setValue(locationData);
    }

    public MutableLiveData<Boolean> getSporingState() {
        return sporing;
    }

    public void setSporingState(boolean sporingState) {
        this.sporing.setValue(sporingState);
    }
}
