package com.example.smartattendance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class StudentHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // ✅ Create Notification Channel (Android 8+)
        NotificationHelper.createChannel(this);

        // ✅ Ask permission (Android 13+)
        requestNotificationPermissionIfNeeded();

        Button btnScan = findViewById(R.id.btnScanQR);
        Button btnHistory = findViewById(R.id.btnHistory);
        ImageView btnProfile = findViewById(R.id.btnProfile);

        btnScan.setOnClickListener(v ->
                startActivity(new Intent(this, StudentScanActivity.class))
        );

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, ActivityStudentHistory.class))
        );

        // Profile
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(StudentHomeActivity.this, ActivityProfile.class);
            startActivity(intent);
        });

    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            }
        }
    }

    private void sendTestNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher) // replace later with ic_notification
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }
}
