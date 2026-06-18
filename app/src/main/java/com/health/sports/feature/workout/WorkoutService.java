package com.health.sports.feature.workout;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
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

public class WorkoutService {
    private static final String TAG = "WorkoutService";
    private static final double FALLBACK_LAT = 39.9042;
    private static final double FALLBACK_LON = 116.4074;

    private final InMemoryStore store;
    private final AccountService accountService;
    private final HealthProfileService profileService;
    private final Context appContext;
    private final LocationClient locationClient;
    private final SensorManager sensorManager;
    private final Handler handler;
    private final boolean baiduLocationEnabled;

    private WorkoutRecord currentRecord;
    private double currentLat;
    private double currentLon;
    private long lastPointTime;
    private long pausedDurationMs;
    private long pauseStartMs;
    private double accumulatedDistance;
    private boolean initialLocationReceived;
    private float currentAccuracy;
    private WorkoutUpdateListener listener;
    private boolean locationStarted;
    private double preloadedLat;
    private double preloadedLon;
    private boolean preloadedReceived;
    private int gpsUpdateCount;
    private int lastLocType;
    private int stepCount;
    private int stepCountStart;
    private long lastLocationUpdateTime;
    private double smoothedLat;
    private double smoothedLon;
    private double lastAcceptedLat;
    private double lastAcceptedLon;
    private int stationaryCount;
    private int consecutiveRejections;
    private KalmanFilter latFilter;
    private KalmanFilter lonFilter;
    private double userStepLengthM;
    private Runnable demoLocationRunnable;
    private int demoTick;
    private Runnable tickerRunnable;
    private static final double MIN_MOVE_SPEED_MS = 0.8;
    private static final double MAX_ACCEPTABLE_ACCURACY = 30.0;
    private static final double MAX_ACCEPTABLE_ACCURACY_FALLBACK = 65.0;
    private static final double MAX_JUMP_METERS = 300.0;
    private static final int MAX_CONSECUTIVE_REJECTIONS = 5;
    private static final double MIN_SPEED_THRESHOLD = 0.3;
    private static final double MAX_SPEED_THRESHOLD = 7.0;
    private static final double DEFAULT_STEP_LENGTH_M = 0.75;
    private static final long GPS_FIRST_FIX_TIMEOUT_MS = 45000;
    private long gpsSearchStartTime;
    private boolean gpsTimeoutFallback;
    private int consecutiveLowAccuracyCount;
    private static final int MAX_LOW_ACCURACY_BURST = 3;

    public interface WorkoutUpdateListener {
        void onTick(WorkoutRecord record, double currentPace);
        void onStateChanged(WorkoutStatus status);
        void onGpsFixAcquired(double lat, double lng, float accuracy);
    }

    public WorkoutService(InMemoryStore store, AccountService accountService,
                          HealthProfileService profileService, Context context,
                          boolean baiduLocationEnabled) {
        this.store = store;
        this.accountService = accountService;
        this.profileService = profileService;
        this.appContext = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.baiduLocationEnabled = baiduLocationEnabled;

        sensorManager = (SensorManager) this.appContext.getSystemService(Context.SENSOR_SERVICE);

        LocationClient client;
        if (baiduLocationEnabled) {
            try {
                client = new LocationClient(this.appContext);
            } catch (Exception e) {
                Log.w(TAG, "Baidu LocationClient unavailable, falling back to demo location");
                client = null;
            }
        } else {
            client = null;
        }
        this.locationClient = client;

        if (locationClient != null) {
            locationClient.registerLocationListener(new WorkoutLocationListener());

            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setCoorType("bd09ll");
            option.setScanSpan(5000);
            option.setOpenGps(true);
            option.setNeedDeviceDirect(true);
            option.setLocationNotify(true);
            option.setIgnoreKillProcess(false);
            option.setIsNeedAddress(false);
            option.setIsNeedLocationDescribe(false);
            option.setIsNeedAltitude(false);
            option.setWifiCacheTimeOut(5 * 60 * 1000);

            try {
                option.setPriority(LocationClientOption.GPS_FIRST);
            } catch (NoSuchMethodError e) {
                Log.w(TAG, "setPriority not available in this SDK version, using default");
            }

            locationClient.setLocOption(option);

            Log.i(TAG, "LocationClient configured: mode=Hight_Accuracy, scan=5000ms, GPS=ON");
        } else {
            Log.i(TAG, "Baidu location disabled; demo location mode is active");
        }
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

        currentLat = preloadedReceived ? preloadedLat : FALLBACK_LAT;
        currentLon = preloadedReceived ? preloadedLon : FALLBACK_LON;
        lastPointTime = System.currentTimeMillis();
        lastLocationUpdateTime = System.currentTimeMillis();
        pausedDurationMs = 0;
        accumulatedDistance = 0;
        initialLocationReceived = preloadedReceived;
        currentAccuracy = preloadedReceived ? 8.0f : 0.0f;
        smoothedLat = currentLat;
        smoothedLon = currentLon;
        lastAcceptedLat = currentLat;
        lastAcceptedLon = currentLon;
        stationaryCount = 0;
        consecutiveRejections = 0;
        consecutiveLowAccuracyCount = 0;
        gpsUpdateCount = 0;
        demoTick = 0;
        gpsSearchStartTime = System.currentTimeMillis();
        gpsTimeoutFallback = false;
        stepCount = 0;
        stepCountStart = 0;

        HealthProfile profile = null;
        try {
            profile = profileService.getProfile(userId);
        } catch (ApiException ignored) {
        }
        if (profile != null && profile.getHeightCm() > 0) {
            userStepLengthM = (profile.getHeightCm() / 100.0) * 0.45;
        } else {
            userStepLengthM = DEFAULT_STEP_LENGTH_M;
        }

        latFilter = new KalmanFilter(currentLat, 10.0);
        lonFilter = new KalmanFilter(currentLon, 10.0);

        startBaiduLocation();
        startStepCounter();
        startWatchdog();
        startTicker();

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.RUNNING);
        }

        return currentRecord;
    }

    public WorkoutRecord pauseWorkout() {
        requireRunning();
        currentRecord.setStatus(WorkoutStatus.PAUSED);
        pauseStartMs = System.currentTimeMillis();

        stopBaiduLocation();
        stopStepCounter();
        stopWatchdog();
        stopTicker();

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
        lastLocationUpdateTime = System.currentTimeMillis();
        consecutiveLowAccuracyCount = 0;
        if (!initialLocationReceived) {
            gpsSearchStartTime = System.currentTimeMillis();
            gpsTimeoutFallback = false;
        }

        startBaiduLocation();
        startStepCounter();
        startWatchdog();
        startTicker();

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.RUNNING);
        }

        return currentRecord;
    }

    public WorkoutRecord endWorkout() {
        requireRunningOrPaused();

        currentRecord.setEndTime(System.currentTimeMillis());
        computeSummary();
        currentRecord.setStatus(WorkoutStatus.COMPLETED);
        store.saveWorkoutRecord(currentRecord);

        stopBaiduLocation();
        stopStepCounter();
        stopWatchdog();
        stopTicker();

        if (listener != null) {
            listener.onStateChanged(WorkoutStatus.COMPLETED);
        }

        WorkoutRecord finished = currentRecord;
        currentRecord = null;
        return finished;
    }

    public WorkoutRecord discardWorkout() {
        requireRunningOrPaused();

        currentRecord.setStatus(WorkoutStatus.DISCARDED);
        store.saveWorkoutRecord(currentRecord);

        stopBaiduLocation();
        stopStepCounter();
        stopWatchdog();
        stopTicker();

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

    public double getLatestLatitude() {
        return currentLat;
    }

    public double getLatestLongitude() {
        return currentLon;
    }

    public boolean isGpsFixed() {
        return initialLocationReceived;
    }

    public float getCurrentAccuracy() {
        return currentAccuracy;
    }

    public boolean isGpsProviderEnabled() {
        if (!baiduLocationEnabled) {
            return true;
        }
        if (appContext == null) return false;
        LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        return lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public int getStepCount() {
        return stepCount;
    }

    public String getGpsStatusText() {
        if (!baiduLocationEnabled) {
            StringBuilder sb = new StringBuilder();
            sb.append("演示定位 · 模拟轨迹");
            sb.append(" · 更新").append(gpsUpdateCount).append("次");
            if (stepCount > 0) {
                sb.append(" · ").append(stepCount).append("步");
            }
            return sb.toString();
        }
        if (!initialLocationReceived && !preloadedReceived) {
            long searchTime = System.currentTimeMillis() - gpsSearchStartTime;
            if (gpsTimeoutFallback || searchTime > GPS_FIRST_FIX_TIMEOUT_MS) {
                return "GPS搜索超时，正在尝试网络定位...";
            }
            return "正在搜索卫星...(" + (searchTime / 1000) + "s)";
        }
        String source;
        if (lastLocType == BDLocation.TypeGpsLocation) {
            source = "GPS卫星";
        } else if (lastLocType == BDLocation.TypeNetWorkLocation) {
            source = "网络定位";
        } else if (lastLocType == BDLocation.TypeCacheLocation) {
            source = "缓存定位";
        } else if (lastLocType == BDLocation.TypeOffLineLocation) {
            source = "离线定位";
        } else {
            source = "混合定位";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(source).append(" · 精度").append(String.format("%.0f", currentAccuracy)).append("m");
        sb.append(" · 更新").append(gpsUpdateCount).append("次");
        if (stepCount > 0) {
            sb.append(" · ").append(stepCount).append("步");
        }
        return sb.toString();
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

    public void shutdown() {
        stopBaiduLocation();
        stopStepCounter();
        stopWatchdog();
        stopTicker();
    }

    public void startPreloading() {
        if (!baiduLocationEnabled) {
            preloadedLat = FALLBACK_LAT;
            preloadedLon = FALLBACK_LON;
            boolean wasReceived = preloadedReceived;
            preloadedReceived = true;
            currentLat = preloadedLat;
            currentLon = preloadedLon;
            currentAccuracy = 8.0f;
            if (!wasReceived && listener != null) {
                listener.onGpsFixAcquired(preloadedLat, preloadedLon, currentAccuracy);
            }
            return;
        }
        if (!locationStarted && locationClient != null) {
            preloadedReceived = false;
            locationClient.start();
            locationStarted = true;
            Log.i(TAG, "Preloading started");
        }
    }

    public void stopPreloading() {
        stopBaiduLocation();
        if (baiduLocationEnabled) {
            preloadedReceived = false;
        }
    }

    public boolean isPreloadedReceived() {
        return preloadedReceived;
    }

    private class WorkoutLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                Log.w(TAG, "onReceiveLocation: location is null");
                return;
            }
            int locType = location.getLocType();
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            float radius = location.getRadius();
            Log.d(TAG, "onReceiveLocation: locType=" + locType
                    + " lat=" + lat + " lng=" + lng
                    + " radius=" + radius);
            handler.post(() -> {
                if (currentRecord == null) {
                    preloadedLat = lat;
                    preloadedLon = lng;
                    boolean wasReceived = preloadedReceived;
                    preloadedReceived = true;
                    lastLocType = locType;
                    gpsUpdateCount++;
                    Log.d(TAG, "Preloaded location: lat=" + preloadedLat + " lng=" + preloadedLon);
                    if (!wasReceived && listener != null) {
                        listener.onGpsFixAcquired(preloadedLat, preloadedLon, radius);
                    }
                    return;
                }
                if (currentRecord.getStatus() != WorkoutStatus.RUNNING) {
                    Log.d(TAG, "processLocation skipped: status=" + currentRecord.getStatus());
                    return;
                }
                if (locType == BDLocation.TypeServerError
                        || locType == BDLocation.TypeNetWorkException
                        || locType == BDLocation.TypeCriteriaException) {
                    Log.w(TAG, "Location error: locType=" + locType
                            + ", checking GPS timeout fallback (searched "
                            + (System.currentTimeMillis() - gpsSearchStartTime) / 1000 + "s)");
                    if (!initialLocationReceived && !gpsTimeoutFallback
                            && System.currentTimeMillis() - gpsSearchStartTime > GPS_FIRST_FIX_TIMEOUT_MS) {
                        gpsTimeoutFallback = true;
                        Log.w(TAG, "GPS first fix timed out after "
                                + (GPS_FIRST_FIX_TIMEOUT_MS / 1000)
                                + "s, will try to restart LocationClient to refresh");
                        stopBaiduLocation();
                        handler.postDelayed(() -> {
                            startBaiduLocation();
                            lastLocationUpdateTime = System.currentTimeMillis();
                        }, 1000);
                    }
                    return;
                }
                lastLocType = locType;
                processLocation(location);
            });
        }
    }

    private void processLocation(BDLocation location) {
        long now = System.currentTimeMillis();
        float speed = location.hasSpeed() ? (float) location.getSpeed() : 0;
        int locType = location.getLocType();
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float accuracy = location.getRadius();
        onLocationSample(lat, lng, accuracy, speed, now, locType);
    }

    private void onLocationSample(double rawLat, double rawLng, float accuracy,
                                  float speed, long timestamp, int locType) {
        if (currentRecord == null || currentRecord.getStatus() != WorkoutStatus.RUNNING) {
            return;
        }

        if (!initialLocationReceived) {
            latFilter.reset(rawLat, accuracy);
            lonFilter.reset(rawLng, accuracy);
            smoothedLat = rawLat;
            smoothedLon = rawLng;
            lastAcceptedLat = rawLat;
            lastAcceptedLon = rawLng;
            currentLat = rawLat;
            currentLon = rawLng;
            currentAccuracy = accuracy;
            gpsUpdateCount++;
            lastLocationUpdateTime = timestamp;
            lastPointTime = timestamp;
            stationaryCount = 0;
            consecutiveRejections = 0;
            consecutiveLowAccuracyCount = 0;
            accumulatedDistance = 0;
            initialLocationReceived = true;
            restartWatchdog();
            gpsTimeoutFallback = false;
            if (locType == BDLocation.TypeGpsLocation) {
                Log.i(TAG, "First GPS fix: lat=" + rawLat + " lng=" + rawLng
                        + " acc=" + accuracy + "m");
            } else {
                Log.i(TAG, "First location (locType=" + locType + "): lat=" + rawLat
                        + " lng=" + rawLng + " acc=" + accuracy + "m");
            }
            if (listener != null) {
                listener.onGpsFixAcquired(rawLat, rawLng, accuracy);
            }
            updateLiveMetrics(timestamp, accumulatedDistance);
            return;
        }

        gpsUpdateCount++;
        lastLocationUpdateTime = timestamp;
        restartWatchdog();

        double rawDist = DistanceCalculator.haversineDistance(
                lastAcceptedLat, lastAcceptedLon, rawLat, rawLng);
        double timeDeltaSec = (timestamp - lastPointTime) / 1000.0;

        if (speed < MIN_SPEED_THRESHOLD && rawDist < 10.0) {
            Log.d(TAG, "Speed hard filter: speed=" + speed + "m/s rawDist="
                    + String.format("%.1f", rawDist) + "m (stationary drift)");
            return;
        }
        if (speed > MAX_SPEED_THRESHOLD && rawDist > 50.0) {
            Log.w(TAG, "GPS spike rejected: speed=" + speed + "m/s rawDist="
                    + String.format("%.1f", rawDist) + "m");
            return;
        }

        if (timeDeltaSec < 3.0 && rawDist > MAX_JUMP_METERS) {
            consecutiveRejections++;
            Log.w(TAG, "Jump rejected #" + consecutiveRejections + ": "
                    + String.format("%.0f", rawDist) + "m in "
                    + String.format("%.1f", timeDeltaSec) + "s");
            if (consecutiveRejections >= MAX_CONSECUTIVE_REJECTIONS) {
                Log.w(TAG, "Deadlock detected, forcing accept (no distance accumulated)");
            } else {
                return;
            }
        }

        boolean isDeadlockRecovery = consecutiveRejections >= MAX_CONSECUTIVE_REJECTIONS;
        if (isDeadlockRecovery) {
            consecutiveRejections = 0;
            lastPointTime = timestamp;
            latFilter.reset(rawLat, accuracy);
            lonFilter.reset(rawLng, accuracy);
        } else {
            consecutiveRejections = 0;
        }

        smoothedLat = latFilter.update(rawLat, accuracy);
        smoothedLon = lonFilter.update(rawLng, accuracy);

        currentLat = smoothedLat;
        currentLon = smoothedLon;
        currentAccuracy = accuracy;

        boolean isGpsLocation = (locType == BDLocation.TypeGpsLocation);
        boolean isNetworkLocation = (locType == BDLocation.TypeNetWorkLocation
                || locType == BDLocation.TypeCacheLocation
                || locType == BDLocation.TypeOffLineLocation);

        double effectiveMaxAccuracy;
        if (isGpsLocation) {
            effectiveMaxAccuracy = MAX_ACCEPTABLE_ACCURACY;
        } else if (isNetworkLocation && gpsSearchStartTime > 0
                && System.currentTimeMillis() - gpsSearchStartTime > GPS_FIRST_FIX_TIMEOUT_MS) {
            effectiveMaxAccuracy = MAX_ACCEPTABLE_ACCURACY_FALLBACK;
        } else {
            effectiveMaxAccuracy = MAX_ACCEPTABLE_ACCURACY;
        }

        boolean lowAccuracy = accuracy > effectiveMaxAccuracy;

        if (lowAccuracy && locType != BDLocation.TypeGpsLocation) {
            consecutiveLowAccuracyCount++;
            if (consecutiveLowAccuracyCount > MAX_LOW_ACCURACY_BURST) {
                consecutiveLowAccuracyCount = MAX_LOW_ACCURACY_BURST;
            }
        } else {
            consecutiveLowAccuracyCount = Math.max(0, consecutiveLowAccuracyCount - 1);
        }

        boolean isCurrentlyStationary = false;
        if (speed < MIN_MOVE_SPEED_MS && rawDist < 5.0) {
            stationaryCount++;
            if (stationaryCount >= 3) {
                isCurrentlyStationary = true;
            }
        } else if (speed >= MIN_MOVE_SPEED_MS || rawDist >= 5.0) {
            stationaryCount = Math.max(0, stationaryCount - 1);
        }

        boolean shouldSaveTrackPoint;
        if (isGpsLocation) {
            shouldSaveTrackPoint = !isDeadlockRecovery && !isCurrentlyStationary
                    && !lowAccuracy && rawDist >= 2.0;
        } else {
            shouldSaveTrackPoint = !isDeadlockRecovery && !isCurrentlyStationary
                    && !lowAccuracy && rawDist >= 2.0;
        }

        if (shouldSaveTrackPoint) {
            TrackPoint point = new TrackPoint(store.nextTrackPointId(),
                    currentRecord.getRecordId(), currentLat, currentLon, stepCount,
                    timestamp, accuracy);
            store.saveTrackPoint(point);
            consecutiveLowAccuracyCount = 0;

            double smoothedDist = DistanceCalculator.haversineDistance(
                    lastAcceptedLat, lastAcceptedLon, smoothedLat, smoothedLon);

            double stepDist = stepCount * userStepLengthM;
            double deltaDist;

            double effectiveSpeed = (speed > 0 && speed < 20) ? speed : 3.33;
            double expectedDist = effectiveSpeed * timeDeltaSec;

            if (smoothedDist > expectedDist * 3 && smoothedDist > 30) {
                deltaDist = expectedDist;
            } else {
                deltaDist = smoothedDist;
            }

            if (stepDist > 0 && smoothedDist > 5.0
                    && Math.abs(smoothedDist - (stepDist - accumulatedDistance)) > 0.3 * smoothedDist) {
                Log.d(TAG, "Step validation override: GPS dist=" + String.format("%.1f", deltaDist)
                        + "m, step total=" + String.format("%.1f", stepDist)
                        + "m, acc dist=" + String.format("%.1f", accumulatedDistance) + "m");
                accumulatedDistance = Math.max(accumulatedDistance, stepDist);
            } else {
                accumulatedDistance += deltaDist;
            }

            lastAcceptedLat = smoothedLat;
            lastAcceptedLon = smoothedLon;
        }

        if (!isDeadlockRecovery && !isCurrentlyStationary && !lowAccuracy && rawDist < 2.0) {
            double stepDist = stepCount * userStepLengthM;
            if (stepDist > accumulatedDistance) {
                accumulatedDistance = stepDist;
            }
        }

        lastPointTime = timestamp;

        String locSource = isGpsLocation ? "GPS" : (isNetworkLocation ? "NET" : "MIX");
        Log.d(TAG, "Loc[" + locSource + "]: lat=" + String.format("%.6f", currentLat)
                + " lng=" + String.format("%.6f", currentLon)
                + " rawDist=" + String.format("%.1f", rawDist) + "m"
                + " acc=" + accuracy + "m"
                + " spd=" + String.format("%.1f", speed) + "m/s"
                + " stationary=" + isCurrentlyStationary
                + " lowAcc=" + lowAccuracy
                + " steps=" + stepCount
                + " totalDist=" + String.format("%.1f", accumulatedDistance) + "m");

        updateLiveMetrics(timestamp, accumulatedDistance);
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

    private void startBaiduLocation() {
        if (!baiduLocationEnabled || locationClient == null) {
            startDemoLocation();
            return;
        }
        if (!locationStarted && locationClient != null) {
            locationClient.start();
            locationStarted = true;
            Log.i(TAG, "Baidu LocationClient started (Hight_Accuracy mode)");
        }
    }

    private void stopBaiduLocation() {
        if (!baiduLocationEnabled || locationClient == null) {
            stopDemoLocation();
            return;
        }
        if (locationStarted && locationClient != null) {
            locationClient.stop();
            locationStarted = false;
            Log.i(TAG, "Baidu LocationClient stopped");
        }
    }

    private void startTicker() {
        stopTicker();
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentRecord != null && currentRecord.getStatus() == WorkoutStatus.RUNNING) {
                    long now = System.currentTimeMillis();
                    int activeDuration = (int) ((now - currentRecord.getStartTime() - pausedDurationMs) / 1000);
                    if (activeDuration <= 0) {
                        activeDuration = 1;
                    }
                    currentRecord.setTotalDurationSeconds(activeDuration);
                    double pace = DistanceCalculator.calculatePaceSecondsPerKm(
                            accumulatedDistance, activeDuration);
                    currentRecord.setAvgPaceSecondsPerKm(pace);
                    if (listener != null) {
                        listener.onTick(currentRecord, pace);
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(tickerRunnable);
    }

    private void stopTicker() {
        if (tickerRunnable != null) {
            handler.removeCallbacks(tickerRunnable);
            tickerRunnable = null;
        }
    }

    private void startDemoLocation() {
        if (demoLocationRunnable != null) {
            return;
        }
        locationStarted = true;
        lastLocType = BDLocation.TypeGpsLocation;
        demoLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentRecord == null || currentRecord.getStatus() != WorkoutStatus.RUNNING) {
                    return;
                }
                demoTick++;
                double lat = FALLBACK_LAT + demoTick * 0.00003;
                double lon = FALLBACK_LON + Math.sin(demoTick / 5.0) * 0.00002;
                onLocationSample(lat, lon, 8.0f, 2.4f, System.currentTimeMillis(),
                        BDLocation.TypeGpsLocation);
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(demoLocationRunnable);
        Log.i(TAG, "Demo location started");
    }

    private void stopDemoLocation() {
        if (demoLocationRunnable != null) {
            handler.removeCallbacks(demoLocationRunnable);
            demoLocationRunnable = null;
        }
        locationStarted = false;
        Log.i(TAG, "Demo location stopped");
    }

    private void startStepCounter() {
        if (sensorManager == null) {
            return;
        }
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) {
            stepCountStart = -1;
            sensorManager.registerListener(stepListener, stepSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            Log.i(TAG, "Step counter sensor registered");
        } else {
            Log.w(TAG, "Step counter sensor not available");
        }
    }

    private void stopStepCounter() {
        if (sensorManager != null) {
            try {
                sensorManager.unregisterListener(stepListener);
            } catch (Exception e) {
                Log.w(TAG, "Step counter unregister error: " + e.getMessage());
            }
        }
    }

    private final SensorEventListener stepListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values.length > 0 && currentRecord != null
                    && currentRecord.getStatus() == WorkoutStatus.RUNNING) {
                int totalSteps = (int) event.values[0];
                if (stepCountStart < 0) {
                    stepCountStart = totalSteps;
                }
                int newStepCount = totalSteps - stepCountStart;
                if (newStepCount != stepCount) {
                    stepCount = newStepCount;
                    handler.post(() -> {
                        if (currentRecord != null
                                && currentRecord.getStatus() == WorkoutStatus.RUNNING) {
                            updateLiveMetrics(System.currentTimeMillis(), accumulatedDistance);
                        }
                    });
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private Runnable watchdogRunnable;
    private int watchdogRestartCount;
    private static final int WATCHDOG_MAX_RESTARTS = 3;
    private static final int WATCHDOG_IDLE_TIMEOUT_MS = 30000;

    private void startWatchdog() {
        if (!baiduLocationEnabled) {
            return;
        }
        stopWatchdog();
        watchdogRestartCount = 0;
        watchdogRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - lastLocationUpdateTime;
                if (elapsed > WATCHDOG_IDLE_TIMEOUT_MS && currentRecord != null
                        && currentRecord.getStatus() == WorkoutStatus.RUNNING) {
                    if (watchdogRestartCount < WATCHDOG_MAX_RESTARTS) {
                        watchdogRestartCount++;
                        Log.w(TAG, "Watchdog: no location update for " + (elapsed / 1000)
                                + "s, restarting Baidu LocationClient (attempt "
                                + watchdogRestartCount + "/" + WATCHDOG_MAX_RESTARTS + ")");
                        stopBaiduLocation();
                        handler.postDelayed(() -> {
                            startBaiduLocation();
                            lastLocationUpdateTime = System.currentTimeMillis();
                        }, 2000);
                    } else {
                        Log.w(TAG, "Watchdog: max restarts reached, GPS acquisition may be blocked."
                                + " Accepting any available location.");
                        if (!initialLocationReceived) {
                            gpsTimeoutFallback = true;
                        }
                    }
                }
                handler.postDelayed(this, 10000);
            }
        };
        handler.postDelayed(watchdogRunnable, WATCHDOG_IDLE_TIMEOUT_MS);
    }

    private void stopWatchdog() {
        if (watchdogRunnable != null) {
            handler.removeCallbacks(watchdogRunnable);
            watchdogRunnable = null;
        }
    }

    private void restartWatchdog() {
        if (watchdogRunnable != null) {
            handler.removeCallbacks(watchdogRunnable);
            watchdogRestartCount = 0;
            handler.postDelayed(watchdogRunnable, WATCHDOG_IDLE_TIMEOUT_MS);
        }
    }
}
