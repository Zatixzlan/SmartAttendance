package com.example.smartattendance;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class ActivityLecturerSessionAttendance extends AppCompatActivity {

    private TextView tvSessionTitle, tvEmpty;
    private ListView listView;
    private View emptyState;

    private final ArrayList<String> items = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private DatabaseReference attendanceRef;
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_session_attendance);

        tvSessionTitle = findViewById(R.id.tvSessionTitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        listView = findViewById(R.id.listViewAttendance);
        emptyState = findViewById(R.id.emptyState);

        // Back button (new UI)
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new ArrayAdapter<>(
                this,
                R.layout.item_list_black_text,
                R.id.tvRowText,
                items
        );
        listView.setAdapter(adapter);

        sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null || sessionId.trim().isEmpty()) sessionId = "-";

        attendanceRef = FirebaseDatabase.getInstance()
                .getReference("attendance")
                .child(sessionId);

        loadAttendance();
    }

    private void loadAttendance() {
        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot rootSnap) {
                items.clear();

                for (DataSnapshot snap : rootSnap.getChildren()) {
                    Attendance a = snap.getValue(Attendance.class);
                    if (a == null) continue;

                    String timeStr = (a.scanTimeMs > 0)
                            ? DateFormat.format("dd/MM/yyyy HH:mm", new Date(a.scanTimeMs)).toString()
                            : "-";

                    String line = "Name: " + safe(a.studentName)
                            + "\nTime: " + timeStr
                            + "\nStatus: " + safe(a.status);

                    items.add(line);
                }

                adapter.notifyDataSetChanged();

                // ✅ Toggle empty state
                if (items.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvEmpty.setText("Load failed: " + error.getMessage());
                emptyState.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            }
        });
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }
}
