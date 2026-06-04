package com.health.sports.model;

import java.util.Date;

public class SyncRecord {
    private final long recordId;
    private final long userId;
    private final String sourceType;
    private String status;
    private final Date startedAt;
    private Date completedAt;
    private int retryCount;
    private int dataSyncedCount;
    private String errorMessage;
    private Date nextRetryAt;

    public SyncRecord(long recordId, long userId, String sourceType, Date startedAt) {
        this.recordId = recordId;
        this.userId = userId;
        this.sourceType = sourceType;
        this.status = "SYNCING";
        this.startedAt = startedAt;
        this.retryCount = 0;
        this.dataSyncedCount = 0;
    }

    public long getRecordId() {
        return recordId;
    }

    public long getUserId() {
        return userId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getDataSyncedCount() {
        return dataSyncedCount;
    }

    public void setDataSyncedCount(int dataSyncedCount) {
        this.dataSyncedCount = dataSyncedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Date nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
}
