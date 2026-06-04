package com.health.sports.feature.workout;

public class KalmanFilter {
    private double processNoise;
    private double estimatedValue;
    private double estimatedError;

    public KalmanFilter(double initialValue, double initialError) {
        this.processNoise = 0.005;
        this.estimatedValue = initialValue;
        this.estimatedError = Math.max(initialError, 1.0);
    }

    public double update(double measurement, double measurementNoise) {
        double effectiveNoise = Math.max(measurementNoise, 0.5);

        double kalmanGain = estimatedError / (estimatedError + effectiveNoise);
        estimatedValue = estimatedValue + kalmanGain * (measurement - estimatedValue);
        estimatedError = (1 - kalmanGain) * estimatedError + processNoise;

        return estimatedValue;
    }

    public void reset(double initialValue, double initialError) {
        this.estimatedValue = initialValue;
        this.estimatedError = Math.max(initialError, 1.0);
    }
}
