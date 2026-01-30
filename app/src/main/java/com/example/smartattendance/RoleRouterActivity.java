package com.example.smartattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RoleRouterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_router);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        FirebaseUser user = auth.getCurrentUser();

        // Not logged in → go to Login
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Logged in → check role in Realtime DB
        usersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String role = snapshot.child("role").getValue(String.class);

                if ("lecturer".equalsIgnoreCase(role)) {
                    startActivity(new Intent(RoleRouterActivity.this, LecturerHomeActivity.class));
                } else if ("student".equalsIgnoreCase(role)) {
                    startActivity(new Intent(RoleRouterActivity.this, StudentHomeActivity.class));
                } else {
                    // Role missing / not set → force to login/register again
                    Toast.makeText(RoleRouterActivity.this, "Role not found. Please register again.", Toast.LENGTH_LONG).show();
                    auth.signOut();
                    startActivity(new Intent(RoleRouterActivity.this, LoginActivity.class));
                }
                finish();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RoleRouterActivity.this, "DB error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                startActivity(new Intent(RoleRouterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
