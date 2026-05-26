package com.health.modulea.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HealthProfile {
    private final long userId;
    private Gender gender;
    private LocalDate birthDate;
    private int heightCm;
    private double weightKg;
    private ActivityLevel activityLevel;
    private LocalDateTime updatedAt;

    public HealthProfile(long userId, Gender gender, LocalDate birthDate, int heightCm, double weightKg,
                         ActivityLevel activityLevel) {
        this.userId = userId;
        this.gender = gender;
        this.birthDate = birthDate;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityLevel = activityLevel;
        this.updatedAt = LocalDateTime.now();
    }

    public long getUserId() {
        return userId;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public double calcBMI() {
        double heightMeter = heightCm / 100.0;
        return Math.round((weightKg / (heightMeter * heightMeter)) * 10.0) / 10.0;
    }

    public String bmiLevel() {
        double bmi = calcBMI();
        if (bmi < 18.5) {
            return "UNDERWEIGHT";
        }
        if (bmi < 24.0) {
            return "NORMAL";
        }
        if (bmi < 28.0) {
            return "OVERWEIGHT";
        }
        return "OBESE";
    }

    public void update(Gender gender, LocalDate birthDate, int heightCm, double weightKg, ActivityLevel activityLevel) {
        this.gender = gender;
        this.birthDate = birthDate;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityLevel = activityLevel;
        this.updatedAt = LocalDateTime.now();
    }
}
