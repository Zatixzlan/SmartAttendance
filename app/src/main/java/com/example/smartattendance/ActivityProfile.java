package com.example.smartattendance;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

public class ActivityProfile extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    private ImageView ivProfile;
    private ActivityResultLauncher<Intent> selfieLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        TextView tvUserRole = findViewById(R.id.tvUserRole);

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        MaterialButton btnTakeSelfie = findViewById(R.id.btnTakeSelfie);

        ImageView btnBack = findViewById(R.id.btnBack);
        ivProfile = findViewById(R.id.ivProfile);

        FirebaseUser user = auth.getCurrentUser();

        // If no user, go login
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = user.getUid();

        // Email from FirebaseAuth
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "-");

        // Default values
        tvUserName.setText("User");
        tvUserRole.setText("Role: -");

        // ✅ Load name + role + profilePath from Realtime DB: users/{uid}
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {

                // Name
                String dbName = snap.child("name").getValue(String.class);

                // Role
                String role = snap.child("role").getValue(String.class);

                // Profile image local path
                String profilePath = snap.child("profilePath").getValue(String.class);

                // Fallback name if DB empty
                String fallbackName = user.getDisplayName();
                String finalName = (dbName != null && !dbName.trim().isEmpty())
                        ? dbName
                        : ((fallbackName != null && !fallbackName.trim().isEmpty()) ? fallbackName : "User");

                String finalRole = (role != null && !role.trim().isEmpty())
                        ? role
                        : "student";

                // Beautify role text
                if (finalRole.length() > 1) {
                    finalRole = finalRole.substring(0, 1).toUpperCase()
                            + finalRole.substring(1).toLowerCase();
                } else {
                    finalRole = finalRole.toUpperCase();
                }

                tvUserName.setText(finalName);
                tvUserRole.setText("Role: " + finalRole);

                // ✅ Load profile image from local file (if exists)
                if (profilePath != null && !profilePath.trim().isEmpty()) {
                    File f = new File(profilePath);
                    if (f.exists()) {
                        Bitmap bmp = BitmapFactory.decodeFile(profilePath);
                        if (bmp != null) {
                            ivProfile.setImageBitmap(bmp);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                String fallbackName = user.getDisplayName();
                if (fallbackName == null || fallbackName.trim().isEmpty()) fallbackName = "User";
                tvUserName.setText(fallbackName);
                tvUserRole.setText("Role: -");
            }
        });

        // ✅ Receive selfie result (camera-only)
        selfieLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String path = result.getData().getStringExtra("selfie_path");
                        if (path != null) {

                            // Show immediately
                            Bitmap bmp = BitmapFactory.decodeFile(path);
                            if (bmp != null) {
                                ivProfile.setImageBitmap(bmp);
                            }

                            // ✅ Save the persistent local file path into Realtime DB
                            usersRef.child(uid).child("profilePath").setValue(path);
                        }
                    }
                }
        );

        btnTakeSelfie.setOnClickListener(v -> {
            Intent i = new Intent(ActivityProfile.this, SelfieCaptureActivity.class);
            selfieLauncher.launch(i);
        });

        // Back
        btnBack.setOnClickListener(v -> finish());

        // Logout
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (d, w) -> {
                        auth.signOut();
                        Intent i = new Intent(ActivityProfile.this, LoginActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
