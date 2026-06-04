package com.health.sports.model;

import java.util.Date;

public class SyncedHealthData {
    private final long userId;
    private final String sourceType;
    private int stepCount;
    private int heartRateAvg;
    private int heartRateMax;
    private int sleepDurationMinutes;
    private int sleepDeepMinutes;
    private int sleepLightMinutes;
    private double caloriesBurned;
    private double bloodOxygen;
    private final Date syncTime;

    public SyncedHealthData(long userId, String sourceType) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.syncTime = new Date();
    }

    public long getUserId() {
        return userId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public int getHeartRateAvg() {
        return heartRateAvg;
    }

    public void setHeartRateAvg(int heartRateAvg) {
        this.heartRateAvg = heartRateAvg;
    }

    public int getHeartRateMax() {
        return heartRateMax;
    }

    public void setHeartRateMax(int heartRateMax) {
        this.heartRateMax = heartRateMax;
    }

    public int getSleepDurationMinutes() {
        return sleepDurationMinutes;
    }

    public void setSleepDurationMinutes(int sleepDurationMinutes) {
        this.sleepDurationMinutes = sleepDurationMinutes;
    }

    public int getSleepDeepMinutes() {
        return sleepDeepMinutes;
    }

    public void setSleepDeepMinutes(int sleepDeepMinutes) {
        this.sleepDeepMinutes = sleepDeepMinutes;
    }

    public int getSleepLightMinutes() {
        return sleepLightMinutes;
    }

    public void setSleepLightMinutes(int sleepLightMinutes) {
        this.sleepLightMinutes = sleepLightMinutes;
    }

    public double getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(double caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public double getBloodOxygen() {
        return bloodOxygen;
    }

    public void setBloodOxygen(double bloodOxygen) {
        this.bloodOxygen = bloodOxygen;
    }

    public Date getSyncTime() {
        return syncTime;
    }
}
