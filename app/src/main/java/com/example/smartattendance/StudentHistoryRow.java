package com.example.smartattendance;

public class StudentHistoryRow {
    public String sessionId;
    public long scanTimeMs;
    public String status;

    public StudentHistoryRow() {}

    public StudentHistoryRow(String sessionId, long scanTimeMs, String status) {
        this.sessionId = sessionId;
        this.scanTimeMs = scanTimeMs;
        this.status = status;
    }
}
