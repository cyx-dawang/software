package com.health.sports.feature.syncplan;

import android.content.Context;

import com.health.sports.feature.account.AccountService;
import com.health.sports.feature.profile.HealthProfileService;
import com.health.sports.feature.workout.WorkoutService;
import com.health.sports.model.*;
import com.health.sports.store.InMemoryStore;

import java.util.List;
import java.util.Map;

public class ModuleCFacade {
    private static ModuleCFacade instance;

    private final HealthSyncService healthSyncService;
    private final TrainingPlanService trainingPlanService;
    private final AchievementService achievementService;

    private ModuleCFacade(InMemoryStore store, AccountService accountService,
                          HealthProfileService profileService, WorkoutService workoutService) {
        this.healthSyncService = new HealthSyncService(store, accountService, workoutService);
        this.trainingPlanService = new TrainingPlanService(store, profileService);
        this.achievementService = new AchievementService(workoutService);
    }

    public static synchronized ModuleCFacade getInstance(InMemoryStore store,
                                                          AccountService accountService,
                                                          HealthProfileService profileService,
                                                          WorkoutService workoutService) {
        if (instance == null) {
            instance = new ModuleCFacade(store, accountService, profileService, workoutService);
        }
        return instance;
    }

    public void addSyncSource(long userId, String sourceType) {
        healthSyncService.addSyncSource(userId, sourceType);
    }

    public void removeSyncSource(long userId, String sourceType) {
        healthSyncService.removeSyncSource(userId, sourceType);
    }

    public List<?> getSyncSources(long userId) {
        return healthSyncService.getSyncSources(userId);
    }

    public String syncSingleSource(long userId, String sourceType) {
        return healthSyncService.syncSingleSource(userId, sourceType);
    }

    public Map<String, String> syncAllSources(long userId) {
        return healthSyncService.syncAllSources(userId);
    }

    public String getSyncStatus(long userId, String sourceType) {
        return healthSyncService.getSyncStatus(userId, sourceType);
    }

    public List<SyncRecord> getSyncHistory(long userId, int page, int pageSize) {
        return healthSyncService.getSyncHistory(userId, page, pageSize);
    }

    public SyncedHealthData getLatestSyncedData(long userId) {
        return healthSyncService.getLatestSyncedData(userId);
    }

    public TrainingPlan createTrainingPlan(long userId, String planType) {
        return trainingPlanService.createPlan(userId, planType);
    }

    public TrainingPlan getTrainingPlan(long planId) {
        return trainingPlanService.getPlan(planId);
    }

    public List<TrainingPlan> getUserTrainingPlans(long userId) {
        return trainingPlanService.getUserPlans(userId);
    }

    public TrainingPlan updateTrainingPlanStatus(long planId, String newStatus) {
        return trainingPlanService.updatePlanStatus(planId, newStatus);
    }

    public TrainingPlan completeTrainingDay(long planId, int dayIndex, int actualDuration, double actualDistance) {
        return trainingPlanService.completeDay(planId, dayIndex, actualDuration, actualDistance);
    }

    public void deleteTrainingPlan(long planId) {
        trainingPlanService.deletePlan(planId);
    }

    public AchievementService.AchievementSummary getAchievementSummary(long userId) {
        return achievementService.getAchievementSummary(userId);
    }

    public List<AchievementService.Badge> getUnlockedBadges(long userId) {
        return achievementService.getUnlockedBadges(userId);
    }

    public List<AchievementService.Badge> getAllAvailableBadges() {
        return achievementService.getAllAvailableBadges();
    }

    public AchievementService.Badge checkNewBadge(long userId) {
        return achievementService.checkNewBadge(userId);
    }

    public String generateSharePoster(long userId, String templateType) {
        return achievementService.generateSharePoster(userId, templateType);
    }
}
