package com.health.sports.model;

public class WorkoutRecord {
    private final long recordId;
    private final long userId;
    private final long startTime;
    private long endTime;
    private double totalDistanceMeters;
    private double totalCalories;
    private int totalDurationSeconds;
    private double avgPaceSecondsPerKm;
    private WorkoutStatus status;

    public WorkoutRecord(long recordId, long userId, long startTime) {
        this.recordId = recordId;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = 0;
        this.totalDistanceMeters = 0;
        this.totalCalories = 0;
        this.totalDurationSeconds = 0;
        this.avgPaceSecondsPerKm = 0;
        this.status = WorkoutStatus.RUNNING;
    }

    public long getRecordId() {
        return recordId;
    }

    public long getUserId() {
        return userId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public double getTotalCalories() {
        return totalCalories;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public double getAvgPaceSecondsPerKm() {
        return avgPaceSecondsPerKm;
    }

    public WorkoutStatus getStatus() {
        return status;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setTotalDistanceMeters(double totalDistanceMeters) {
        this.totalDistanceMeters = Math.round(totalDistanceMeters * 10.0) / 10.0;
    }

    public void setTotalCalories(double totalCalories) {
        this.totalCalories = Math.round(totalCalories);
    }

    public void setTotalDurationSeconds(int totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public void setAvgPaceSecondsPerKm(double avgPaceSecondsPerKm) {
        this.avgPaceSecondsPerKm = avgPaceSecondsPerKm;
    }

    public void setStatus(WorkoutStatus status) {
        this.status = status;
    }

    public double getTotalDistanceKm() {
        return Math.round(totalDistanceMeters / 10.0) / 100.0;
    }

    public String formatPace() {
        if (avgPaceSecondsPerKm <= 0) {
            return "--'--\"";
        }
        int minutes = (int) (avgPaceSecondsPerKm / 60);
        int seconds = (int) (avgPaceSecondsPerKm % 60);
        return minutes + "'" + String.format("%02d", seconds) + "\"";
    }

    public String formatDuration() {
        int hours = totalDurationSeconds / 3600;
        int minutes = (totalDurationSeconds % 3600) / 60;
        int seconds = totalDurationSeconds % 60;
        if (hours > 0) {
            return hours + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        }
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }
}
