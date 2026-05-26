package com.health.modulea;

import com.health.modulea.model.ActivityLevel;
import com.health.modulea.model.ApiException;
import com.health.modulea.model.Gender;
import com.health.modulea.model.HealthProfile;
import com.health.modulea.model.User;
import com.health.modulea.service.AccountService;
import com.health.modulea.service.HealthProfileService;
import com.health.modulea.service.PasswordHasher;
import com.health.modulea.store.InMemoryStore;

import java.time.LocalDate;

public class ModuleATest {
    public static void main(String[] args) {
        InMemoryStore store = new InMemoryStore();
        AccountService accountService = new AccountService(store, new PasswordHasher());
        HealthProfileService profileService = new HealthProfileService(store, accountService);

        registerAndLogin(accountService);
        duplicateMobileShouldFail(accountService);
        profileShouldCalculateBmi(accountService, profileService);
        invalidProfileShouldFail(accountService, profileService);
        updateUserProfile(accountService);

        System.out.println("All module A tests passed.");
    }

    private static void registerAndLogin(AccountService accountService) {
        accountService.sendVerificationCode("13800138000");
        User user = accountService.register("13800138000", "123456", "Pass1234", "alice");
        User loggedIn = accountService.login("13800138000", "Pass1234");
        assertEquals(user.getUserId(), loggedIn.getUserId(), "登录用户ID应与注册用户一致");
    }

    private static void duplicateMobileShouldFail(AccountService accountService) {
        accountService.sendVerificationCode("13900139000");
        accountService.register("13900139000", "123456", "Pass1234", "bob");
        expectApiException(409, new Runnable() {
            public void run() {
                accountService.register("13900139000", "123456", "Pass1234", "bob2");
            }
        });
    }

    private static void profileShouldCalculateBmi(AccountService accountService, HealthProfileService profileService) {
        accountService.sendVerificationCode("13700137000");
        User user = accountService.register("13700137000", "123456", "Pass1234", "cindy");
        HealthProfile profile = profileService.saveProfile(user.getUserId(), Gender.FEMALE,
                LocalDate.of(2002, 6, 1), 165, 55.0, ActivityLevel.MODERATE);
        assertEquals(20.2, profile.calcBMI(), "BMI计算结果错误");
        assertEquals("NORMAL", profile.bmiLevel(), "BMI等级错误");
    }

    private static void invalidProfileShouldFail(AccountService accountService, HealthProfileService profileService) {
        accountService.sendVerificationCode("13600136000");
        User user = accountService.register("13600136000", "123456", "Pass1234", "david");
        expectApiException(400, new Runnable() {
            public void run() {
                profileService.saveProfile(user.getUserId(), Gender.MALE,
                        LocalDate.of(2000, 1, 1), 20, 70, ActivityLevel.HIGH);
            }
        });
    }

    private static void updateUserProfile(AccountService accountService) {
        accountService.sendVerificationCode("13500135000");
        User user = accountService.register("13500135000", "123456", "Pass1234", "eric");
        accountService.updateUserProfile(user.getUserId(), "eric-new", "https://example.com/avatar.png");
        User updated = accountService.getUser(user.getUserId());
        assertEquals("eric-new", updated.getNickname(), "昵称更新失败");
        assertEquals("https://example.com/avatar.png", updated.getAvatarUrl(), "头像地址更新失败");
    }

    private static void expectApiException(int statusCode, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected ApiException " + statusCode);
        } catch (ApiException e) {
            assertEquals(statusCode, e.getStatusCode(), "异常状态码不匹配");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ", expected=" + expected + ", actual=" + actual);
        }
    }
}
