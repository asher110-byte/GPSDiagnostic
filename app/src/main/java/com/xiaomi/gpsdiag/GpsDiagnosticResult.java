package com.xiaomi.gpsdiag;

public class GpsDiagnosticResult {

    public enum Status {
        PASS,
        FAIL,
        WARNING,
        UNKNOWN
    }

    private final String testName;
    private final Status status;
    private final String details;
    private final String recommendation;

    public GpsDiagnosticResult(String testName, Status status, String details, String recommendation) {
        this.testName = testName;
        this.status = status;
        this.details = details;
        this.recommendation = recommendation;
    }

    public String getTestName() { return testName; }
    public Status getStatus() { return status; }
    public String getDetails() { return details; }
    public String getRecommendation() { return recommendation; }

    public String getStatusEmoji() {
        switch (status) {
            case PASS:    return "\u2705";
            case FAIL:    return "\u274C";
            case WARNING: return "\u26A0\uFE0F";
            default:      return "\u2753";
        }
    }
}
