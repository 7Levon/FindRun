package com.example.findrun;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class LocationUpdateService extends Service {
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        TrackingState.getInstance().restoreState(this); // Restore state on service start
        setupLocationUpdates();
        startForegroundService();
    }

    private void setupLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        updateMyLocation(location);
                    }
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateMyLocation(Location location) {
        if (location != null && location.getAccuracy() <= 20) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userLocationRef = FirebaseDatabase.getInstance().getReference("User Locations").child(userId);
            userLocationRef.setValue(new UserLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis()));

            // Update polyline and distance
            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            List<LatLng> path = TrackingState.getInstance().getUserPath();
            if (path.size() > 0) {
                LatLng lastLatLng = path.get(path.size() - 1);
                float[] results = new float[1];
                Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, myLatLng.latitude, myLatLng.longitude, results);
                float distance = results[0];
                TrackingState.getInstance().addDistance(distance);
            }
            path.add(myLatLng);
            TrackingState.getInstance().setUserPath(path);
            TrackingState.getInstance().setLastKnownLocation(myLatLng);

            TrackingState.getInstance().saveState(this); // Save state after updating

            // Broadcast location update
            Intent intent = new Intent("LocationUpdate");
            intent.putExtra("location", location);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void startForegroundService() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(channel);
            }
        }
        Intent notificationIntent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FindRun")
                .setContentText("Tracking your location")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
