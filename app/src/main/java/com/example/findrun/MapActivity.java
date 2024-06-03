package com.example.findrun;

import static com.example.findrun.Constants.MAPVIEW_BUNDLE_KEY;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.findrun.TrackingState;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final long INACTIVE_THRESHOLD = 5 * 60 * 1000;
    private static final String TAG = "MapActivity";

    private MapView mMapView;
    private GoogleMap mMap;
    private DatabaseReference mUserLocationsRef;
    private FirebaseAuth mAuth;
    private Map<String, Marker> mMarkers = new HashMap<>();
    private Map<Marker, String> markerUserMap = new HashMap<>();
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Button startButton;
    private ImageView stopButton;
    private List<LatLng> userPath = new ArrayList<>();
    private Polyline userPolyline;
    private TextView timerTextView;
    private TextView distanceTextView;
    private Handler timerHandler;
    private long timeInMilliseconds = 0L;
    private boolean isInitialZoomDone = false;
    private Handler statusHandler;
    private Runnable statusRunnable;
    private static final long STATUS_UPDATE_INTERVAL = 5000;
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d(TAG, "onCreate called");
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        mMapView = findViewById(R.id.user_list_map);

        if (mMapView != null) {
            if (savedInstanceState != null) {
                mMapView.onCreate(savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY));
            } else {
                mMapView.onCreate(null);
            }
            mMapView.getMapAsync(this);
        } else {
            Log.e(TAG, "MapView is null");
        }

        initializeViews();
        initializeFirebase();
        setupLocationUpdates();

        statusHandler = new Handler();
        statusRunnable = new Runnable() {
            @Override
            public void run() {
                updateUserStatus();
                statusHandler.postDelayed(this, STATUS_UPDATE_INTERVAL);
            }
        };
        statusHandler.post(statusRunnable);
    }

    private void initializeViews() {
        startButton = findViewById(R.id.start);
        stopButton = findViewById(R.id.stop);
        timerTextView = findViewById(R.id.timer);
        distanceTextView = findViewById(R.id.distanceTextView);

        timerHandler = new Handler();

        startButton.setOnClickListener(v -> startTracking());
        stopButton.setOnClickListener(v -> stopTracking());

        Log.d(TAG, "Views initialized");
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUserLocationsRef = FirebaseDatabase.getInstance().getReference("User Locations");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Log.d(TAG, "Firebase initialized");
    }

    private void updateUserStatus() {
        String currentUserUid = mAuth.getCurrentUser().getUid();
        if (currentUserUid != null) {
            try {
                mUserLocationsRef.child(currentUserUid).child("isActive").setValue(true);
                mUserLocationsRef.child(currentUserUid).child("lastUpdated").setValue(System.currentTimeMillis());
                Log.d(TAG, "User status updated to active");
            } catch (Exception e) {
                Log.e(TAG, "Error updating user status: " + e.getMessage());
            }
        }
    }

    private void updateUserStatusInactive() {
        String currentUserUid = mAuth.getCurrentUser().getUid();
        if (currentUserUid != null) {
            try {
                mUserLocationsRef.child(currentUserUid).child("isActive").setValue(false);
                Log.d(TAG, "User status updated to inactive");
            } catch (Exception e) {
                Log.e(TAG, "Error updating user status: " + e.getMessage());
            }
        }
    }

    private void setupLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (android.location.Location location : locationResult.getLocations()) {
                        updateMyLocation(location);
                    }
                }
            }
        };

        Log.d(TAG, "Location updates set up");
    }



    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (TrackingState.getInstance().isTracking()) {
                long startTime = TrackingState.getInstance().getStartTime();
                timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
                TrackingState.getInstance().setTimeInMilliseconds(timeInMilliseconds);

                int secs = (int) (timeInMilliseconds / 1000);
                int mins = secs / 60;
                int hours = mins / 60;
                secs = secs % 60;
                timerTextView.setText(String.format("%02d:%02d:%02d", hours, mins, secs));
                timerHandler.postDelayed(this, 1000);

                Log.d(TAG, "Timer updated: " + String.format("%02d:%02d:%02d", hours, mins, secs));
            }
        }
    };

    private void startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            TrackingState.getInstance().setTracking(true);
            TrackingState.getInstance().setStartTime(SystemClock.uptimeMillis());
            timerHandler.postDelayed(updateTimerThread, 0);
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);

            Log.d(TAG, "Tracking started");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void stopTracking() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
        if (userPolyline != null) {
            userPolyline.remove();
        }
        TrackingState.getInstance().getUserPath().clear();
        TrackingState.getInstance().resetDistance();
        TrackingState.getInstance().setTimeInMilliseconds(0L);

        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);

        TrackingState.getInstance().setTracking(false);
        timerHandler.removeCallbacks(updateTimerThread);
        timerTextView.setText("00:00:00");
        distanceTextView.setText("0.0 m");

        Log.d(TAG, "Tracking stopped");
    }


    private void updateMyLocation(android.location.Location location) {
        if (mMap != null && location != null) {
            if (location.getAccuracy() > 20) {
                return;
            }

            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            myLatLng = smoothLocation(myLatLng);

            // Update user location without adding or updating the current user's marker
            if (TrackingState.getInstance().isTracking()) {
                List<LatLng> path = TrackingState.getInstance().getUserPath();
                if (path.size() > 0) {
                    LatLng lastLatLng = path.get(path.size() - 1);
                    float[] results = new float[1];
                    android.location.Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, myLatLng.latitude, myLatLng.longitude, results);
                    float distance = results[0];
                    TrackingState.getInstance().addDistance(distance);
                }

                path.add(myLatLng);
                TrackingState.getInstance().setUserPath(path);
                runOnUiThread(() -> {
                    if (userPolyline != null) {
                        userPolyline.setPoints(path);
                    } else {
                        userPolyline = mMap.addPolyline(new PolylineOptions()
                                .addAll(path)
                                .width(10)
                                .color(ContextCompat.getColor(MapActivity.this, R.color.dark_blue)));
                    }
                    updateDistanceUI();
                });
            }

            if (!isInitialZoomDone) {
                LatLng finalMyLatLng = myLatLng;
                runOnUiThread(() -> {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(finalMyLatLng, 15));
                    isInitialZoomDone = true;
                });
            }

            Log.d(TAG, "Location updated: " + myLatLng.toString());
        }
    }



    private void updateDistanceUI() {
        runOnUiThread(() -> {
            float totalDistance = TrackingState.getInstance().getTotalDistance();
            distanceTextView.setText(String.format("%.2f m", totalDistance));
        });
    }
    private void updatePolyline() {
        if (mMap != null && TrackingState.getInstance().getUserPath() != null) {
            if (userPolyline != null) {
                userPolyline.remove();
            }
            userPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(TrackingState.getInstance().getUserPath())
                    .width(10)
                    .color(ContextCompat.getColor(this, R.color.dark_blue)));
        }
    }


    private LatLng smoothLocation(LatLng newLocation) {
        List<LatLng> path = TrackingState.getInstance().getUserPath();
        if (path.size() < 2) {
            return newLocation;
        }

        LatLng lastLocation = path.get(path.size() - 1);
        double distance = Math.sqrt(Math.pow(newLocation.latitude - lastLocation.latitude, 2) +
                Math.pow(newLocation.longitude - lastLocation.longitude, 2));

        if (distance > 0.0001) {
            double smoothedLat = lastLocation.latitude + 0.2 * (newLocation.latitude - lastLocation.latitude);
            double smoothedLng = lastLocation.longitude + 0.2 * (newLocation.longitude - lastLocation.longitude);
            return new LatLng(smoothedLat, smoothedLng);
        } else {
            return lastLocation;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
        setupUserMarkers();

        mMap.setOnMarkerClickListener(marker -> {
            String userId = markerUserMap.get(marker);
            if (userId != null && !userId.equals(mAuth.getCurrentUser().getUid())) {
                openChatWindow(userId);
                return true;
            }
            return false;
        });

        Log.d(TAG, "Map is ready");
        updatePolyline();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission needed to show current location.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
        Log.d(TAG, "Activity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
        statusHandler.removeCallbacks(statusRunnable);
        Log.d(TAG, "Activity stopped");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        mFusedLocationClient.removeLocationUpdates(locationCallback);
        updateUserStatusInactive();

        // Save state
        SharedPreferences preferences = getSharedPreferences("FindRunPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("timeInMilliseconds", TrackingState.getInstance().getTimeInMilliseconds());
        editor.putFloat("totalDistance", TrackingState.getInstance().getTotalDistance());
        editor.putString("userPath", new Gson().toJson(TrackingState.getInstance().getUserPath()));
        editor.putBoolean("isTracking", TrackingState.getInstance().isTracking());
        editor.apply();

        Log.d(TAG, "Activity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDestroyed = false;
        mMapView.onResume();
        statusHandler.post(statusRunnable);
        updateUserStatus();

        if (mMap == null) {
            mMapView.getMapAsync(this);
        } else {
            updatePolyline();
        }

        SharedPreferences preferences = getSharedPreferences("FindRunPrefs", MODE_PRIVATE);
        long savedTimeInMilliseconds = preferences.getLong("timeInMilliseconds", 0L);
        float savedTotalDistance = preferences.getFloat("totalDistance", 0f);
        String userPathJson = preferences.getString("userPath", "[]");
        boolean isTracking = preferences.getBoolean("isTracking", false);
        Type listType = new TypeToken<ArrayList<LatLng>>() {}.getType();
        List<LatLng> savedUserPath = new Gson().fromJson(userPathJson, listType);

        TrackingState.getInstance().setTimeInMilliseconds(savedTimeInMilliseconds);
        TrackingState.getInstance().setTotalDistance(savedTotalDistance);
        TrackingState.getInstance().setUserPath(savedUserPath);
        TrackingState.getInstance().setTracking(isTracking);

        if (isTracking) {
            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
            timerHandler.postDelayed(updateTimerThread, 0);
        } else {
            startButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.GONE);
        }

        updateUI();

        Log.d(TAG, "Activity resumed");
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        statusHandler.removeCallbacks(statusRunnable);
        updateUserStatusInactive();
        Log.d(TAG, "Activity destroyed");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
        Log.d(TAG, "Low memory warning");
    }

    private void openChatWindow(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users user = snapshot.getValue(Users.class);
                if (user != null && !isDestroyed) {
                    String currentUserUid = mAuth.getCurrentUser().getUid();
                    initiateChat(currentUserUid, userId);

                    Intent intent = new Intent(MapActivity.this, chatwindo.class);
                    intent.putExtra("uid", userId);
                    intent.putExtra("nameeee", user.getUserName());
                    intent.putExtra("reciverImg", user.getProfilepic());
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUserMarkers() {
        mUserLocationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long currentTime = System.currentTimeMillis();
                String currentUserId = mAuth.getCurrentUser().getUid();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserLocation location = snapshot.getValue(UserLocation.class);
                    String userId = snapshot.getKey();
                    if (location != null && userId != null) {
                        Boolean isActive = snapshot.child("isActive").getValue(Boolean.class);
                        Long lastUpdated = snapshot.child("lastUpdated").getValue(Long.class);
                        if (isActive != null && isActive && (lastUpdated != null && (currentTime - lastUpdated) <= INACTIVE_THRESHOLD)) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                            // Skip adding the current user's marker
                            if (!userId.equals(currentUserId)) {
                                if (!mMarkers.containsKey(userId)) {
                                    addCustomMarker(userId, latLng);
                                } else {
                                    Marker marker = mMarkers.get(userId);
                                    runOnUiThread(() -> marker.setPosition(latLng));
                                }
                            }
                        } else {
                            Marker marker = mMarkers.remove(userId);
                            if (marker != null) {
                                markerUserMap.remove(marker);
                                runOnUiThread(marker::remove);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read user locations: " + databaseError.toException());
            }
        });
    }


    private void addCustomMarker(String userId, LatLng latLng) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users user = snapshot.getValue(Users.class);
                if (user != null && !isDestroyed) {
                    Glide.with(MapActivity.this)
                            .asBitmap()
                            .load(user.getProfilepic())
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    if (!isDestroyed) {
                                        runOnUiThread(() -> {
                                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                    .position(latLng)
                                                    .title("User: " + user.getUserName())
                                                    .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(resource))));
                                            mMarkers.put(userId, marker);
                                            markerUserMap.put(marker, userId);
                                        });
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user data: " + error.getMessage());
            }
        });
    }

    private Bitmap getMarkerBitmapFromView(Bitmap bitmap) {
        Bitmap circularBitmap = getCircularBitmap(bitmap);

        View customMarkerView = getLayoutInflater().inflate(R.layout.custom_marker, null);
        ImageView markerImageView = customMarkerView.findViewById(R.id.profile_image);
        markerImageView.setImageBitmap(circularBitmap);
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private void initiateChat(String currentUserId, String otherUserId) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        chatsRef.child(currentUserId).child(otherUserId).setValue(true);
        chatsRef.child(otherUserId).child(currentUserId).setValue(true);
    }

    private void updateUI() {
        updatePolyline();
        updateTimerUI();
    }



    private void updateTimerUI() {
        long timeInMilliseconds = TrackingState.getInstance().getTimeInMilliseconds();
        int secs = (int) (timeInMilliseconds / 1000);
        int mins = secs / 60;
        int hours = mins / 60;
        secs = secs % 60;
        timerTextView.setText(String.format("%02d:%02d:%02d", hours, mins, secs));
    }
}