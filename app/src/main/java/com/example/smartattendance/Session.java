package com.example.smartattendance;

public class Session {
    public String sessionId;
    public String subject;
    public String classId;
    public long startTimeMs;
    public int durationMinutes;
    public double lecturerLat;
    public double lecturerLng;
    public int radiusMeters;

    public Session() { } // required for Firebase

    public Session(String sessionId, String subject, String classId,
                   long startTimeMs, int durationMinutes,
                   double lecturerLat, double lecturerLng, int radiusMeters) {
        this.sessionId = sessionId;
        this.subject = subject;
        this.classId = classId;
        this.startTimeMs = startTimeMs;
        this.durationMinutes = durationMinutes;
        this.lecturerLat = lecturerLat;
        this.lecturerLng = lecturerLng;
        this.radiusMeters = radiusMeters;
    }
}
