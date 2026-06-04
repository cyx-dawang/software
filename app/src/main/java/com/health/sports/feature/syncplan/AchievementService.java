package com.health.sports.feature.syncplan;

import android.util.Log;

import com.health.sports.feature.workout.WorkoutService;
import com.health.sports.model.WorkoutRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementService {
    private static final String TAG = "AchievementService";

    private final WorkoutService workoutService;

    private final Map<Long, UserAchievements> userAchievementsMap = new HashMap<>();

    public AchievementService(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    public AchievementSummary getAchievementSummary(long userId) {
        UserAchievements achievements = getUserAchievements(userId);
        List<WorkoutRecord> records = workoutService.findUserRecords(userId);

        int totalWorkouts = records.size();
        double totalDistance = 0;
        int totalDurationSeconds = 0;
        double totalCalories = 0;

        for (WorkoutRecord record : records) {
            totalDistance += record.getTotalDistanceKm();
            totalDurationSeconds += record.getTotalDurationSeconds();
            totalCalories += record.getTotalCalories();
        }

        return new AchievementSummary(
                userId,
                totalWorkouts,
                Math.round(totalDistance * 10) / 10.0,
                totalDurationSeconds,
                Math.round(totalCalories),
                achievements.getUnlockedBadgeCount(),
                achievements.getUnlockedBadges(),
                new Date()
        );
    }

    public List<Badge> getUnlockedBadges(long userId) {
        UserAchievements achievements = getUserAchievements(userId);
        return new ArrayList<>(achievements.unlockedBadges.values());
    }

    public List<Badge> getAllAvailableBadges() {
        List<Badge> badges = new ArrayList<>();
        badges.add(new Badge("FIRST_WORKOUT", "初次运动", "完成第一次运动", "🏃"));
        badges.add(new Badge("SEVEN_DAYS", "七日连续", "连续运动7天", "📅"));
        badges.add(new Badge("THIRTY_DAYS", "月度坚持", "连续运动30天", "⭐"));
        badges.add(new Badge("HALF_MARATHON", "半马达成", "单次跑步超过21公里", "🏆"));
        badges.add(new Badge("MARATHON", "全马达成", "单次跑步超过42公里", "👑"));
        badges.add(new Badge("ONE_HUNDRED_KM", "百公里俱乐部", "累计跑步超过100公里", "💯"));
        badges.add(new Badge("FIVE_HUNDRED_KM", "五百公里俱乐部", "累计跑步超过500公里", "🔥"));
        badges.add(new Badge("CALORIES_KILLER", "卡路里杀手", "累计消耗超过10000卡路里", "⚡"));
        return badges;
    }

    public Badge checkNewBadge(long userId) {
        UserAchievements achievements = getUserAchievements(userId);
        List<WorkoutRecord> records = workoutService.findUserRecords(userId);

        double totalDistance = 0;
        double totalCalories = 0;
        int totalWorkouts = records.size();

        for (WorkoutRecord record : records) {
            totalDistance += record.getTotalDistanceKm();
            totalCalories += record.getTotalCalories();
        }

        String[] badgeIds = {
                "FIRST_WORKOUT",
                "SEVEN_DAYS",
                "THIRTY_DAYS",
                "HALF_MARATHON",
                "MARATHON",
                "ONE_HUNDRED_KM",
                "FIVE_HUNDRED_KM",
                "CALORIES_KILLER"
        };

        for (String badgeId : badgeIds) {
            if (!achievements.unlockedBadges.containsKey(badgeId)) {
                Badge badge = unlockBadgeIfConditionMet(userId, badgeId, totalWorkouts, totalDistance, totalCalories);
                if (badge != null) {
                    return badge;
                }
            }
        }
        return null;
    }

    private Badge unlockBadgeIfConditionMet(long userId, String badgeId, int totalWorkouts,
                                            double totalDistance, double totalCalories) {
        UserAchievements achievements = getUserAchievements(userId);

        switch (badgeId) {
            case "FIRST_WORKOUT":
                if (totalWorkouts >= 1) {
                    Badge badge = new Badge("FIRST_WORKOUT", "初次运动", "完成第一次运动", "🏃");
                    achievements.unlockedBadges.put("FIRST_WORKOUT", badge);
                    return badge;
                }
                break;

            case "SEVEN_DAYS":
                if (isConsecutiveDays(records, 7)) {
                    Badge badge = new Badge("SEVEN_DAYS", "七日连续", "连续运动7天", "📅");
                    achievements.unlockedBadges.put("SEVEN_DAYS", badge);
                    return badge;
                }
                break;

            case "THIRTY_DAYS":
                if (isConsecutiveDays(records, 30)) {
                    Badge badge = new Badge("THIRTY_DAYS", "月度坚持", "连续运动30天", "⭐");
                    achievements.unlockedBadges.put("THIRTY_DAYS", badge);
                    return badge;
                }
                break;

            case "HALF_MARATHON":
                for (WorkoutRecord record : records) {
                    if (record.getTotalDistanceKm() >= 21.0) {
                        Badge badge = new Badge("HALF_MARATHON", "半马达成", "单次跑步超过21公里", "🏆");
                        achievements.unlockedBadges.put("HALF_MARATHON", badge);
                        return badge;
                    }
                }
                break;

            case "MARATHON":
                for (WorkoutRecord record : records) {
                    if (record.getTotalDistanceKm() >= 42.0) {
                        Badge badge = new Badge("MARATHON", "全马达成", "单次跑步超过42公里", "👑");
                        achievements.unlockedBadges.put("MARATHON", badge);
                        return badge;
                    }
                }
                break;

            case "ONE_HUNDRED_KM":
                if (totalDistance >= 100.0) {
                    Badge badge = new Badge("ONE_HUNDRED_KM", "百公里俱乐部", "累计跑步超过100公里", "💯");
                    achievements.unlockedBadges.put("ONE_HUNDRED_KM", badge);
                    return badge;
                }
                break;

            case "FIVE_HUNDRED_KM":
                if (totalDistance >= 500.0) {
                    Badge badge = new Badge("FIVE_HUNDRED_KM", "五百公里俱乐部", "累计跑步超过500公里", "🔥");
                    achievements.unlockedBadges.put("FIVE_HUNDRED_KM", badge);
                    return badge;
                }
                break;

            case "CALORIES_KILLER":
                if (totalCalories >= 10000) {
                    Badge badge = new Badge("CALORIES_KILLER", "卡路里杀手", "累计消耗超过10000卡路里", "⚡");
                    achievements.unlockedBadges.put("CALORIES_KILLER", badge);
                    return badge;
                }
                break;
        }
        return null;
    }

    private boolean isConsecutiveDays(List<WorkoutRecord> records, int days) {
        if (records == null || records.size() < days) {
            return false;
        }
        return true;
    }

    public String generateSharePoster(long userId, String templateType) {
        AchievementSummary summary = getAchievementSummary(userId);
        StringBuilder poster = new StringBuilder();

        switch (templateType) {
            case "SIMPLE":
                poster.append("🏃 我的运动成就\n");
                poster.append("──────────────\n");
                poster.append("累计运动: ").append(summary.totalWorkouts).append("次\n");
                poster.append("累计里程: ").append(String.format("%.1f", summary.totalDistanceKm)).append("公里\n");
                poster.append("累计消耗: ").append(summary.totalCalories).append("卡路里\n");
                poster.append("解锁徽章: ").append(summary.unlockedBadgeCount).append("个\n");
                poster.append("──────────────\n");
                poster.append("#健康运动 #坚持打卡");
                break;

            case "DETAILED":
                poster.append("╔══════════════════╗\n");
                poster.append("║    🏃 运动达人    ║\n");
                poster.append("╚══════════════════╝\n");
                poster.append("\n📊 成绩统计\n");
                poster.append("──────────────\n");
                poster.append("运动次数: ").append(summary.totalWorkouts).append("次\n");
                poster.append("累计里程: ").append(String.format("%.1f", summary.totalDistanceKm)).append(" km\n");
                poster.append("运动时长: ").append(formatDuration(summary.totalDurationSeconds)).append("\n");
                poster.append("消耗热量: ").append(summary.totalCalories).append(" 卡\n");
                poster.append("\n🎖️ 已解锁徽章\n");
                poster.append("──────────────\n");
                for (Badge badge : summary.unlockedBadges) {
                    poster.append(badge.icon).append(" ").append(badge.name).append("\n");
                }
                poster.append("\n💪 继续加油！");
                break;

            case "MINIMAL":
            default:
                poster.append("运动打卡 ✅\n");
                poster.append(String.format("%.1f", summary.totalDistanceKm)).append(" km | ");
                poster.append(summary.totalCalories).append(" 卡 | ");
                poster.append(summary.unlockedBadgeCount).append(" 徽章");
                break;
        }

        return poster.toString();
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private UserAchievements getUserAchievements(long userId) {
        return userAchievementsMap.computeIfAbsent(userId, k -> new UserAchievements());
    }

    private static class UserAchievements {
        final Map<String, Badge> unlockedBadges = new HashMap<>();

        int getUnlockedBadgeCount() {
            return unlockedBadges.size();
        }

        List<Badge> getUnlockedBadges() {
            return new ArrayList<>(unlockedBadges.values());
        }
    }

    public static class AchievementSummary {
        public final long userId;
        public final int totalWorkouts;
        public final double totalDistanceKm;
        public final int totalDurationSeconds;
        public final int totalCalories;
        public final int unlockedBadgeCount;
        public final List<Badge> unlockedBadges;
        public final Date generatedAt;

        public AchievementSummary(long userId, int totalWorkouts, double totalDistanceKm,
                                 int totalDurationSeconds, int totalCalories,
                                 int unlockedBadgeCount, List<Badge> unlockedBadges, Date generatedAt) {
            this.userId = userId;
            this.totalWorkouts = totalWorkouts;
            this.totalDistanceKm = totalDistanceKm;
            this.totalDurationSeconds = totalDurationSeconds;
            this.totalCalories = totalCalories;
            this.unlockedBadgeCount = unlockedBadgeCount;
            this.unlockedBadges = unlockedBadges;
            this.generatedAt = generatedAt;
        }
    }

    public static class Badge {
        public final String badgeId;
        public final String name;
        public final String description;
        public final String icon;

        public Badge(String badgeId, String name, String description, String icon) {
            this.badgeId = badgeId;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }
    }
}
