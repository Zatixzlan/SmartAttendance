package com.example.smartattendance;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class ActivityStudentHistory extends AppCompatActivity {

    private ListView listView;
    private View emptyState;

    private StudentHistoryAdapter adapter;
    private final ArrayList<StudentHistoryRow> rows = new ArrayList<>();

    private FirebaseAuth auth;
    private DatabaseReference myHistoryRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_history);

        listView = findViewById(R.id.listViewHistory);
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new StudentHistoryAdapter(this, rows);
        listView.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();

        loadMyHistory();
    }

    private void loadMyHistory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String uid = user.getUid();

        // ✅ Read ONLY my own history
        myHistoryRef = FirebaseDatabase.getInstance()
                .getReference("attendance_by_user")
                .child(uid);

        myHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                rows.clear();

                for (DataSnapshot sessionSnap : snap.getChildren()) {
                    String sessionId = sessionSnap.getKey();
                    Attendance att = sessionSnap.getValue(Attendance.class);
                    if (att == null) continue;

                    String status = safe(att.status);
                    long time = att.scanTimeMs;

                    rows.add(new StudentHistoryRow(sessionId, time, status));
                }

                adapter.notifyDataSetChanged();

                if (rows.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ActivityStudentHistory.this,
                        "Load failed: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }
}
