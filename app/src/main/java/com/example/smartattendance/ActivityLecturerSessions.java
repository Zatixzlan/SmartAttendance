package com.example.smartattendance;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class ActivityLecturerSessions extends AppCompatActivity {

    private ListView listView;
    private TextView tvEmpty;
    private View emptyState;

    private final ArrayList<String> items = new ArrayList<>();
    private final ArrayList<String> sessionIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private DatabaseReference sessionsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_sessions);

        listView = findViewById(R.id.listViewSessions);
        tvEmpty = findViewById(R.id.tvEmpty);
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new ArrayAdapter<>(
                this,
                R.layout.item_session_row,
                R.id.tvSessionRow,
                items
        );
        listView.setAdapter(adapter);

        sessionsRef = FirebaseDatabase.getInstance().getReference("sessions");

        loadSessions();

        // ✅ Tap → choose Show QR / View Attendance
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String sessionId = sessionIds.get(position);

            new AlertDialog.Builder(ActivityLecturerSessions.this)
                    .setTitle("Session Options")
                    .setItems(new String[]{"Show QR", "View Attendance"}, (dialog, which) -> {
                        if (which == 0) {
                            Intent i = new Intent(ActivityLecturerSessions.this, LecturerQRActivity.class);
                            i.putExtra("sessionId", sessionId);
                            startActivity(i);
                        } else {
                            Intent i = new Intent(ActivityLecturerSessions.this, ActivityLecturerSessionAttendance.class);
                            i.putExtra("sessionId", sessionId);
                            startActivity(i);
                        }
                    })
                    .show();
        });
    }

    private void loadSessions() {
        sessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot rootSnap) {
                items.clear();
                sessionIds.clear();

                for (DataSnapshot snap : rootSnap.getChildren()) {
                    Session s = snap.getValue(Session.class);
                    if (s == null) continue;

                    // sessionId fallback to key
                    String sid = (s.sessionId != null && !s.sessionId.trim().isEmpty())
                            ? s.sessionId
                            : snap.getKey();
                    if (sid == null) continue;

                    sessionIds.add(sid);

                    String timeStr = (s.startTimeMs > 0)
                            ? DateFormat.format("dd/MM/yyyy HH:mm", new Date(s.startTimeMs)).toString()
                            : "-";

                    // ✅ Display only Subject, Class, Time
                    String line = "Subject: " + safe(s.subject)
                            + "\nClass: " + safe(s.classId)
                            + "\nTime: " + timeStr;

                    items.add(line);
                }

                adapter.notifyDataSetChanged();

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
