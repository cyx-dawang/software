package com.health.sports.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrainingPlan {
    private final long planId;
    private final long userId;
    private String planName;
    private String planType;
    private String status;
    private final Date createdAt;
    private String startDate;
    private String endDate;
    private int totalDays;
    private int completedDays;
    private final List<TrainingDay> trainingDays;
    private String goalDescription;
    private final List<String> tips;

    public TrainingPlan(long planId, long userId, String planName, String planType) {
        this.planId = planId;
        this.userId = userId;
        this.planName = planName;
        this.planType = planType;
        this.status = "ACTIVE";
        this.createdAt = new Date();
        this.totalDays = 0;
        this.completedDays = 0;
        this.trainingDays = new ArrayList<>();
        this.tips = new ArrayList<>();
    }

    public long getPlanId() {
        return planId;
    }

    public long getUserId() {
        return userId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getCompletedDays() {
        return completedDays;
    }

    public void setCompletedDays(int completedDays) {
        this.completedDays = completedDays;
    }

    public List<TrainingDay> getTrainingDays() {
        return trainingDays;
    }

    public String getGoalDescription() {
        return goalDescription;
    }

    public void setGoalDescription(String goalDescription) {
        this.goalDescription = goalDescription;
    }

    public List<String> getTips() {
        return tips;
    }

    public double getProgressPercent() {
        if (totalDays == 0) {
            return 0.0;
        }
        return Math.round((completedDays * 100.0 / totalDays) * 10) / 10.0;
    }

    public int getRemainingDays() {
        return totalDays - completedDays;
    }
}
