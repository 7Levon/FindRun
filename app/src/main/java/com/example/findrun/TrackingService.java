package com.example.findrun;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.content.pm.PackageManager;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";
    private static final String CHANNEL_ID = "TrackingServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isRunning;
    private long startTime;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private List<LatLng> polylinePoints;

    @Override
    public void onCreate() {
        super.onCreate();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();
        polylinePoints = new ArrayList<>();
        startTime = 0;
        isRunning = false;
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                updateTimer(time);
                timerHandler.postDelayed(this, 1000);
            }
        };

        createNotificationChannel();
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
                        addPolylinePoint(new LatLng(location.getLatitude(), location.getLongitude()));
                        updateDistance();
                    }
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateMyLocation(Location location) {
        if (location != null && location.getAccuracy() <= 20) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userLocationRef = FirebaseDatabase.getInstance().getReference("User Locations").child(userId);
            userLocationRef.setValue(new UserLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis()));
        }
    }

    private void addPolylinePoint(LatLng point) {
        polylinePoints.add(point);
        // Broadcast new polyline point to activity
        Intent intent = new Intent("POLYLINE_UPDATE");
        intent.putExtra("point", point);
        sendBroadcast(intent);
    }

    private void updateTimer(String time) {
        Intent intent = new Intent("TIMER_UPDATE");
        intent.putExtra("time", time);
        sendBroadcast(intent);
    }

    private void updateDistance() {
        float totalDistance = 0;
        for (int i = 0; i < polylinePoints.size() - 1; i++) {
            Location loc1 = new Location("");
            loc1.setLatitude(polylinePoints.get(i).latitude);
            loc1.setLongitude(polylinePoints.get(i).longitude);
            Location loc2 = new Location("");
            loc2.setLatitude(polylinePoints.get(i + 1).latitude);
            loc2.setLongitude(polylinePoints.get(i + 1).longitude);
            totalDistance += loc1.distanceTo(loc2);
        }
        Intent intent = new Intent("DISTANCE_UPDATE");
        intent.putExtra("distance", totalDistance);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("START_TRACKING")) {
                startTracking();
            } else if (intent.getAction().equals("STOP_TRACKING")) {
                stopTracking();
            }
        }
        return START_STICKY;
    }

    private void startTracking() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
            isRunning = true;
            startForegroundService();
        }
    }

    private void stopTracking() {
        if (isRunning) {
            timerHandler.removeCallbacks(timerRunnable);
            isRunning = false;
            stopForeground(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        TrackingService getService() {
            return TrackingService.this;
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FindRun")
                .setContentText("Tracking your run...")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tracking Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
