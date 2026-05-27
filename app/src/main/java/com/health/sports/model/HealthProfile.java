package com.health.sports.model;

public class HealthProfile {
    private final long userId;
    private Gender gender;
    private String birthDate;
    private int heightCm;
    private double weightKg;
    private ActivityLevel activityLevel;

    public HealthProfile(long userId, Gender gender, String birthDate, int heightCm, double weightKg,
                         ActivityLevel activityLevel) {
        this.userId = userId;
        this.gender = gender;
        this.birthDate = birthDate;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityLevel = activityLevel;
    }

    public long getUserId() {
        return userId;
    }

    public Gender getGender() {
        return gender;
    }

    public String getBirthDate() {
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

    public double calcBMI() {
        double heightMeter = heightCm / 100.0;
        return Math.round((weightKg / (heightMeter * heightMeter)) * 10.0) / 10.0;
    }

    public String bmiLevel() {
        double bmi = calcBMI();
        if (bmi < 18.5) {
            return "偏瘦";
        }
        if (bmi < 24.0) {
            return "正常";
        }
        if (bmi < 28.0) {
            return "超重";
        }
        return "肥胖";
    }

    public void update(Gender gender, String birthDate, int heightCm, double weightKg, ActivityLevel activityLevel) {
        this.gender = gender;
        this.birthDate = birthDate;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityLevel = activityLevel;
    }
}

