package com.example.smartattendance;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class LecturerQRActivity extends AppCompatActivity {

    public static final String EXTRA_QR_TEXT = "EXTRA_QR_TEXT";
    public static final String EXTRA_INFO_TEXT = "EXTRA_INFO_TEXT";

    private TextView tvInfo;
    private ImageView imgQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_qr);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvInfo = findViewById(R.id.tvInfo);
        imgQr = findViewById(R.id.imgQr);

        // 1) From MainActivity create session
        String qrText = getIntent().getStringExtra(EXTRA_QR_TEXT);
        String infoText = getIntent().getStringExtra(EXTRA_INFO_TEXT);

        if (qrText != null && !qrText.trim().isEmpty()) {
            if (infoText != null) tvInfo.setText(infoText);
            generateQr(qrText);
            return;
        }

        // 2) From sessions list
        String sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Toast.makeText(this, "Session ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadSessionAndGenerate(sessionId);
    }

    private void loadSessionAndGenerate(String sessionId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("sessions")
                .child(sessionId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                Session s = snap.getValue(Session.class);
                if (s == null) {
                    Toast.makeText(LecturerQRActivity.this, "Session not found", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                if (s.sessionId == null || s.sessionId.trim().isEmpty()) s.sessionId = sessionId;

                // QR format student scan expects:
                String qrText = s.sessionId + "|" + safe(s.subject) + "|" + safe(s.classId) + "|"
                        + s.startTimeMs + "|" + s.durationMinutes + "|" + s.radiusMeters;

                String info = "Subject: " + safe(s.subject)
                        + "\nClass: " + safe(s.classId)
                        + "\nSession ID: " + safe(s.sessionId);

                tvInfo.setText(info);
                generateQr(qrText);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(LecturerQRActivity.this, "Load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void generateQr(String qrText) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(qrText, BarcodeFormat.QR_CODE, 600, 600);
            imgQr.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "QR error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }
}
