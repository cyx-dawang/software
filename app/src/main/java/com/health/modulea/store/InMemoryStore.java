package com.health.modulea.store;

import com.health.modulea.model.HealthProfile;
import com.health.modulea.model.User;

import java.util.HashMap;
import java.util.Map;

public class InMemoryStore {
    private long userIdSequence = 1000;
    private final Map<Long, User> usersById = new HashMap<Long, User>();
    private final Map<String, Long> userIdsByMobile = new HashMap<String, Long>();
    private final Map<Long, HealthProfile> profilesByUserId = new HashMap<Long, HealthProfile>();
    private final Map<String, String> verificationCodes = new HashMap<String, String>();

    public long nextUserId() {
        userIdSequence += 1;
        return userIdSequence;
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
}
