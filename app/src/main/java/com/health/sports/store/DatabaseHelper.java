package com.health.sports.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.health.sports.model.*;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "health_sports.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users ("
                + "userId INTEGER PRIMARY KEY, mobile TEXT UNIQUE NOT NULL,"
                + "passwordHash TEXT NOT NULL, nickname TEXT, avatarUrl TEXT,"
                + "status TEXT DEFAULT 'ACTIVE')");

        db.execSQL("CREATE TABLE health_profiles ("
                + "userId INTEGER PRIMARY KEY, gender TEXT, birthDate TEXT,"
                + "heightCm INTEGER, weightKg REAL, activityLevel TEXT,"
                + "restingHeartRate INTEGER, bpSystolic INTEGER, bpDiastolic INTEGER)");

        db.execSQL("CREATE TABLE workout_records ("
                + "recordId INTEGER PRIMARY KEY, userId INTEGER,"
                + "startTime INTEGER, endTime INTEGER,"
                + "totalDistanceMeters REAL, totalCalories REAL,"
                + "totalDurationSeconds INTEGER, avgPaceSecondsPerKm REAL,"
                + "status TEXT)");

        db.execSQL("CREATE TABLE track_points ("
                + "pointId INTEGER PRIMARY KEY, recordId INTEGER,"
                + "latitude REAL, longitude REAL, altitude REAL,"
                + "timestamp INTEGER, accuracy REAL)");

        db.execSQL("CREATE TABLE verification_codes ("
                + "mobile TEXT PRIMARY KEY, code TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS track_points");
        db.execSQL("DROP TABLE IF EXISTS workout_records");
        db.execSQL("DROP TABLE IF EXISTS health_profiles");
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS verification_codes");
        onCreate(db);
    }

    private long userIdSeq = 1000;
    private long recordIdSeq = 2000;
    private long trackPointIdSeq = 3000;

    public synchronized long nextUserId() { return ++userIdSeq; }
    public synchronized long nextRecordId() { return ++recordIdSeq; }
    public synchronized long nextTrackPointId() { return ++trackPointIdSeq; }

    public void initSequences() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(userId) FROM users", null);
        if (c.moveToFirst() && !c.isNull(0)) userIdSeq = c.getLong(0);
        c.close();
        c = db.rawQuery("SELECT MAX(recordId) FROM workout_records", null);
        if (c.moveToFirst() && !c.isNull(0)) recordIdSeq = c.getLong(0);
        c.close();
        c = db.rawQuery("SELECT MAX(pointId) FROM track_points", null);
        if (c.moveToFirst() && !c.isNull(0)) trackPointIdSeq = c.getLong(0);
        c.close();
    }

    // --- User ---
    public void saveUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", user.getUserId());
        cv.put("mobile", user.getMobile());
        cv.put("passwordHash", user.getPasswordHash());
        cv.put("nickname", user.getNickname());
        cv.put("avatarUrl", user.getAvatarUrl());
        cv.put("status", user.getStatus().name());
        db.insertWithOnConflict("users", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public User findUserById(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("users", null, "userId=?", new String[]{String.valueOf(userId)},
                null, null, null);
        try {
            if (c.moveToFirst()) return cursorToUser(c);
            return null;
        } finally { c.close(); }
    }

    public User findUserByMobile(String mobile) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("users", null, "mobile=?", new String[]{mobile},
                null, null, null);
        try {
            if (c.moveToFirst()) return cursorToUser(c);
            return null;
        } finally { c.close(); }
    }

    public boolean mobileExists(String mobile) {
        return findUserByMobile(mobile) != null;
    }

    private User cursorToUser(Cursor c) {
        User u = new User(c.getLong(0), c.getString(1), c.getString(2), c.getString(3));
        u.updateProfile(c.getString(3), c.getString(4));
        try { u.setStatus(UserStatus.valueOf(c.getString(5))); } catch (Exception ignored) {}
        return u;
    }

    // --- Verification Code ---
    public void saveVerificationCode(String mobile, String code) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("mobile", mobile);
        cv.put("code", code);
        db.insertWithOnConflict("verification_codes", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String findVerificationCode(String mobile) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("verification_codes", new String[]{"code"},
                "mobile=?", new String[]{mobile}, null, null, null);
        try {
            if (c.moveToFirst()) return c.getString(0);
            return null;
        } finally { c.close(); }
    }

    // --- HealthProfile ---
    public void saveHealthProfile(HealthProfile profile) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", profile.getUserId());
        cv.put("gender", profile.getGender().name());
        cv.put("birthDate", profile.getBirthDate());
        cv.put("heightCm", profile.getHeightCm());
        cv.put("weightKg", profile.getWeightKg());
        cv.put("activityLevel", profile.getActivityLevel().name());
        cv.put("restingHeartRate", profile.getRestingHeartRate());
        cv.put("bpSystolic", profile.getBpSystolic());
        cv.put("bpDiastolic", profile.getBpDiastolic());
        db.insertWithOnConflict("health_profiles", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public HealthProfile findHealthProfile(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("health_profiles", null, "userId=?",
                new String[]{String.valueOf(userId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                HealthProfile p = new HealthProfile(c.getLong(0),
                        Gender.valueOf(c.getString(1)), c.getString(2),
                        c.getInt(3), c.getDouble(4),
                        ActivityLevel.valueOf(c.getString(5)));
                p.setRestingHeartRate(c.getInt(6));
                p.setBpSystolic(c.getInt(7));
                p.setBpDiastolic(c.getInt(8));
                return p;
            }
            return null;
        } finally { c.close(); }
    }

    // --- WorkoutRecord ---
    public void saveWorkoutRecord(WorkoutRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("recordId", record.getRecordId());
        cv.put("userId", record.getUserId());
        cv.put("startTime", record.getStartTime());
        cv.put("endTime", record.getEndTime());
        cv.put("totalDistanceMeters", record.getTotalDistanceMeters());
        cv.put("totalCalories", record.getTotalCalories());
        cv.put("totalDurationSeconds", record.getTotalDurationSeconds());
        cv.put("avgPaceSecondsPerKm", record.getAvgPaceSecondsPerKm());
        cv.put("status", record.getStatus().name());
        db.insertWithOnConflict("workout_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public WorkoutRecord findWorkoutRecord(long recordId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("workout_records", null, "recordId=?",
                new String[]{String.valueOf(recordId)}, null, null, null);
        try {
            if (c.moveToFirst()) return cursorToRecord(c);
            return null;
        } finally { c.close(); }
    }

    public List<WorkoutRecord> findWorkoutRecordsByUserId(long userId) {
        List<WorkoutRecord> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("workout_records", null, "userId=?",
                new String[]{String.valueOf(userId)}, null, null, "startTime DESC");
        try {
            while (c.moveToNext()) result.add(cursorToRecord(c));
        } finally { c.close(); }
        return result;
    }

    private WorkoutRecord cursorToRecord(Cursor c) {
        WorkoutRecord r = new WorkoutRecord(c.getLong(0), c.getLong(1), c.getLong(2));
        r.setEndTime(c.getLong(3));
        r.setTotalDistanceMeters(c.getDouble(4));
        r.setTotalCalories(c.getDouble(5));
        r.setTotalDurationSeconds(c.getInt(6));
        r.setAvgPaceSecondsPerKm(c.getDouble(7));
        try { r.setStatus(WorkoutStatus.valueOf(c.getString(8))); } catch (Exception ignored) {}
        return r;
    }

    // --- TrackPoint ---
    public void saveTrackPoint(TrackPoint point) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("pointId", point.getPointId());
        cv.put("recordId", point.getRecordId());
        cv.put("latitude", point.getLatitude());
        cv.put("longitude", point.getLongitude());
        cv.put("altitude", point.getAltitude());
        cv.put("timestamp", point.getTimestamp());
        cv.put("accuracy", point.getAccuracy());
        db.insert("track_points", null, cv);
    }

    public List<TrackPoint> findTrackPointsByRecordId(long recordId) {
        List<TrackPoint> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("track_points", null, "recordId=?",
                new String[]{String.valueOf(recordId)}, null, null, "timestamp ASC");
        try {
            while (c.moveToNext()) {
                result.add(new TrackPoint(c.getLong(0), c.getLong(1),
                        c.getDouble(2), c.getDouble(3), c.getDouble(4),
                        c.getLong(5), c.getFloat(6)));
            }
        } finally { c.close(); }
        return result;
    }

    public List<User> getAllUsers() {
        List<User> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("users", null, null, null, null, null, null);
        try { while (c.moveToNext()) result.add(cursorToUser(c)); }
        finally { c.close(); }
        return result;
    }

    public List<HealthProfile> getAllHealthProfiles() {
        List<HealthProfile> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("health_profiles", null, null, null, null, null, null);
        try {
            while (c.moveToNext()) {
                HealthProfile p = new HealthProfile(c.getLong(0),
                        Gender.valueOf(c.getString(1)), c.getString(2),
                        c.getInt(3), c.getDouble(4),
                        ActivityLevel.valueOf(c.getString(5)));
                p.setRestingHeartRate(c.getInt(6));
                p.setBpSystolic(c.getInt(7));
                p.setBpDiastolic(c.getInt(8));
                result.add(p);
            }
        } finally { c.close(); }
        return result;
    }

    public List<WorkoutRecord> getAllWorkoutRecords() {
        List<WorkoutRecord> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("workout_records", null, null, null, null, null, null);
        try { while (c.moveToNext()) result.add(cursorToRecord(c)); }
        finally { c.close(); }
        return result;
    }
}
