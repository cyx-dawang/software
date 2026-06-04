package com.health.sports.model;

public class TrainingDay {
    private final int dayIndex;
    private final String date;
    private String exerciseType;
    private int targetDurationMinutes;
    private double targetDistanceKm;
    private String intensity;
    private String description;
    private boolean completed;
    private int actualDurationMinutes;
    private double actualDistanceKm;
    private String note;

    public TrainingDay(int dayIndex, String date) {
        this.dayIndex = dayIndex;
        this.date = date;
        this.intensity = "MEDIUM";
        this.completed = false;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public String getDate() {
        return date;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public int getTargetDurationMinutes() {
        return targetDurationMinutes;
    }

    public void setTargetDurationMinutes(int targetDurationMinutes) {
        this.targetDurationMinutes = targetDurationMinutes;
    }

    public double getTargetDistanceKm() {
        return targetDistanceKm;
    }

    public void setTargetDistanceKm(double targetDistanceKm) {
        this.targetDistanceKm = targetDistanceKm;
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = intensity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(int actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public double getActualDistanceKm() {
        return actualDistanceKm;
    }

    public void setActualDistanceKm(double actualDistanceKm) {
        this.actualDistanceKm = actualDistanceKm;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
