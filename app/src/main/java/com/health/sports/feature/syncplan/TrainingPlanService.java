package com.health.sports.feature.syncplan;

import android.util.Log;

import com.health.sports.feature.profile.HealthProfileService;
import com.health.sports.model.ApiException;
import com.health.sports.model.HealthProfile;
import com.health.sports.model.TrainingDay;
import com.health.sports.model.TrainingPlan;
import com.health.sports.store.InMemoryStore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainingPlanService {
    private static final String TAG = "TrainingPlanService";

    private final InMemoryStore store;
    private final HealthProfileService profileService;

    private final Map<Long, List<TrainingPlan>> plansByUserId = new HashMap<>();

    public TrainingPlanService(InMemoryStore store, HealthProfileService profileService) {
        this.store = store;
        this.profileService = profileService;
    }

    public TrainingPlan createPlan(long userId, String planType) {
        String planName = generatePlanName(planType);
        long planId = store.nextRecordId();
        TrainingPlan plan = new TrainingPlan(planId, userId, planName, planType);

        plan.setGoalDescription(generateGoalDescription(planType));
        plan.getTips().addAll(generateTips(planType));
        generateTrainingDays(plan, planType);

        plansByUserId.computeIfAbsent(userId, k -> new ArrayList<>()).add(plan);
        Log.i(TAG, "创建训练计划: userId=" + userId + ", planId=" + planId + ", type=" + planType);
        return plan;
    }

    public TrainingPlan getPlan(long planId) {
        for (List<TrainingPlan> plans : plansByUserId.values()) {
            for (TrainingPlan plan : plans) {
                if (plan.getPlanId() == planId) {
                    return plan;
                }
            }
        }
        throw new ApiException(404, "训练计划不存在");
    }

    public List<TrainingPlan> getUserPlans(long userId) {
        return plansByUserId.getOrDefault(userId, new ArrayList<>());
    }

    public TrainingPlan updatePlanStatus(long planId, String newStatus) {
        TrainingPlan plan = getPlan(planId);
        String oldStatus = plan.getStatus();

        if (!isValidTransition(oldStatus, newStatus)) {
            throw new ApiException(400, "状态转换不允许: " + oldStatus + " -> " + newStatus);
        }

        plan.setStatus(newStatus);
        Log.i(TAG, "更新计划状态: planId=" + planId + ", " + oldStatus + " -> " + newStatus);
        return plan;
    }

    public TrainingPlan completeDay(long planId, int dayIndex, int actualDuration, double actualDistance) {
        TrainingPlan plan = getPlan(planId);

        if (plan.getStatus().equals("PAUSED")) {
            throw new ApiException(400, "计划已暂停，请先恢复");
        }

        if (dayIndex < 1 || dayIndex > plan.getTotalDays()) {
            throw new ApiException(400, "无效的训练天数");
        }

        List<TrainingDay> days = plan.getTrainingDays();
        if (dayIndex <= days.size()) {
            TrainingDay day = days.get(dayIndex - 1);
            if (day.isCompleted()) {
                throw new ApiException(400, "该天已完成");
            }
            day.setCompleted(true);
            day.setActualDurationMinutes(actualDuration);
            day.setActualDistanceKm(actualDistance);
            plan.setCompletedDays(plan.getCompletedDays() + 1);

            if (plan.getCompletedDays() == plan.getTotalDays()) {
                plan.setStatus("COMPLETED");
            }
            Log.i(TAG, "完成训练日: planId=" + planId + ", day=" + dayIndex);
        }
        return plan;
    }

    public void deletePlan(long planId) {
        TrainingPlan plan = getPlan(planId);
        List<TrainingPlan> plans = plansByUserId.get(plan.getUserId());
        plans.removeIf(p -> p.getPlanId() == planId);
        Log.i(TAG, "删除训练计划: planId=" + planId);
    }

    private String generatePlanName(String planType) {
        Map<String, String> names = new HashMap<>();
        names.put("BEGINNER_RUN", "新手跑步入门计划");
        names.put("INTERMEDIATE_RUN", "进阶跑步提升计划");
        names.put("MARATHON_TRAINING", "马拉松备战计划");
        names.put("WEIGHT_LOSS", "减脂塑形计划");
        names.put("STRENGTH_TRAINING", "力量训练计划");
        names.put("CARDIO", "心肺功能提升计划");
        return names.getOrDefault(planType, "自定义训练计划");
    }

    private String generateGoalDescription(String planType) {
        Map<String, String> goals = new HashMap<>();
        goals.put("BEGINNER_RUN", "帮助初学者建立跑步习惯，逐步提升耐力");
        goals.put("INTERMEDIATE_RUN", "提升跑步水平，突破个人记录");
        goals.put("MARATHON_TRAINING", "系统训练，备战全程马拉松");
        goals.put("WEIGHT_LOSS", "通过运动达到健康减重目标");
        goals.put("STRENGTH_TRAINING", "增强肌肉力量，塑造健康体态");
        goals.put("CARDIO", "提升心肺功能，增强体能");
        return goals.getOrDefault(planType, "完成训练目标");
    }

    private List<String> generateTips(String planType) {
        List<String> tips = new ArrayList<>();
        tips.add("运动前做好热身准备");
        tips.add("保持规律训练，不要轻易放弃");
        tips.add("注意运动后拉伸恢复");
        tips.add("合理饮食，补充营养");
        return tips;
    }

    private void generateTrainingDays(TrainingPlan plan, String planType) {
        int totalDays = getPlanDuration(planType);
        plan.setTotalDays(totalDays);

        Calendar cal = Calendar.getInstance();
        String startDate = formatDate(cal.getTime());
        plan.setStartDate(startDate);

        for (int i = 1; i <= totalDays; i++) {
            TrainingDay day = new TrainingDay(i, formatDate(cal.getTime()));
            configureDay(day, i, planType);
            plan.getTrainingDays().add(day);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        plan.setEndDate(formatDate(cal.getTime()));
    }

    private int getPlanDuration(String planType) {
        Map<String, Integer> durations = new HashMap<>();
        durations.put("BEGINNER_RUN", 21);
        durations.put("INTERMEDIATE_RUN", 30);
        durations.put("MARATHON_TRAINING", 120);
        durations.put("WEIGHT_LOSS", 60);
        durations.put("STRENGTH_TRAINING", 45);
        durations.put("CARDIO", 30);
        return durations.getOrDefault(planType, 30);
    }

    private void configureDay(TrainingDay day, int dayIndex, String planType) {
        switch (planType) {
            case "BEGINNER_RUN":
                day.setExerciseType("跑步");
                day.setTargetDurationMinutes(20 + (dayIndex / 3) * 5);
                day.setTargetDistanceKm(1.5 + (dayIndex / 3) * 0.5);
                if (dayIndex % 7 == 0 || dayIndex % 7 == 3) {
                    day.setIntensity("LOW");
                    day.setTargetDurationMinutes(day.getTargetDurationMinutes() / 2);
                } else {
                    day.setIntensity("MEDIUM");
                }
                day.setDescription("轻松跑，保持舒适的呼吸节奏");
                break;

            case "INTERMEDIATE_RUN":
                day.setExerciseType("跑步");
                day.setTargetDurationMinutes(30 + (dayIndex / 5) * 10);
                day.setTargetDistanceKm(3.0 + (dayIndex / 5) * 1.0);
                day.setIntensity("HIGH");
                day.setDescription("有一定强度的训练，提升配速");
                break;

            case "MARATHON_TRAINING":
                if (dayIndex % 7 == 0) {
                    day.setExerciseType("休息");
                    day.setTargetDurationMinutes(0);
                    day.setIntensity("REST");
                    day.setDescription("充分休息，恢复体力");
                } else if (dayIndex % 14 == 7) {
                    day.setExerciseType("长距离跑");
                    day.setTargetDurationMinutes(90 + ((dayIndex / 14) * 30));
                    day.setTargetDistanceKm(10 + (dayIndex / 14) * 3);
                    day.setIntensity("HIGH");
                    day.setDescription("长距离耐力训练");
                } else {
                    day.setExerciseType("跑步");
                    day.setTargetDurationMinutes(45);
                    day.setTargetDistanceKm(5);
                    day.setIntensity("MEDIUM");
                    day.setDescription("日常训练，保持状态");
                }
                break;

            case "WEIGHT_LOSS":
                day.setExerciseType("有氧运动");
                day.setTargetDurationMinutes(45);
                day.setIntensity("MEDIUM");
                day.setDescription("持续有氧运动，燃烧卡路里");
                break;

            case "STRENGTH_TRAINING":
                day.setExerciseType("力量训练");
                day.setTargetDurationMinutes(60);
                day.setIntensity("HIGH");
                day.setDescription("全身力量训练，增强肌肉");
                break;

            case "CARDIO":
                day.setExerciseType("心肺训练");
                day.setTargetDurationMinutes(30);
                day.setIntensity("HIGH");
                day.setDescription("高强度间歇训练，提升心肺功能");
                break;

            default:
                day.setExerciseType("运动");
                day.setTargetDurationMinutes(30);
                day.setIntensity("MEDIUM");
                day.setDescription("日常训练");
        }
    }

    private boolean isValidTransition(String fromStatus, String toStatus) {
        if ("ACTIVE".equals(fromStatus)) {
            return "PAUSED".equals(toStatus) || "ABANDONED".equals(toStatus) || "COMPLETED".equals(toStatus);
        }
        if ("PAUSED".equals(fromStatus)) {
            return "ACTIVE".equals(toStatus) || "ABANDONED".equals(toStatus);
        }
        if ("COMPLETED".equals(fromStatus)) {
            return false;
        }
        if ("ABANDONED".equals(fromStatus)) {
            return false;
        }
        return true;
    }

    private String formatDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}
