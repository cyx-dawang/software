package com.health.sports.model;

public class User {
    private final long userId;
    private final String mobile;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private UserStatus status;

    public User(long userId, String mobile, String passwordHash, String nickname) {
        this.userId = userId;
        this.mobile = mobile;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.avatarUrl = "";
        this.status = UserStatus.ACTIVE;
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

    public void updateProfile(String nickname, String avatarUrl) {
        if (nickname != null && nickname.trim().length() > 0) {
            this.nickname = nickname.trim();
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl.trim();
        }
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}

