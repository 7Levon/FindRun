package com.example.findrun;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationLiveData extends LiveData<Location> {
    private static final long UPDATE_INTERVAL = 1000; // 1 second
    private static final long FASTEST_INTERVAL = 500; // 0.5 second
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationCallback locationCallback;

    public LocationLiveData(Application application) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    setLocationData(locationResult.getLastLocation());
                }
            }
        };
    }

    private void setLocationData(Location location) {
        setValue(location);
    }

    @Override
    protected void onActive() {
        super.onActive();
        startLocationUpdates();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(UPDATE_INTERVAL);
        request.setFastestInterval(FASTEST_INTERVAL);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
