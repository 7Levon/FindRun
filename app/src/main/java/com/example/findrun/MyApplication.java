package com.example.findrun;

import android.app.Application;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {
    private FirebaseAuth mAuth;
    private DatabaseReference mUserLocationsRef;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        mUserLocationsRef = FirebaseDatabase.getInstance().getReference("User Locations");

        // Set user as active when app is started
        updateUserStatus(true);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Set user as inactive when app is closed
        updateUserStatus(false);
    }

    private void updateUserStatus(boolean isActive) {
        if (mAuth.getCurrentUser() != null) {
            String currentUserUid = mAuth.getCurrentUser().getUid();
            if (currentUserUid != null) {
                mUserLocationsRef.child(currentUserUid).child("isActive").setValue(isActive)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("MyApplication", "User status updated to " + (isActive ? "active" : "inactive"));
                            } else {
                                Log.e("MyApplication", "Failed to update user status");
                            }
                        });
                if (isActive) {
                    mUserLocationsRef.child(currentUserUid).child("lastUpdated").setValue(System.currentTimeMillis());
                }
            }
        }
    }
}
