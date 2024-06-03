package com.example.findrun;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class TrackingState {
    private static TrackingState instance;
    private List<LatLng> userPath = new ArrayList<>();
    private long startTime, timeInMilliseconds = 0L;
    private boolean isTracking = false;
    private float totalDistance = 0f;

    private TrackingState() {}

    public static synchronized TrackingState getInstance() {
        if (instance == null) {
            instance = new TrackingState();
        }
        return instance;
    }

    public List<LatLng> getUserPath() {
        return userPath;
    }

    public void setUserPath(List<LatLng> userPath) {
        this.userPath = userPath;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getTimeInMilliseconds() {
        return timeInMilliseconds;
    }

    public void setTimeInMilliseconds(long timeInMilliseconds) {
        this.timeInMilliseconds = timeInMilliseconds;
    }

    public boolean isTracking() {
        return isTracking;
    }

    public void setTracking(boolean tracking) {
        isTracking = tracking;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public void addDistance(float distance) {
        totalDistance += distance;
    }

    public void resetDistance() {
        totalDistance = 0f;
    }
}
