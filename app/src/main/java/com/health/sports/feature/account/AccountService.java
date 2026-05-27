package com.health.sports.feature.account;

import com.health.sports.model.ApiException;
import com.health.sports.model.User;
import com.health.sports.model.UserStatus;
import com.health.sports.store.InMemoryStore;

import java.util.regex.Pattern;

public class AccountService {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final String DEMO_CODE = "123456";

    private final InMemoryStore store;
    private final PasswordHasher passwordHasher;

    public AccountService(InMemoryStore store, PasswordHasher passwordHasher) {
        this.store = store;
        this.passwordHasher = passwordHasher;
    }

    public String sendVerificationCode(String mobile) {
        validateMobile(mobile);
        store.saveVerificationCode(mobile, DEMO_CODE);
        return DEMO_CODE;
    }

    public User register(String mobile, String code, String password, String nickname) {
        validateMobile(mobile);
        validatePassword(password);
        if (nickname == null || nickname.trim().length() == 0) {
            throw new ApiException(400, "昵称不能为空");
        }
        if (store.mobileExists(mobile)) {
            throw new ApiException(409, "手机号已注册");
        }
        String savedCode = store.findVerificationCode(mobile);
        if (savedCode == null || !savedCode.equals(code)) {
            throw new ApiException(400, "验证码错误");
        }

        User user = new User(store.nextUserId(), mobile, passwordHasher.hash(password), nickname.trim());
        store.saveUser(user);
        return user;
    }

    public User login(String mobile, String password) {
        validateMobile(mobile);
        User user = store.findUserByMobile(mobile);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(403, "用户状态不可登录");
        }
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new ApiException(401, "密码错误");
        }
        return user;
    }

    public User getUser(long userId) {
        User user = store.findUserById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        return user;
    }

    public User updateUserProfile(long userId, String nickname, String avatarUrl) {
        User user = getUser(userId);
        user.updateProfile(nickname, avatarUrl);
        return user;
    }

    private void validateMobile(String mobile) {
        if (mobile == null || !MOBILE_PATTERN.matcher(mobile).matches()) {
            throw new ApiException(400, "手机号格式错误");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 32) {
            throw new ApiException(400, "密码长度必须为6到32位");
        }
    }
}

