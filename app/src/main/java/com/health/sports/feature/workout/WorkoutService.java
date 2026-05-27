package com.health.sports.feature.workout;

import android.os.Handler;
import android.os.Looper;

import com.health.sports.model.ApiException;
import com.health.sports.model.HealthProfile;
import com.health.sports.model.TrackPoint;
import com.health.sports.model.User;
import com.health.sports.model.WorkoutRecord;
import com.health.sports.model.WorkoutStatus;
import com.health.sports.feature.account.AccountService;
import com.health.sports.feature.profile.HealthProfileService;
import com.health.sports.store.InMemoryStore;

import java.util.List;
import java.util.Random;

public class WorkoutService {
    private static final double START_LAT = 39.9042;
    private static final double START_LON = 116.4074;
    private static final double SIMULATED_SPEED_MPS = 3.33;
    private static final double MET_RUNNING = 8.0;

    private final InMemoryStore store;
    private final AccountService accountService;
    private final HealthProfileService profileService;
    private final Handler handler;
    private final Random random;

    private WorkoutRecord currentRecord;
    private double currentLat;
    private double currentLon;
    private double headingDeg;
    private long lastPointTime;
    private long pausedDurationMs;
    private long pauseStartMs;
    private double accumulatedDistance;
    private WorkoutUpdateListener listener;

    public interface WorkoutUpdateListener {
        void onTick(WorkoutRecord record, double currentPace);
        void onStateChanged(WorkoutStatus status);
    }

    public WorkoutService(InMemoryStore store, AccountService accountService,
                          HealthProfileService profileService) {
        this.store = store;
        this.accountService = accountService;
        this.profileService = profileService;
        this.handler = new Handler(Looper.getMainLooper());
        this.random = new Random();
    }

    public void setListener(WorkoutUpdateListener listener) {
        this.listener = listener;
    }

    public WorkoutRecord startWorkout(long userId) {
        accountService.getUser(userId);
        if (currentRecord != null && currentRecord.getStatus() == WorkoutStatus.RUNNING) {
            throw new ApiException(409, "已有进行中的运动");
        }

        long recordId = store.nextRecordId();
        currentRecord = new WorkoutRecord(recordId, userId, System.currentTimeMillis());
        store.saveWorkoutRecord(currentRecord);

        currentLat = START_LAT;
        currentLon = START_LON;
        headingDeg = random.nextDouble() * 360;
        lastPointTime = System.currentTimeMillis();
        pausedDurationMs = 0;
        accumulatedDistance = 0;

        store.saveTrackPoint(new TrackPoint(store.nextTrackPointId(), recordId,
                currentLat, currentLon, 0, lastPointTime, 5.0f));

        startGpsSimulation();

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.RUNNING);
        }

        return currentRecord;
    }

    public WorkoutRecord pauseWorkout() {
        requireRunning();
        currentRecord.setStatus(WorkoutStatus.PAUSED);
        pauseStartMs = System.currentTimeMillis();
        stopGpsSimulation();

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.PAUSED);
        }

        return currentRecord;
    }

    public WorkoutRecord resumeWorkout() {
        if (currentRecord == null || currentRecord.getStatus() != WorkoutStatus.PAUSED) {
            throw new ApiException(400, "没有已暂停的运动");
        }

        pausedDurationMs += (System.currentTimeMillis() - pauseStartMs);
        currentRecord.setStatus(WorkoutStatus.RUNNING);
        lastPointTime = System.currentTimeMillis();
        startGpsSimulation();

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.RUNNING);
        }

        return currentRecord;
    }

    public WorkoutRecord endWorkout() {
        requireRunningOrPaused();
        stopGpsSimulation();

        currentRecord.setEndTime(System.currentTimeMillis());
        computeSummary();
        currentRecord.setStatus(WorkoutStatus.COMPLETED);
        store.saveWorkoutRecord(currentRecord);

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.COMPLETED);
        }

        WorkoutRecord finished = currentRecord;
        currentRecord = null;
        return finished;
    }

    public WorkoutRecord discardWorkout() {
        requireRunningOrPaused();
        stopGpsSimulation();

        currentRecord.setStatus(WorkoutStatus.DISCARDED);
        store.saveWorkoutRecord(currentRecord);

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.DISCARDED);
        }

        WorkoutRecord discarded = currentRecord;
        currentRecord = null;
        return discarded;
    }

    public WorkoutRecord getCurrentRecord() {
        return currentRecord;
    }

    public WorkoutStatus getStatus() {
        return currentRecord == null ? WorkoutStatus.IDLE : currentRecord.getStatus();
    }

    public WorkoutRecord findRecord(long recordId) {
        WorkoutRecord record = store.findWorkoutRecord(recordId);
        if (record == null) {
            throw new ApiException(404, "运动记录不存在");
        }
        return record;
    }

    public List<WorkoutRecord> findUserRecords(long userId) {
        accountService.getUser(userId);
        return store.findWorkoutRecordsByUserId(userId);
    }

    public List<TrackPoint> findTrackPoints(long recordId) {
        findRecord(recordId);
        return store.findTrackPointsByRecordId(recordId);
    }

    private void startGpsSimulation() {
        handler.postDelayed(gpsRunnable, 1000);
    }

    private void stopGpsSimulation() {
        handler.removeCallbacks(gpsRunnable);
    }

    private final Runnable gpsRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentRecord == null || currentRecord.getStatus() != WorkoutStatus.RUNNING) {
                return;
            }
            generateNextPoint();
            handler.postDelayed(this, 1000);
        }
    };

    private void generateNextPoint() {
        long now = System.currentTimeMillis();
        double deltaSec = (now - lastPointTime) / 1000.0;
        double distance = SIMULATED_SPEED_MPS * deltaSec;

        headingDeg += (random.nextDouble() - 0.5) * 4.0;
        double headingRad = Math.toRadians(headingDeg);

        double dLat = distance * Math.cos(headingRad) / 111320.0;
        double dLon = distance * Math.sin(headingRad) / (111320.0 * Math.cos(Math.toRadians(currentLat)));

        double prevLat = currentLat;
        double prevLon = currentLon;

        currentLat += dLat;
        currentLon += dLon;
        lastPointTime = now;

        float accuracy = 3.0f + random.nextFloat() * 4.0f;

        TrackPoint point = new TrackPoint(store.nextTrackPointId(),
                currentRecord.getRecordId(), currentLat, currentLon, 0, now, accuracy);
        store.saveTrackPoint(point);

        accumulatedDistance += DistanceCalculator.haversineDistance(prevLat, prevLon, currentLat, currentLon);

        updateLiveMetrics(now, accumulatedDistance);
    }

    private void updateLiveMetrics(long now, double totalDist) {
        long activeStart = currentRecord.getStartTime();
        int activeDuration = (int) ((now - activeStart - pausedDurationMs) / 1000);
        if (activeDuration <= 0) {
            activeDuration = 1;
        }

        currentRecord.setTotalDistanceMeters(totalDist);
        currentRecord.setTotalDurationSeconds(activeDuration);

        double pace = DistanceCalculator.calculatePaceSecondsPerKm(totalDist, activeDuration);
        currentRecord.setAvgPaceSecondsPerKm(pace);

        HealthProfile profile = null;
        try {
            profile = profileService.getProfile(currentRecord.getUserId());
        } catch (ApiException ignored) {
        }
        double weight = profile != null ? profile.getWeightKg() : 65.0;
        double distanceKm = totalDist / 1000.0;
        double calories = DistanceCalculator.calculateCalories(distanceKm, weight, activeDuration / 60);
        currentRecord.setTotalCalories(calories);

        if (listener != null) {
            listener.onTick(currentRecord, pace);
        }
    }

    private void computeSummary() {
        long activeDuration = (int) ((currentRecord.getEndTime()
                - currentRecord.getStartTime() - pausedDurationMs) / 1000);
        if (activeDuration <= 0) {
            activeDuration = 1;
        }

        currentRecord.setTotalDistanceMeters(accumulatedDistance);
        currentRecord.setTotalDurationSeconds((int) activeDuration);

        double pace = DistanceCalculator.calculatePaceSecondsPerKm(accumulatedDistance, (int) activeDuration);
        currentRecord.setAvgPaceSecondsPerKm(pace);

        HealthProfile profile = null;
        try {
            profile = profileService.getProfile(currentRecord.getUserId());
        } catch (ApiException ignored) {
        }
        double weight = profile != null ? profile.getWeightKg() : 65.0;
        double distanceKm = accumulatedDistance / 1000.0;
        int durationMin = (int) (activeDuration / 60);
        double calories = DistanceCalculator.calculateCalories(distanceKm, weight, durationMin);
        currentRecord.setTotalCalories(calories);
    }

    private void requireRunning() {
        if (currentRecord == null || currentRecord.getStatus() != WorkoutStatus.RUNNING) {
            throw new ApiException(400, "当前没有进行中的运动");
        }
    }

    private void requireRunningOrPaused() {
        if (currentRecord == null) {
            throw new ApiException(400, "当前没有运动记录");
        }
        if (currentRecord.getStatus() != WorkoutStatus.RUNNING
                && currentRecord.getStatus() != WorkoutStatus.PAUSED) {
            throw new ApiException(400, "当前运动状态不允许此操作");
        }
    }
}
