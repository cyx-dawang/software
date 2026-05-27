package com.health.sports.feature.profile;

import com.health.sports.feature.account.AccountService;
import com.health.sports.model.ActivityLevel;
import com.health.sports.model.ApiException;
import com.health.sports.model.Gender;
import com.health.sports.model.HealthProfile;
import com.health.sports.store.InMemoryStore;

public class HealthProfileService {
    private final InMemoryStore store;
    private final AccountService accountService;

    public HealthProfileService(InMemoryStore store, AccountService accountService) {
        this.store = store;
        this.accountService = accountService;
    }

    public HealthProfile saveProfile(long userId, Gender gender, String birthDate, int heightCm,
                                     double weightKg, ActivityLevel activityLevel) {
        accountService.getUser(userId);
        validateProfile(birthDate, heightCm, weightKg);

        HealthProfile profile = store.findHealthProfile(userId);
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
        HealthProfile profile = store.findHealthProfile(userId);
        if (profile == null) {
            throw new ApiException(404, "健康档案不存在");
        }
        return profile;
    }

    private void validateProfile(String birthDate, int heightCm, double weightKg) {
        if (birthDate == null || !birthDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new ApiException(400, "出生日期格式应为yyyy-MM-dd");
        }
        if (heightCm < 50 || heightCm > 250) {
            throw new ApiException(400, "身高必须在50到250厘米之间");
        }
        if (weightKg < 10 || weightKg > 300) {
            throw new ApiException(400, "体重必须在10到300千克之间");
        }
    }
}

