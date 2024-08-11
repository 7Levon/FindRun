package com.example.findrun;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class BaseActivity extends AppCompatActivity {
    protected FirebaseAuth mAuth;
    private DatabaseReference mUserLocationsRef;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final String TAG = "BaseActivity";
    private boolean polylineRequestListenerSet = false;
    private boolean mapActivityStarted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.cyan));

        mUserLocationsRef = FirebaseDatabase.getInstance().getReference("User Locations");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUserActive();
        startLocationUpdates();
        if (!polylineRequestListenerSet) {
            listenForPolylineRequests();
            polylineRequestListenerSet = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuth.getCurrentUser() != null) {
            updateUserStatus();
        }
        stopLocationUpdates();
    }

    private void setUserActive() {
        updateUserStatus();
    }

    public void updateUserStatus() {
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

    public void updateUserStatusInactive() {
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
                        saveUserLocation(location);
                    }
                }
            }
        };
    }

    private void saveUserLocation(android.location.Location location) {
        if (location != null && mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            UserLocation userLocation = new UserLocation(location.getLatitude(), location.getLongitude());
            mUserLocationsRef.child(userId).setValue(userLocation);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }
     void updateIsActiveStatus(boolean isActive) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseDatabase.getInstance().getReference("User Locations")
                    .child(currentUser.getUid())
                    .child("isActive")
                    .setValue(isActive)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (!task.isSuccessful()) {
                                Log.e("updateIsActiveStatus", "Failed to update isActive status", task.getException());
                            }
                        }
                    });
        }
    }

    void listenForPolylineRequests() {
        String currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserUid != null) {
            DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference("polyline_requests").child(currentUserUid);
            requestRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean handled = snapshot.child("handled").getValue(Boolean.class);
                        if (handled != null && handled) {
                            Log.d(TAG, "Polyline request already handled");
                            return;
                        }

                        String status = snapshot.child("status").getValue(String.class);
                        String senderUid = snapshot.child("senderUid").getValue(String.class);
                        String receiverUid = snapshot.child("receiverUid").getValue(String.class);

                        Log.d(TAG, "Polyline request status for " + currentUserUid + ": " + status + ", senderUid=" + senderUid);

                        if ("pending".equals(status)) {
                            showPolylineRequestDialog(senderUid);
                        } else if ("accepted".equals(status)) {
                            navigateToMapActivityOnce(senderUid, receiverUid);
                        }
                    } else {
                        Log.d(TAG, "No polyline request data available for " + currentUserUid);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read polyline request: " + error.getMessage());
                }
            });
        }
    }

    private void showPolylineRequestDialog(String senderUid) {
        new AlertDialog.Builder(this)
                .setTitle("Polyline Request")
                .setMessage("User " + senderUid + " wants to draw a polyline. Do you accept?")
                .setPositiveButton("Accept", (dialog, which) -> handlePolylineAcceptance(senderUid, true))
                .setNegativeButton("Reject", (dialog, which) -> handlePolylineAcceptance(senderUid, false))
                .show();
    }

    private void handlePolylineAcceptance(String senderUid, boolean accepted) {
        String currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserUid != null) {
            DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference("polyline_requests").child(currentUserUid);
            if (accepted) {
                requestRef.child("status").setValue("accepted").addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Set handled flag to true to avoid infinite loop
                        requestRef.child("handled").setValue(true);
                        DatabaseReference senderRequestRef = FirebaseDatabase.getInstance().getReference("polyline_requests").child(senderUid);
                        senderRequestRef.child("status").setValue("accepted");
                        senderRequestRef.child("receiverUid").setValue(currentUserUid);
                        senderRequestRef.child("handled").setValue(true);
                        navigateToMapActivityOnce(senderUid, currentUserUid);
                    } else {
                        Log.e(TAG, "Failed to accept polyline request");
                    }
                });
            } else {
                requestRef.child("status").setValue("rejected").addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        requestRef.child("handled").setValue(true);
                        Log.d(TAG, "Polyline request rejected");
                    } else {
                        Log.e(TAG, "Failed to reject polyline request");
                    }
                });
            }
        }
    }

    private void navigateToMapActivityOnce(String senderUid, String receiverUid) {
        if (!mapActivityStarted) {
            mapActivityStarted = true;
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("senderUid", senderUid);
            intent.putExtra("receiverUid", receiverUid);
            startActivity(intent);
        }
    }
}
