package com.health.sports.feature.syncplan;

import android.util.Log;

import com.health.sports.feature.account.AccountService;
import com.health.sports.feature.workout.WorkoutService;
import com.health.sports.model.ApiException;
import com.health.sports.model.HealthProfile;
import com.health.sports.model.SyncedHealthData;
import com.health.sports.model.SyncRecord;
import com.health.sports.store.InMemoryStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthSyncService {
    private static final String TAG = "HealthSyncService";
    private static final int MAX_RETRY_COUNT = 3;
    private static final double BASE_DELAY_SECONDS = 2.0;
    private static final double MAX_DELAY_SECONDS = 60.0;

    private final InMemoryStore store;
    private final AccountService accountService;
    private final WorkoutService workoutService;

    private final Map<Long, List<SyncRecord>> syncRecordsByUserId = new HashMap<>();
    private final Map<Long, List<SyncedHealthData>> syncedDataByUserId = new HashMap<>();
    private final Map<Long, List<SyncConfig>> syncConfigsByUserId = new HashMap<>();

    public HealthSyncService(InMemoryStore store, AccountService accountService,
                            WorkoutService workoutService) {
        this.store = store;
        this.accountService = accountService;
        this.workoutService = workoutService;
    }

    public void addSyncSource(long userId, String sourceType) {
        List<SyncConfig> configs = syncConfigsByUserId.computeIfAbsent(userId, k -> new ArrayList<>());
        boolean exists = configs.stream().anyMatch(c -> c.sourceType.equals(sourceType));
        if (!exists) {
            configs.add(new SyncConfig(sourceType));
            Log.i(TAG, "用户 " + userId + " 添加同步源: " + sourceType);
        }
    }

    public void removeSyncSource(long userId, String sourceType) {
        List<SyncConfig> configs = syncConfigsByUserId.get(userId);
        if (configs != null) {
            configs.removeIf(c -> c.sourceType.equals(sourceType));
        }
    }

    public List<SyncConfig> getSyncSources(long userId) {
        return syncConfigsByUserId.getOrDefault(userId, new ArrayList<>());
    }

    public String syncSingleSource(long userId, String sourceType) {
        List<SyncConfig> configs = syncConfigsByUserId.get(userId);
        if (configs == null || configs.stream().noneMatch(c -> c.sourceType.equals(sourceType))) {
            Log.w(TAG, "用户 " + userId + " 未配置同步源: " + sourceType);
            return "CANCELLED";
        }

        long recordId = store.nextRecordId();
        SyncRecord record = new SyncRecord(recordId, userId, sourceType, new Date());
        syncRecordsByUserId.computeIfAbsent(userId, k -> new ArrayList<>()).add(record);

        int attempts = 0;
        while (attempts < MAX_RETRY_COUNT) {
            attempts++;
            try {
                Log.i(TAG, "同步尝试 " + attempts + ": userId=" + userId + ", source=" + sourceType);

                SyncedHealthData data = doSync(userId, sourceType);

                syncedDataByUserId.computeIfAbsent(userId, k -> new ArrayList<>()).add(data);

                record.setStatus("SUCCESS");
                record.setCompletedAt(new Date());
                record.setRetryCount(attempts - 1);
                record.setDataSyncedCount(1);
                Log.i(TAG, "同步成功: userId=" + userId + ", source=" + sourceType);
                return "SUCCESS";

            } catch (SyncNetworkError e) {
                if (attempts < MAX_RETRY_COUNT) {
                    double delay = calculateRetryDelay(attempts);
                    record.setNextRetryAt(new Date(System.currentTimeMillis() + (long) (delay * 1000)));
                    Log.w(TAG, "网络错误，" + String.format("%.1f", delay) + "秒后重试: " + e.getMessage());
                } else {
                    record.setStatus("FAILED");
                    record.setCompletedAt(new Date());
                    record.setRetryCount(MAX_RETRY_COUNT);
                    record.setErrorMessage("超过最大重试次数: " + e.getMessage());
                    Log.e(TAG, "同步最终失败: " + record.getErrorMessage());
                    return "FAILED";
                }
            } catch (SyncAuthError e) {
                record.setStatus("FAILED");
                record.setCompletedAt(new Date());
                record.setErrorMessage("授权失败: " + e.getMessage());
                Log.e(TAG, record.getErrorMessage());
                return "FAILED";
            } catch (Exception e) {
                record.setStatus("FAILED");
                record.setCompletedAt(new Date());
                record.setErrorMessage("未知错误: " + e.getMessage());
                Log.e(TAG, record.getErrorMessage());
                return "FAILED";
            }
        }
        return "FAILED";
    }

    public Map<String, String> syncAllSources(long userId) {
        Map<String, String> results = new HashMap<>();
        List<SyncConfig> configs = syncConfigsByUserId.get(userId);
        if (configs != null) {
            for (SyncConfig config : configs) {
                results.put(config.sourceType, syncSingleSource(userId, config.sourceType));
            }
        }
        return results;
    }

    private SyncedHealthData doSync(long userId, String sourceType) throws SyncNetworkError, SyncAuthError {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SyncedHealthData data = new SyncedHealthData(userId, sourceType);

        if (sourceType.startsWith("wearable_")) {
            data.setStepCount(8500);
            data.setHeartRateAvg(72);
            data.setHeartRateMax(145);
            data.setSleepDurationMinutes(420);
            data.setSleepDeepMinutes(120);
            data.setSleepLightMinutes(250);
            data.setCaloriesBurned(320.5);
            data.setBloodOxygen(98.0);
        } else if (sourceType.startsWith("platform_")) {
            data.setHeartRateAvg(68);
            data.setHeartRateMax(155);
            data.setCaloriesBurned(480.0);
        }

        return data;
    }

    private double calculateRetryDelay(int attempt) {
        double delay = BASE_DELAY_SECONDS * Math.pow(2, attempt - 1);
        return Math.min(delay, MAX_DELAY_SECONDS);
    }

    public String getSyncStatus(long userId, String sourceType) {
        List<SyncRecord> records = syncRecordsByUserId.get(userId);
        if (records == null || records.isEmpty()) {
            return "PENDING";
        }
        return records.stream()
                .filter(r -> r.getSourceType().equals(sourceType))
                .max((a, b) -> a.getStartedAt().compareTo(b.getStartedAt()))
                .map(SyncRecord::getStatus)
                .orElse("PENDING");
    }

    public List<SyncRecord> getSyncHistory(long userId, int page, int pageSize) {
        List<SyncRecord> records = syncRecordsByUserId.getOrDefault(userId, new ArrayList<>());
        records.sort((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()));
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, records.size());
        return records.subList(start, end);
    }

    public SyncedHealthData getLatestSyncedData(long userId) {
        List<SyncedHealthData> dataList = syncedDataByUserId.get(userId);
        if (dataList == null || dataList.isEmpty()) {
            return null;
        }
        return dataList.stream()
                .max((a, b) -> a.getSyncTime().compareTo(b.getSyncTime()))
                .orElse(null);
    }

    public List<SyncedHealthData> getAllSyncedData(long userId) {
        return syncedDataByUserId.getOrDefault(userId, new ArrayList<>());
    }

    private static class SyncConfig {
        final String sourceType;
        final int maxRetryCount = MAX_RETRY_COUNT;

        SyncConfig(String sourceType) {
            this.sourceType = sourceType;
        }
    }

    public static class SyncNetworkError extends Exception {
        public SyncNetworkError(String message) {
            super(message);
        }
    }

    public static class SyncAuthError extends Exception {
        public SyncAuthError(String message) {
            super(message);
        }
    }
}
