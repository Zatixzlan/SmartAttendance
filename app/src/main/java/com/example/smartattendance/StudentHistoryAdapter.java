package com.example.smartattendance;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.List;

public class StudentHistoryAdapter extends ArrayAdapter<StudentHistoryRow> {

    private final LayoutInflater inflater;
    private final DatabaseReference sessionsRef;

    public StudentHistoryAdapter(@NonNull Context context, @NonNull List<StudentHistoryRow> rows) {
        super(context, 0, rows);
        inflater = LayoutInflater.from(context);
        sessionsRef = FirebaseDatabase.getInstance().getReference("sessions");
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) v = inflater.inflate(R.layout.item_student_history_row, parent, false);

        StudentHistoryRow row = getItem(position);
        if (row == null) return v;

        TextView tvLine = v.findViewById(R.id.tvLine);
        Button btnMap = v.findViewById(R.id.btnMap);

        String timeStr = (row.scanTimeMs > 0)
                ? DateFormat.format("dd/MM/yyyy HH:mm", new Date(row.scanTimeMs)).toString()
                : "-";

        String sid = (row.sessionId != null) ? row.sessionId : "-";
        String status = (row.status == null || row.status.trim().isEmpty()) ? "-" : row.status;

        String line = "Session: " + sid
                + "\nTime: " + timeStr
                + "\nStatus: " + status;

        tvLine.setText(line);

        btnMap.setOnClickListener(view -> openMapForSession(sid));

        return v;
    }

    private void openMapForSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty() || sessionId.equals("-")) {
            Toast.makeText(getContext(), "Invalid session id", Toast.LENGTH_SHORT).show();
            return;
        }

        sessionsRef.child(sessionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                Double lat = snap.child("lecturerLat").getValue(Double.class);
                Double lng = snap.child("lecturerLng").getValue(Double.class);

                if (lat == null || lng == null) {
                    Toast.makeText(getContext(), "Location not found for this session", Toast.LENGTH_LONG).show();
                    return;
                }

                Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(Class Location)");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getContext().getPackageManager()) == null) {
                    mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                }

                getContext().startActivity(mapIntent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "DB error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
