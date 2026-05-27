package com.health.sports.model;

public class TrackPoint {
    private final long pointId;
    private final long recordId;
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final long timestamp;
    private final float accuracy;

    public TrackPoint(long pointId, long recordId, double latitude, double longitude,
                      double altitude, long timestamp, float accuracy) {
        this.pointId = pointId;
        this.recordId = recordId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
        this.accuracy = accuracy;
    }

    public long getPointId() {
        return pointId;
    }

    public long getRecordId() {
        return recordId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getAccuracy() {
        return accuracy;
    }
}
