package com.health.modulea.model;

import java.time.LocalDateTime;

public class User {
    private final long userId;
    private final String mobile;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private UserStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(long userId, String mobile, String passwordHash, String nickname) {
        this.userId = userId;
        this.mobile = mobile;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.avatarUrl = "";
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public long getUserId() {
        return userId;
    }

    public String getMobile() {
        return mobile;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(String nickname, String avatarUrl) {
        if (nickname != null && !nickname.trim().isEmpty()) {
            this.nickname = nickname.trim();
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl.trim();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(UserStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
