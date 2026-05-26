package com.health.modulea.controller;

import com.health.modulea.model.HealthProfile;
import com.health.modulea.model.User;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String ok(String dataJson) {
        return "{\"success\":true,\"data\":" + dataJson + "}";
    }

    public static String message(String message) {
        return "{\"success\":true,\"message\":\"" + escape(message) + "\"}";
    }

    public static String error(String message) {
        return "{\"success\":false,\"message\":\"" + escape(message) + "\"}";
    }

    public static String user(User user) {
        return "{"
                + "\"userId\":" + user.getUserId()
                + ",\"mobile\":\"" + escape(user.getMobile()) + "\""
                + ",\"nickname\":\"" + escape(user.getNickname()) + "\""
                + ",\"avatarUrl\":\"" + escape(user.getAvatarUrl()) + "\""
                + ",\"status\":\"" + user.getStatus().name() + "\""
                + "}";
    }

    public static String profile(HealthProfile profile) {
        return "{"
                + "\"userId\":" + profile.getUserId()
                + ",\"gender\":\"" + profile.getGender().name() + "\""
                + ",\"birthDate\":\"" + profile.getBirthDate() + "\""
                + ",\"heightCm\":" + profile.getHeightCm()
                + ",\"weightKg\":" + profile.getWeightKg()
                + ",\"activityLevel\":\"" + profile.getActivityLevel().name() + "\""
                + ",\"bmi\":" + profile.calcBMI()
                + ",\"bmiLevel\":\"" + profile.bmiLevel() + "\""
                + "}";
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
