package com.health.sports.store;

import com.health.sports.model.HealthProfile;
import com.health.sports.model.TrackPoint;
import com.health.sports.model.User;
import com.health.sports.model.WorkoutRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryStore {
    private long userIdSequence = 1000;
    private long recordIdSequence = 2000;
    private long trackPointIdSequence = 3000;
    private final Map<Long, User> usersById = new HashMap<Long, User>();
    private final Map<String, Long> userIdsByMobile = new HashMap<String, Long>();
    private final Map<Long, HealthProfile> profilesByUserId = new HashMap<Long, HealthProfile>();
    private final Map<String, String> verificationCodes = new HashMap<String, String>();
    private final Map<Long, WorkoutRecord> recordsById = new HashMap<Long, WorkoutRecord>();
    private final Map<Long, List<TrackPoint>> trackPointsByRecordId = new HashMap<Long, List<TrackPoint>>();

    public long nextUserId() {
        userIdSequence += 1;
        return userIdSequence;
    }

    public long nextRecordId() {
        recordIdSequence += 1;
        return recordIdSequence;
    }

    public long nextTrackPointId() {
        trackPointIdSequence += 1;
        return trackPointIdSequence;
    }

    public void saveUser(User user) {
        usersById.put(user.getUserId(), user);
        userIdsByMobile.put(user.getMobile(), user.getUserId());
    }

    public User findUserById(long userId) {
        return usersById.get(userId);
    }

    public User findUserByMobile(String mobile) {
        Long userId = userIdsByMobile.get(mobile);
        return userId == null ? null : usersById.get(userId);
    }

    public boolean mobileExists(String mobile) {
        return userIdsByMobile.containsKey(mobile);
    }

    public void saveVerificationCode(String mobile, String code) {
        verificationCodes.put(mobile, code);
    }

    public String findVerificationCode(String mobile) {
        return verificationCodes.get(mobile);
    }

    public void saveHealthProfile(HealthProfile profile) {
        profilesByUserId.put(profile.getUserId(), profile);
    }

    public HealthProfile findHealthProfile(long userId) {
        return profilesByUserId.get(userId);
    }

    public void saveWorkoutRecord(WorkoutRecord record) {
        recordsById.put(record.getRecordId(), record);
    }

    public WorkoutRecord findWorkoutRecord(long recordId) {
        return recordsById.get(recordId);
    }

    public List<WorkoutRecord> findWorkoutRecordsByUserId(long userId) {
        List<WorkoutRecord> result = new ArrayList<WorkoutRecord>();
        for (WorkoutRecord record : recordsById.values()) {
            if (record.getUserId() == userId) {
                result.add(record);
            }
        }
        return result;
    }

    public void saveTrackPoint(TrackPoint point) {
        List<TrackPoint> points = trackPointsByRecordId.get(point.getRecordId());
        if (points == null) {
            points = new ArrayList<TrackPoint>();
            trackPointsByRecordId.put(point.getRecordId(), points);
        }
        points.add(point);
    }

    public List<TrackPoint> findTrackPointsByRecordId(long recordId) {
        List<TrackPoint> points = trackPointsByRecordId.get(recordId);
        return points == null ? new ArrayList<TrackPoint>() : points;
    }
}

