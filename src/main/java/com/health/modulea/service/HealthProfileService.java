package com.health.modulea.service;

import com.health.modulea.model.ActivityLevel;
import com.health.modulea.model.ApiException;
import com.health.modulea.model.Gender;
import com.health.modulea.model.HealthProfile;
import com.health.modulea.store.InMemoryStore;

import java.time.LocalDate;

public class HealthProfileService {
    private final InMemoryStore store;
    private final AccountService accountService;

    public HealthProfileService(InMemoryStore store, AccountService accountService) {
        this.store = store;
        this.accountService = accountService;
    }

    public HealthProfile saveProfile(long userId, Gender gender, LocalDate birthDate, int heightCm,
                                     double weightKg, ActivityLevel activityLevel) {
        accountService.getUser(userId);
        validateProfile(birthDate, heightCm, weightKg);

        HealthProfile profile = store.findHealthProfile(userId).orElse(null);
        if (profile == null) {
            profile = new HealthProfile(userId, gender, birthDate, heightCm, weightKg, activityLevel);
        } else {
            profile.update(gender, birthDate, heightCm, weightKg, activityLevel);
        }
        store.saveHealthProfile(profile);
        return profile;
    }

    public HealthProfile getProfile(long userId) {
        accountService.getUser(userId);
        return store.findHealthProfile(userId)
                .orElseThrow(() -> new ApiException(404, "健康档案不存在"));
    }

    private void validateProfile(LocalDate birthDate, int heightCm, double weightKg) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new ApiException(400, "出生日期不合法");
        }
        if (heightCm < 50 || heightCm > 250) {
            throw new ApiException(400, "身高必须在50到250厘米之间");
        }
        if (weightKg < 10 || weightKg > 300) {
            throw new ApiException(400, "体重必须在10到300千克之间");
        }
    }
}
