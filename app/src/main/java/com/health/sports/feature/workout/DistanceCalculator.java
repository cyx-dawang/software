package com.health.sports.feature.workout;

public class DistanceCalculator {

    private static final double EARTH_RADIUS_M = 6371000.0;

    private DistanceCalculator() {
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    public static double calculatePaceSecondsPerKm(double distanceMeters, int durationSeconds) {
        if (distanceMeters <= 0 || durationSeconds <= 0) {
            return 0;
        }
        return durationSeconds / (distanceMeters / 1000.0);
    }

    public static double calculateCalories(double distanceKm, double weightKg, int durationMinutes) {
        double met = 8.0;
        if (weightKg <= 0) {
            weightKg = 65.0;
        }
        double hours = durationMinutes / 60.0;
        return met * weightKg * hours;
    }
}
