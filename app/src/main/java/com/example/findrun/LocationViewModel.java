package com.example.findrun;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.findrun.LocationLiveData;

public class LocationViewModel extends AndroidViewModel {
    private final LocationLiveData locationLiveData;

    public LocationViewModel(@NonNull Application application) {
        super(application);
        locationLiveData = new LocationLiveData(application);
    }

    public LiveData<Location> getLocationLiveData() {
        return locationLiveData;
    }
}
