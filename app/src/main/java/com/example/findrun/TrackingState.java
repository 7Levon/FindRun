package com.example.findrun;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TrackingState {
    private static TrackingState instance;
    private boolean isTracking;
    private long startTime;
    private long timeInMilliseconds;
    private List<LatLng> userPath = new ArrayList<>();
    private float totalDistance;
    private LatLng lastKnownLocation;

    private TrackingState() {}

    public static synchronized TrackingState getInstance() {
        if (instance == null) {
            instance = new TrackingState();
        }
        return instance;
    }

    public boolean isTracking() {
        return isTracking;
    }

    public void setTracking(boolean tracking) {
        isTracking = tracking;
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

    public List<LatLng> getUserPath() {
        return userPath;
    }

    public void setUserPath(List<LatLng> userPath) {
        this.userPath = userPath;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void addDistance(float distance) {
        this.totalDistance += distance;
    }

    public void resetDistance() {
        this.totalDistance = 0;
    }

    public LatLng getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(LatLng lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    // Methods to save and restore state
    public void saveState(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("FindRunPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("timeInMilliseconds", timeInMilliseconds);
        editor.putFloat("totalDistance", totalDistance);
        editor.putString("userPath", new Gson().toJson(userPath));
        editor.putBoolean("isTracking", isTracking);
        editor.apply();
    }

    public void restoreState(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("FindRunPrefs", Context.MODE_PRIVATE);
        timeInMilliseconds = preferences.getLong("timeInMilliseconds", 0L);
        totalDistance = preferences.getFloat("totalDistance", 0f);
        String userPathJson = preferences.getString("userPath", "[]");
        isTracking = preferences.getBoolean("isTracking", false);
        Type listType = new TypeToken<ArrayList<LatLng>>() {}.getType();
        userPath = new Gson().fromJson(userPathJson, listType);
    }
}
