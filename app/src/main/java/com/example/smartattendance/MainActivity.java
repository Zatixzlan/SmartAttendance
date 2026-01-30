package com.example.smartattendance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 101;

    private DatabaseReference sessionsRef;
    private EditText etSubject, etClassId, etDuration, etRadius;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        NotificationHelper.createChannel(this);

        sessionsRef = FirebaseDatabase.getInstance().getReference("sessions");

        etSubject = findViewById(R.id.etSubject);
        etClassId = findViewById(R.id.etClassId);
        etDuration = findViewById(R.id.etDuration);
        etRadius = findViewById(R.id.etRadius);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        findViewById(R.id.btnCreateSession).setOnClickListener(v -> {
            // Instead of createSession() directly, get lecturer location first
            createSessionWithLecturerLocation();
        });
    }

    private void createSessionWithLecturerLocation() {
        // 1) Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION
            );
            return;
        }

        // 2) Get last known location (fast)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        createSession(location.getLatitude(), location.getLongitude());
                    } else {
                        // If location is null (sometimes), tell user to turn on GPS
                        Toast.makeText(this,
                                "Cannot get location. Turn on GPS and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void createSession(double lecturerLat, double lecturerLng) {
        String subject = etSubject.getText().toString().trim();
        String classId = etClassId.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String radiusStr = etRadius.getText().toString().trim();

        if (TextUtils.isEmpty(subject)) {
            etSubject.setError("Enter subject");
            etSubject.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(classId)) {
            etClassId.setError("Enter class ID");
            etClassId.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(durationStr)) {
            etDuration.setError("Enter duration (minutes)");
            etDuration.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(radiusStr)) {
            etRadius.setError("Enter radius (meters)");
            etRadius.requestFocus();
            return;
        }

        int durationMinutes;
        int radiusMeters;

        try {
            durationMinutes = Integer.parseInt(durationStr);
            radiusMeters = Integer.parseInt(radiusStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Duration/Radius must be numbers", Toast.LENGTH_LONG).show();
            return;
        }

        if (durationMinutes <= 0) {
            etDuration.setError("Must be > 0");
            etDuration.requestFocus();
            return;
        }
        if (radiusMeters <= 0) {
            etRadius.setError("Must be > 0");
            etRadius.requestFocus();
            return;
        }

        long startTimeMs = System.currentTimeMillis();

        String sessionId = sessionsRef.push().getKey();
        if (sessionId == null) {
            Toast.makeText(this, "Failed to generate session ID", Toast.LENGTH_LONG).show();
            return;
        }

        Session session = new Session(
                sessionId,
                subject,
                classId,
                startTimeMs,
                durationMinutes,
                lecturerLat,
                lecturerLng,
                radiusMeters
        );

        sessionsRef.child(sessionId).setValue(session)
                .addOnSuccessListener(unused -> {

                    // Lecturer reminder 10 minutes before end
                    long endTimeMs = startTimeMs + (durationMinutes * 60L * 1000L);
                    ReminderScheduler.schedule10MinBeforeEnd(
                            MainActivity.this,
                            endTimeMs,
                            "Session ending soon",
                            "10 minutes left. Please remind students to scan attendance.",
                            ("LECT_" + sessionId).hashCode()
                    );

                    String qrText = sessionId + "|" + subject + "|" + classId + "|"
                            + startTimeMs + "|" + durationMinutes + "|" + radiusMeters;

                    String info = "Subject: " + subject +
                            "\nClass: " + classId +
                            "\nSession ID: " + sessionId;

                    Intent i = new Intent(MainActivity.this, LecturerQRActivity.class);
                    i.putExtra(LecturerQRActivity.EXTRA_QR_TEXT, qrText);
                    i.putExtra(LecturerQRActivity.EXTRA_INFO_TEXT, info);
                    startActivity(i);

                    Toast.makeText(MainActivity.this, "Session created with GPS location!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createSessionWithLecturerLocation();
            } else {
                Toast.makeText(this, "Location permission required to create session", Toast.LENGTH_LONG).show();
            }
        }
    }
}
