package com.example.smartattendance;

public class Attendance {
    public String attendanceId;
    public String sessionId;
    public String studentUid;
    public String studentName;
    public long scanTimeMs;
    public double scanLat;
    public double scanLng;
    public String status; // "present" / "late" / "rejected"

    public Attendance() { } // required for Firebase

    public Attendance(String attendanceId, String sessionId, String studentUid, String studentName,
                      long scanTimeMs, double scanLat, double scanLng, String status) {
        this.attendanceId = attendanceId;
        this.sessionId = sessionId;
        this.studentUid = studentUid;
        this.studentName = studentName;
        this.scanTimeMs = scanTimeMs;
        this.scanLat = scanLat;
        this.scanLng = scanLng;
        this.status = status;
    }
}
