package com.example.smartattendance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Locale;

public class StudentScanActivity extends AppCompatActivity {

    private TextView tvResult;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private DatabaseReference sessionsRef;

    // ✅ We will write to BOTH:
    private DatabaseReference attendanceRef;        // /attendance
    private DatabaseReference attendanceByUserRef;  // /attendance_by_user

    private FusedLocationProviderClient fusedLocationClient;

    private String pendingRawQr = null;

    // Late grace time (minutes)
    private static final int LATE_GRACE_MINUTES = 5;

    // Demo safety switch: set true if you want to skip distance check during demo
    // private static final boolean DEMO_MODE_SKIP_DISTANCE = true;
    private static final boolean DEMO_MODE_SKIP_DISTANCE = false;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                if (fine || coarse) {
                    if (pendingRawQr != null) handleQrAfterPermission(pendingRawQr);
                } else {
                    Toast.makeText(this, "Location permission required for attendance", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }

                String raw = result.getContents();
                tvResult.setText(raw);
                pendingRawQr = raw;

                if (!hasLocationPermission()) {
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                    return;
                }

                handleQrAfterPermission(raw);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_scan);

        tvResult = findViewById(R.id.tvResult);
        Button btnScan = findViewById(R.id.btnScan);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        sessionsRef = FirebaseDatabase.getInstance().getReference("sessions");

        attendanceRef = FirebaseDatabase.getInstance().getReference("attendance");
        attendanceByUserRef = FirebaseDatabase.getInstance().getReference("attendance_by_user");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        btnScan.setOnClickListener(v -> startScan());
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan lecturer QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        barcodeLauncher.launch(options);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void promptEnableLocation() {
        Toast.makeText(this, "Please turn on Location (GPS) for attendance", Toast.LENGTH_LONG).show();
        try {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (Exception ignored) {}
    }

    private void handleQrAfterPermission(String raw) {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Please allow location permission", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isLocationEnabled()) {
            promptEnableLocation();
            return;
        }

        // QR format: sessionId|subject|classId|startTimeMs|durationMinutes|radiusMeters
        String[] parts = raw.split("\\|");
        if (parts.length < 6) {
            Toast.makeText(this, "Invalid QR format", Toast.LENGTH_LONG).show();
            return;
        }

        String sessionId = parts[0];

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            return;
        }

        String studentUid = user.getUid();
        String studentEmail = user.getEmail();

        // ✅ Get a FRESH location (not getLastLocation)
        getFreshLocation(new LocationReadyCallback() {
            @Override
            public void onReady(Location loc) {
                if (loc == null) {
                    Toast.makeText(StudentScanActivity.this,
                            "Location not available yet. Open Google Maps, wait for blue dot, then try again.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                double scanLat = loc.getLatitude();
                double scanLng = loc.getLongitude();

                // 2) Load session from Firebase (truth source)
                sessionsRef.child(sessionId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snap) {
                        Session session = snap.getValue(Session.class);
                        if (session == null) {
                            Toast.makeText(StudentScanActivity.this, "Session not found", Toast.LENGTH_LONG).show();
                            return;
                        }

                        double lecturerLat = session.lecturerLat;
                        double lecturerLng = session.lecturerLng;
                        int radiusMeters = session.radiusMeters;

                        long startTimeMs = session.startTimeMs;
                        int durationMinutes = session.durationMinutes;

                        // ✅ sanity check: if lecturer location not saved properly
                        if (Math.abs(lecturerLat) < 0.0001 && Math.abs(lecturerLng) < 0.0001) {
                            Toast.makeText(StudentScanActivity.this,
                                    "Lecturer location not set. Lecturer please start session again.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        double distanceM = distanceMeters(scanLat, scanLng, lecturerLat, lecturerLng);

                        String finalStatus = calculateFinalStatus(
                                distanceM, radiusMeters,
                                System.currentTimeMillis(), startTimeMs, durationMinutes
                        );

                        // 3) Get student name then save attendance
                        usersRef.child(studentUid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot userSnap) {
                                String studentName = userSnap.child("name").getValue(String.class);
                                if (studentName == null || studentName.trim().isEmpty()) {
                                    studentName = (studentEmail == null) ? "Student" : studentEmail;
                                }

                                long scanTimeMs = System.currentTimeMillis();

                                Attendance att = new Attendance(
                                        studentUid,
                                        sessionId,
                                        studentUid,
                                        studentName,
                                        scanTimeMs,
                                        scanLat,
                                        scanLng,
                                        finalStatus
                                );

                                // ✅ SAVE #1: attendance/session/student
                                attendanceRef.child(sessionId).child(studentUid).setValue(att)
                                        .addOnSuccessListener(unused -> {

                                            // ✅ SAVE #2: attendance_by_user/student/session  (for history page)
                                            attendanceByUserRef.child(studentUid).child(sessionId).setValue(att)
                                                    .addOnSuccessListener(unused2 -> {
                                                        String msg = buildNiceResultMessage(finalStatus, distanceM, radiusMeters,
                                                                startTimeMs, durationMinutes, scanLat, scanLng, lecturerLat, lecturerLng);
                                                        Toast.makeText(StudentScanActivity.this, msg, Toast.LENGTH_LONG).show();
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(StudentScanActivity.this,
                                                            "History save failed: " + e.getMessage(),
                                                            Toast.LENGTH_LONG).show());

                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(StudentScanActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                        );
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(StudentScanActivity.this, "User read failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(StudentScanActivity.this, "Session read failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                Toast.makeText(StudentScanActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ✅ Fresh location helper:
    // 1) Try getCurrentLocation (fast)
    // 2) If null -> request ONE update (more reliable indoors)
    private void getFreshLocation(LocationReadyCallback cb) {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            cb.onReady(loc);
                            return;
                        }
                        requestOneUpdate(cb);
                    })
                    .addOnFailureListener(e -> requestOneUpdate(cb));
        } catch (SecurityException se) {
            cb.onError("Location permission not granted");
        }
    }

    private void requestOneUpdate(LocationReadyCallback cb) {
        try {
            LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(500)
                    .setMaxUpdates(1)
                    .build();

            LocationCallback callback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    fusedLocationClient.removeLocationUpdates(this);
                    Location loc = (locationResult == null) ? null : locationResult.getLastLocation();
                    cb.onReady(loc);
                }
            };

            fusedLocationClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
                    .addOnFailureListener(e -> cb.onError("Failed to request location update: " + e.getMessage()));

        } catch (SecurityException se) {
            cb.onError("Location permission not granted");
        }
    }

    private String calculateFinalStatus(double distanceM, int radiusMeters,
                                        long scanTimeMs, long startTimeMs, int durationMinutes) {

        // 1) GPS check first
        if (!DEMO_MODE_SKIP_DISTANCE && distanceM > radiusMeters) {
            return "rejected_outside";
        }

        // 2) Time check second
        long durationMs = durationMinutes * 60L * 1000L;
        long lateGraceMs = LATE_GRACE_MINUTES * 60L * 1000L;

        long endTimeMs = startTimeMs + durationMs;
        long lateEndMs = endTimeMs + lateGraceMs;

        if (scanTimeMs <= endTimeMs) {
            return "present";
        } else if (scanTimeMs <= lateEndMs) {
            return "late";
        } else {
            return "rejected_late";
        }
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    private String buildNiceResultMessage(String status, double distanceM, int radiusMeters,
                                          long startTimeMs, int durationMinutes,
                                          double scanLat, double scanLng,
                                          double lecLat, double lecLng) {

        long now = System.currentTimeMillis();
        long endTimeMs = startTimeMs + durationMinutes * 60L * 1000L;

        long minutesLeft = (endTimeMs - now) / (60L * 1000L);

        if ("present".equals(status)) {
            return String.format(Locale.getDefault(),
                    "✅ Present\nDistance: %dm / %dm\nTime left: %d min",
                    Math.round(distanceM), radiusMeters, Math.max(minutesLeft, 0));
        }

        if ("late".equals(status)) {
            return String.format(Locale.getDefault(),
                    "🟡 Late\nDistance: %dm / %dm",
                    Math.round(distanceM), radiusMeters);
        }

        if ("rejected_outside".equals(status)) {
            return String.format(Locale.getDefault(),
                    "❌ Rejected (Outside)\nDistance: %dm > %dm",
                    Math.round(distanceM), radiusMeters);
        }

        if ("rejected_late".equals(status)) {
            return "❌ Rejected (Session ended)";
        }

        return "Saved: " + status;
    }

    private interface LocationReadyCallback {
        void onReady(Location loc);
        void onError(String message);
    }
}
