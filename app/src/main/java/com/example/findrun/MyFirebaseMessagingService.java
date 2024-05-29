package com.example.findrun;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM";
    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper(getApplicationContext());
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Log incoming message to see its structure
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Handle data messages
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().getOrDefault("title", "Notification");
            String message = remoteMessage.getData().getOrDefault("message", "You've got a new message!");

            // Show notification
            notificationHelper.showNotification(title, message);
        }
    }

    @Override
    public void onNewToken(String token) {
        // Update the new token for this device as needed
        Log.d(TAG, "New Token: " + token);
        // Optionally, you can send this token to your server for future targeting
    }
}
