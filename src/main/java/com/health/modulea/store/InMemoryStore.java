package com.health.modulea.store;

import com.health.modulea.model.HealthProfile;
import com.health.modulea.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryStore {
    private final AtomicLong userIdSequence = new AtomicLong(1000);
    private final Map<Long, User> usersById = new ConcurrentHashMap<Long, User>();
    private final Map<String, Long> userIdsByMobile = new ConcurrentHashMap<String, Long>();
    private final Map<Long, HealthProfile> profilesByUserId = new ConcurrentHashMap<Long, HealthProfile>();
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<String, String>();

    public long nextUserId() {
        return userIdSequence.incrementAndGet();
    }

    public void saveUser(User user) {
        usersById.put(user.getUserId(), user);
        userIdsByMobile.put(user.getMobile(), user.getUserId());
    }

    public Optional<User> findUserById(long userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    public Optional<User> findUserByMobile(String mobile) {
        Long userId = userIdsByMobile.get(mobile);
        return userId == null ? Optional.<User>empty() : findUserById(userId);
    }

    public boolean mobileExists(String mobile) {
        return userIdsByMobile.containsKey(mobile);
    }

    public void saveVerificationCode(String mobile, String code) {
        verificationCodes.put(mobile, code);
    }

    public Optional<String> findVerificationCode(String mobile) {
        return Optional.ofNullable(verificationCodes.get(mobile));
    }

    public void saveHealthProfile(HealthProfile profile) {
        profilesByUserId.put(profile.getUserId(), profile);
    }

    public Optional<HealthProfile> findHealthProfile(long userId) {
        return Optional.ofNullable(profilesByUserId.get(userId));
    }
}
