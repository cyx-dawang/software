package com.health.sports;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;

import com.health.sports.model.ActivityLevel;
import com.health.sports.model.ApiException;
import com.health.sports.model.Gender;
import com.health.sports.model.HealthProfile;
import com.health.sports.model.User;
import com.health.sports.model.WorkoutRecord;
import com.health.sports.model.WorkoutStatus;
import com.health.sports.feature.workout.DistanceCalculator;
import com.health.sports.feature.account.AccountService;
import com.health.sports.feature.profile.HealthProfileService;
import com.health.sports.feature.account.PasswordHasher;
import com.health.sports.feature.workout.WorkoutService;
import com.health.sports.feature.syncplan.TrainingPlanService;
import com.health.sports.feature.syncplan.HealthSyncService;
import com.health.sports.feature.syncplan.AchievementService;
import com.health.sports.model.TrainingPlan;
import com.health.sports.model.TrainingDay;
import com.health.sports.model.SyncedHealthData;
import com.health.sports.store.InMemoryStore;
import com.health.sports.store.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements WorkoutService.WorkoutUpdateListener {
    private static final String TAG = "HealthApp";

    private AccountService accountService;
    private HealthProfileService profileService;
    private WorkoutService workoutService;
    private TrainingPlanService trainingPlanService;
    private HealthSyncService healthSyncService;
    private AchievementService achievementService;
    private User currentUser;

    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout mapSlot;
    private LinearLayout navRow1;
    private LinearLayout navRow2;
    private ScrollView scrollView;
    private TextView titleView;
    private TextView currentUserView;

    private LinearLayout workoutDataPanel;
    private TextView workoutStatusView;
    private TextView workoutDistanceView;
    private TextView workoutPaceView;
    private TextView workoutDurationView;
    private TextView workoutCaloriesView;
    private TextView workoutGpsView;
    private Button workoutPauseBtn;
    private Button workoutResumeBtn;
    private Button workoutEndBtn;
    private Button workoutStartBtn;
    private LinearLayout workoutControls;
    private LinearLayout workoutReportContainer;

    private MapView mapView;
    private BaiduMap baiduMap;
    private List<LatLng> trackLatLngs;
    private boolean mapReady;
    private boolean baiduSdkReady;
    private boolean trackLineDirty;
    private LatLng lastAnimatedPoint;
    private Bundle savedState;

    private EditText registerMobile;
    private EditText registerCode;
    private EditText registerPassword;
    private EditText registerNickname;
    private EditText loginMobile;
    private EditText loginPassword;

    private EditText nicknameInput;
    private EditText avatarInput;
    private Spinner genderInput;
    private EditText birthDateInput;
    private EditText heightInput;
    private EditText weightInput;
    private Spinner activityInput;
    private TextView profileResult;

    private static final int REQUEST_PERMISSIONS = 100;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedState = savedInstanceState;
        initBaiduSdk();
        setContentView(R.layout.activity_main);
        requestRuntimePermissions();

        db = new DatabaseHelper(getApplicationContext());
        db.initSequences();

        InMemoryStore store = new InMemoryStore(db);
        accountService = new AccountService(store, new PasswordHasher());
        profileService = new HealthProfileService(store, accountService);
        workoutService = new WorkoutService(store, accountService, profileService, getApplicationContext());
        workoutService.setListener(this);
        trainingPlanService = new TrainingPlanService(store, profileService);
        healthSyncService = new HealthSyncService(store, accountService, workoutService);
        achievementService = new AchievementService(workoutService);
        trackLatLngs = new ArrayList<>();

        initViews();
        initPersistentMapView();
        showRegisterPage();
    }

    private void initBaiduSdk() {
        try {
            SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
            SDKInitializer.initialize(getApplicationContext());
            LocationClient.setAgreePrivacy(true);
            baiduSdkReady = true;
            Log.i(TAG, "Baidu SDK initialized successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Baidu SDK init failed: " + t.getClass().getSimpleName(), t);
            baiduSdkReady = false;
        }
    }

    private void requestRuntimePermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS,
            }, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) {
            return;
        }
        boolean fineLocationGranted = false;
        boolean coarseLocationGranted = false;
        boolean notificationGranted = false;
        for (int i = 0; i < permissions.length; i++) {
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                fineLocationGranted = granted;
            } else if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                coarseLocationGranted = granted;
            } else if (Manifest.permission.POST_NOTIFICATIONS.equals(permissions[i])) {
                notificationGranted = granted;
            }
        }
        if (!fineLocationGranted) {
            Log.w(TAG, "Fine location permission denied");
            toast("精确定位权限被拒绝，GPS定位和地图功能将不可用");
        } else {
            Log.i(TAG, "Fine location permission granted");
        }
        if (!coarseLocationGranted) {
            Log.w(TAG, "Coarse location permission denied");
        }
        if (!notificationGranted) {
            Log.w(TAG, "Notification permission denied");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (mapView != null) {
                mapView.onResume();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in onResume", t);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (mapView != null) {
                mapView.onPause();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in onPause", t);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mapView != null) {
                Log.i(TAG, "Destroying persistent MapView");
                mapView.onPause();
                mapView.onDestroy();
                mapView = null;
                baiduMap = null;
                mapReady = false;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in onDestroy", t);
        }
        if (workoutService != null) {
            workoutService.shutdown();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    private void initViews() {
        root = findViewById(R.id.root);
        titleView = findViewById(R.id.titleView);
        currentUserView = findViewById(R.id.currentUserView);
        navRow1 = findViewById(R.id.navRow1);
        navRow2 = findViewById(R.id.navRow2);
        mapSlot = findViewById(R.id.mapSlot);
        scrollView = findViewById(R.id.scrollView);
        content = findViewById(R.id.content);

        Button account = navButton("账户");
        Button user = navButton("资料");
        Button profile = navButton("档案");
        Button workout = navButton("运动");
        navRow1.addView(account, weightParams());
        navRow1.addView(user, weightParams());
        navRow1.addView(profile, weightParams());
        navRow1.addView(workout, weightParams());

        account.setOnClickListener(v -> showRegisterPage());
        user.setOnClickListener(v -> showUserPage());
        profile.setOnClickListener(v -> showProfilePage());
        workout.setOnClickListener(v -> showWorkoutPage());

        Button trainingBtn = navButton("训练计划");
        Button dashboardBtn = navButton("健康看板");
        Button syncShareBtn = navButton("同步分享");
        navRow2.addView(trainingBtn, weightParams());
        navRow2.addView(dashboardBtn, weightParams());
        navRow2.addView(syncShareBtn, weightParams());

        trainingBtn.setOnClickListener(v -> showTrainingPlanPage());
        dashboardBtn.setOnClickListener(v -> showDashboardPage());
        syncShareBtn.setOnClickListener(v -> showSyncSharePage());
    }

    private void showRegisterPage() {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("手机号注册"));

        registerMobile = input("手机号，如 13800138000", false);
        registerCode = input("验证码", false);
        registerCode.setText("123456");
        registerPassword = input("密码，至少6位", true);
        registerNickname = input("昵称", false);
        content.addView(registerMobile);
        content.addView(registerCode);
        content.addView(registerPassword);
        content.addView(registerNickname);

        Button codeButton = primaryButton("发送验证码");
        Button registerButton = primaryButton("注册并进入");
        content.addView(codeButton);
        content.addView(registerButton);

        Button switchToLogin = secondaryButton("已有账号？去登录");
        content.addView(switchToLogin);

        codeButton.setOnClickListener(v -> runAction(() -> {
            String code = accountService.sendVerificationCode(registerMobile.getText().toString().trim());
            registerCode.setText(code);
            toast("验证码已生成：" + code);
        }));
        registerButton.setOnClickListener(v -> runAction(() -> {
            User user = accountService.register(text(registerMobile), text(registerCode),
                    text(registerPassword), text(registerNickname));
            setCurrentUser(user);
            toast("注册成功，欢迎 " + user.getNickname());
        }));
        switchToLogin.setOnClickListener(v -> showLoginPage());
    }

    private void showLoginPage() {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("账号密码登录"));

        loginMobile = input("手机号", false);
        loginPassword = input("密码", true);
        content.addView(loginMobile);
        content.addView(loginPassword);

        Button loginButton = primaryButton("登录");
        content.addView(loginButton);

        Button switchToRegister = secondaryButton("没有账号？去注册");
        content.addView(switchToRegister);

        loginButton.setOnClickListener(v -> runAction(() -> {
            User user = accountService.login(text(loginMobile), text(loginPassword));
            setCurrentUser(user);
            toast("登录成功，欢迎回来 " + user.getNickname());
        }));
        switchToRegister.setOnClickListener(v -> showRegisterPage());
    }

    private void showUserPage() {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("个人资料"));
        nicknameInput = input("昵称", false);
        avatarInput = input("头像地址，可选", false);
        content.addView(nicknameInput);
        content.addView(avatarInput);

        Button loadButton = primaryButton("加载当前用户");
        Button saveButton = primaryButton("保存资料");
        content.addView(loadButton);
        content.addView(saveButton);

        loadButton.setOnClickListener(v -> runAction(() -> fillUserFields(requireLogin())));
        saveButton.setOnClickListener(v -> runAction(() -> {
            User user = requireLogin();
            User updated = accountService.updateUserProfile(user.getUserId(), text(nicknameInput), text(avatarInput));
            setCurrentUser(updated);
            toast("个人资料已保存");
        }));
    }

    private void showProfilePage() {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("健康档案"));
        genderInput = spinner(new String[]{"MALE", "FEMALE", "UNKNOWN"});
        birthDateInput = input("出生日期 yyyy-MM-dd", false);
        heightInput = input("身高 cm", false);
        weightInput = input("体重 kg", false);
        activityInput = spinner(new String[]{"LOW", "MODERATE", "HIGH"});
        birthDateInput.setText("2003-01-01");
        heightInput.setText("170");
        weightInput.setText("60");

        content.addView(label("性别"));
        content.addView(genderInput);
        content.addView(birthDateInput);
        content.addView(heightInput);
        content.addView(weightInput);
        content.addView(label("活动水平"));
        content.addView(activityInput);

        Button saveButton = primaryButton("保存健康档案");
        Button loadButton = primaryButton("查询健康档案");
        content.addView(saveButton);
        content.addView(loadButton);

        profileResult = new TextView(this);
        profileResult.setTextColor(Color.rgb(37, 52, 66));
        profileResult.setTextSize(15);
        profileResult.setPadding(dp(14), dp(14), dp(14), dp(14));
        profileResult.setBackgroundColor(Color.WHITE);
        content.addView(profileResult, blockParams());

        saveButton.setOnClickListener(v -> runAction(() -> {
            User user = requireLogin();
            HealthProfile profile = profileService.saveProfile(
                    user.getUserId(),
                    Gender.valueOf(genderInput.getSelectedItem().toString()),
                    text(birthDateInput),
                    Integer.parseInt(text(heightInput)),
                    Double.parseDouble(text(weightInput)),
                    ActivityLevel.valueOf(activityInput.getSelectedItem().toString()));
            showProfile(profile);
            toast("健康档案已保存");
        }));
        loadButton.setOnClickListener(v -> runAction(() -> showProfile(profileService.getProfile(requireLogin().getUserId()))));
    }

    private void showWorkoutPage() {
        cleanupMapView();
        content.removeAllViews();
        WorkoutStatus status = workoutService.getStatus();

        Log.d(TAG, "showWorkoutPage status=" + status + " baiduSdkReady=" + baiduSdkReady);

        if (status == WorkoutStatus.COMPLETED) {
            return;
        }

        if (status == WorkoutStatus.IDLE) {
            workoutService.startPreloading();
            content.addView(sectionTitle("户外跑步"));
            content.addView(label("准备开始一段新的跑步旅程"));

            workoutStartBtn = primaryButton("开始跑步");
            workoutStartBtn.setBackgroundColor(Color.rgb(0, 229, 178));
            workoutStartBtn.setTextColor(Color.rgb(30, 42, 94));
            content.addView(workoutStartBtn);

            workoutStartBtn.setOnClickListener(v -> runAction(() -> {
                User user = requireLogin();
                workoutService.startWorkout(user.getUserId());
            }));
            return;
        }

        content.addView(sectionTitle(status == WorkoutStatus.PAUSED ? "运动已暂停" : "运动进行中"));

        workoutGpsView = new TextView(this);
        workoutGpsView.setText(workoutService.getGpsStatusText());
        workoutGpsView.setTextColor(Color.rgb(102, 102, 102));
        workoutGpsView.setTextSize(12);
        workoutGpsView.setPadding(0, 0, 0, dp(6));
        content.addView(workoutGpsView);

        if (mapSlot != null && mapView != null) {
            mapSlot.setVisibility(View.VISIBLE);
        }

        workoutDataPanel = new LinearLayout(this);
        workoutDataPanel.setOrientation(LinearLayout.VERTICAL);
        content.addView(workoutDataPanel);

        workoutDistanceView = dataMetric("总距离", "-- km", Color.rgb(0, 229, 178));
        workoutDurationView = dataMetric("时长", "--:--", Color.rgb(30, 42, 94));
        workoutPaceView = dataMetric("平均配速", "--'--\"", Color.rgb(255, 127, 80));
        workoutCaloriesView = dataMetric("消耗", "-- kcal", Color.rgb(100, 100, 100));
        workoutStatusView = new TextView(this);
        workoutStatusView.setTextSize(13);
        workoutStatusView.setPadding(0, dp(8), 0, dp(4));

        workoutDataPanel.addView(workoutDistanceView);
        workoutDataPanel.addView(workoutDurationView);
        workoutDataPanel.addView(workoutPaceView);
        workoutDataPanel.addView(workoutCaloriesView);
        workoutDataPanel.addView(workoutStatusView);

        workoutControls = new LinearLayout(this);
        workoutControls.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(workoutControls);

        WorkoutRecord rec = workoutService.getCurrentRecord();
        if (status == WorkoutStatus.RUNNING) {
            workoutPauseBtn = secondaryButton("暂停");
            workoutEndBtn = dangerButton("结束");
            workoutControls.addView(workoutPauseBtn, weightParams());
            workoutControls.addView(workoutEndBtn, weightParams());

            workoutPauseBtn.setOnClickListener(v -> runAction(() -> {
                workoutService.pauseWorkout();
            }));
            workoutEndBtn.setOnClickListener(v -> runAction(() -> {
                workoutService.pauseWorkout();
                showEndConfirmDialog();
            }));

            updateWorkoutLiveData(rec, rec.getAvgPaceSecondsPerKm());
            if (mapReady && workoutService.isGpsFixed()) {
                updateMapTrajectory();
            }
        } else if (status == WorkoutStatus.PAUSED) {
            workoutResumeBtn = primaryButton("继续");
            workoutResumeBtn.setBackgroundColor(Color.rgb(0, 229, 178));
            workoutResumeBtn.setTextColor(Color.rgb(30, 42, 94));
            workoutEndBtn = dangerButton("结束");
            workoutControls.addView(workoutResumeBtn, weightParams());
            workoutControls.addView(workoutEndBtn, weightParams());

            workoutResumeBtn.setOnClickListener(v -> runAction(() -> {
                workoutService.resumeWorkout();
            }));
            workoutEndBtn.setOnClickListener(v -> runAction(() -> showEndConfirmDialog()));

            updateWorkoutLiveData(rec, rec.getAvgPaceSecondsPerKm());
            if (mapReady && workoutService.isGpsFixed()) {
                updateMapTrajectory();
            }
        }
    }

    private void showEndConfirmDialog() {
        content.removeAllViews();
        content.addView(sectionTitle("确认结束运动"));
        content.addView(label("确定要结束本次运动吗？"));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button confirmBtn = dangerButton("确定结束");
        Button cancelBtn = secondaryButton("取消");

        buttons.addView(cancelBtn, weightParams());
        buttons.addView(confirmBtn, weightParams());
        content.addView(buttons);

        confirmBtn.setOnClickListener(v -> runAction(() -> {
            WorkoutRecord finished = workoutService.endWorkout();
            showWorkoutReportPage(finished);
        }));
        cancelBtn.setOnClickListener(v -> runAction(() -> refreshWorkoutPage()));
    }

    private void showWorkoutReportPage(WorkoutRecord record) {
        content.removeAllViews();
        content.addView(sectionTitle("运动总结报告"));

        double distanceKm = record.getTotalDistanceKm();

        TextView dateView = new TextView(this);
        dateView.setText("运动记录 #" + record.getRecordId());
        dateView.setTextColor(Color.rgb(102, 102, 102));
        dateView.setTextSize(13);
        dateView.setPadding(0, 0, 0, dp(12));
        content.addView(dateView);

        workoutReportContainer = new LinearLayout(this);
        workoutReportContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(workoutReportContainer);

        LinearLayout distanceCard = reportCard("总距离", String.format("%.2f", distanceKm) + " km", Color.rgb(0, 229, 178));
        LinearLayout durationCard = reportCard("总时长", record.formatDuration(), Color.rgb(30, 42, 94));
        LinearLayout paceCard = reportCard("平均配速", record.formatPace(), Color.rgb(255, 127, 80));
        LinearLayout caloriesCard = reportCard("消耗卡路里", (int) record.getTotalCalories() + " kcal", Color.rgb(100, 100, 100));

        workoutReportContainer.addView(distanceCard);
        workoutReportContainer.addView(durationCard);
        workoutReportContainer.addView(paceCard);
        workoutReportContainer.addView(caloriesCard);

        LinearLayout reportButtons = new LinearLayout(this);
        reportButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button saveBtn = primaryButton("保存记录");
        saveBtn.setBackgroundColor(Color.rgb(0, 229, 178));
        saveBtn.setTextColor(Color.rgb(30, 42, 94));
        Button discardBtn = dangerButton("丢弃记录");
        Button againBtn = secondaryButton("再跑一次");

        reportButtons.addView(saveBtn, weightParams());
        reportButtons.addView(discardBtn, weightParams());
        content.addView(reportButtons);
        content.addView(againBtn);

        saveBtn.setOnClickListener(v -> runAction(() -> {
            toast("已保存至历史记录");
            showWorkoutPage();
        }));
        discardBtn.setOnClickListener(v -> runAction(() -> {
            workoutService.discardWorkout();
            toast("记录已丢弃");
            showWorkoutPage();
        }));
        againBtn.setOnClickListener(v -> runAction(() -> {
            showWorkoutPage();
        }));
    }

    private void showTrainingPlanPage() {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("个性化运动计划"));

        if (currentUser == null) {
            content.addView(label("请先登录"));
            return;
        }

        List<TrainingPlan> plans = trainingPlanService.getUserPlans(currentUser.getUserId());

        if (plans.isEmpty()) {
            content.addView(label("您还没有训练计划"));

            Button createBtn = primaryButton("生成我的初始运动计划");
            createBtn.setBackgroundColor(Color.rgb(0, 229, 178));
            createBtn.setTextColor(Color.rgb(30, 42, 94));
            content.addView(createBtn);

            createBtn.setOnClickListener(v -> runAction(() -> {
                User user = requireLogin();
                TrainingPlan plan = trainingPlanService.createPlan(user.getUserId(), "BEGINNER_RUN");
                toast("已生成计划: " + plan.getPlanName());
                showTrainingPlanPage();
            }));
            return;
        }

        for (TrainingPlan plan : plans) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackgroundColor(Color.WHITE);
            card.setLayoutParams(blockParams());

            TextView nameView = new TextView(this);
            nameView.setText(plan.getPlanName());
            nameView.setTextColor(Color.rgb(30, 42, 94));
            nameView.setTextSize(16);
            nameView.setPadding(0, 0, 0, dp(4));
            card.addView(nameView);

            TextView descView = new TextView(this);
            descView.setText(plan.getGoalDescription());
            descView.setTextColor(Color.rgb(102, 102, 102));
            descView.setTextSize(13);
            descView.setPadding(0, 0, 0, dp(6));
            card.addView(descView);

            TextView progressView = new TextView(this);
            progressView.setText("进度: " + plan.getCompletedDays() + "/" + plan.getTotalDays()
                    + " 天 (" + String.format("%.0f", plan.getProgressPercent()) + "%)");
            progressView.setTextColor(Color.rgb(0, 229, 178));
            progressView.setTextSize(14);
            card.addView(progressView);

            TextView statusView = new TextView(this);
            statusView.setText("状态: " + plan.getStatus() + "  " + plan.getStartDate() + " ~ " + plan.getEndDate());
            statusView.setTextColor(Color.rgb(102, 102, 102));
            statusView.setTextSize(12);
            card.addView(statusView);

            content.addView(card);

            Button viewBtn = secondaryButton("查看详情");
            viewBtn.setOnClickListener(v -> showTrainingPlanDetail(plan));
            content.addView(viewBtn);
        }
    }

    private void showTrainingPlanDetail(TrainingPlan plan) {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle(plan.getPlanName()));

        TextView goalView = new TextView(this);
        goalView.setText("目标: " + plan.getGoalDescription());
        goalView.setTextColor(Color.rgb(102, 102, 102));
        goalView.setTextSize(13);
        goalView.setPadding(0, 0, 0, dp(6));
        content.addView(goalView);

        TextView progressView = new TextView(this);
        progressView.setText("周期: " + plan.getStartDate() + " ~ " + plan.getEndDate() +
                " | 已完成 " + plan.getCompletedDays() + "/" + plan.getTotalDays() + " 天");
        progressView.setTextColor(Color.rgb(0, 229, 178));
        progressView.setTextSize(13);
        progressView.setPadding(0, 0, 0, dp(12));
        content.addView(progressView);

        if (!plan.getTips().isEmpty()) {
            content.addView(label("温馨提示"));
            for (String tip : plan.getTips()) {
                TextView tipView = new TextView(this);
                tipView.setText("- " + tip);
                tipView.setTextColor(Color.rgb(102, 102, 102));
                tipView.setTextSize(12);
                tipView.setPadding(dp(8), 0, 0, 0);
                content.addView(tipView);
            }
        }

        content.addView(sectionTitle("每日训练计划"));
        for (TrainingDay day : plan.getTrainingDays()) {
            LinearLayout dayCard = new LinearLayout(this);
            dayCard.setOrientation(LinearLayout.HORIZONTAL);
            dayCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            dayCard.setBackgroundColor(day.isCompleted() ? Color.rgb(0, 229, 178) : Color.WHITE);
            dayCard.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

            TextView dayIndexView = new TextView(this);
            dayIndexView.setText("第" + day.getDayIndex() + "天");
            dayIndexView.setTextColor(day.isCompleted() ? Color.WHITE : Color.rgb(30, 42, 94));
            dayIndexView.setTextSize(14);
            dayIndexView.setPadding(0, 0, dp(12), 0);
            dayCard.addView(dayIndexView);

            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);

            TextView typeView = new TextView(this);
            typeView.setText(day.getExerciseType() + " · " + day.getIntensity());
            typeView.setTextColor(day.isCompleted() ? Color.WHITE : Color.rgb(102, 102, 102));
            typeView.setTextSize(13);
            infoCol.addView(typeView);

            if (day.getTargetDurationMinutes() > 0) {
                TextView durView = new TextView(this);
                durView.setText(day.getTargetDurationMinutes() + "分钟 "
                        + (day.getTargetDistanceKm() > 0 ? day.getTargetDistanceKm() + "km" : ""));
                durView.setTextColor(day.isCompleted() ? Color.WHITE : Color.rgb(102, 102, 102));
                durView.setTextSize(12);
                infoCol.addView(durView);
            }

            dayCard.addView(infoCol);
            content.addView(dayCard);
        }

        Button backBtn = secondaryButton("返回计划列表");
        backBtn.setOnClickListener(v -> showTrainingPlanPage());
        content.addView(backBtn);
    }

    private void showDashboardPage() {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("健康数据看板"));

        if (currentUser == null) {
            content.addView(label("请先登录"));
            return;
        }

        SyncedHealthData data = healthSyncService.getLatestSyncedData(currentUser.getUserId());
        if (data == null) {
            content.addView(label("暂无同步数据，请先连接穿戴设备"));
            Button connectBtn = primaryButton("连接健康应用");
            connectBtn.setOnClickListener(v -> runAction(() -> {
                User user = requireLogin();
                healthSyncService.addSyncSource(user.getUserId(), "wearable_watch");
                healthSyncService.syncSingleSource(user.getUserId(), "wearable_watch");
                toast("已连接并同步数据");
                showDashboardPage();
            }));
            content.addView(connectBtn);
            return;
        }

        LinearLayout stepsCard = dashboardCard("步数", data.getStepCount() + "/8000", Color.rgb(0, 229, 178));
        LinearLayout hrCard = dashboardCard("心率", "平均" + data.getHeartRateAvg()
                + " · 最高" + data.getHeartRateMax() + " bpm", Color.rgb(255, 127, 80));
        LinearLayout sleepCard = dashboardCard("睡眠", data.getSleepDurationMinutes() / 60 + "小时"
                + data.getSleepDurationMinutes() % 60 + "分", Color.rgb(30, 42, 94));
        LinearLayout caloriesCard = dashboardCard("消耗", String.format("%.0f", data.getCaloriesBurned()) + " kcal",
                Color.rgb(100, 100, 100));

        content.addView(stepsCard);
        content.addView(hrCard);
        content.addView(sleepCard);
        content.addView(caloriesCard);

        if (data.getSourceType() != null) {
            TextView sourceView = new TextView(this);
            sourceView.setText("数据来源: " + data.getSourceType() + " | 同步时间: " + data.getSyncTime());
            sourceView.setTextColor(Color.rgb(153, 153, 153));
            sourceView.setTextSize(11);
            sourceView.setPadding(0, dp(12), 0, 0);
            content.addView(sourceView);
        }

        Button refreshBtn = secondaryButton("立即同步");
        refreshBtn.setOnClickListener(v -> runAction(() -> {
            User user = requireLogin();
            healthSyncService.syncSingleSource(user.getUserId(), "wearable_watch");
            toast("数据已更新");
            showDashboardPage();
        }));
        content.addView(refreshBtn);
    }

    private LinearLayout dashboardCard(String label, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundColor(Color.WHITE);
        card.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.rgb(102, 102, 102));
        labelView.setTextSize(13);
        card.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(color);
        valueView.setTextSize(24);
        card.addView(valueView);

        return card;
    }

    private void showSyncSharePage() {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("同步与成就分享"));

        if (currentUser == null) {
            content.addView(label("请先登录"));
            return;
        }

        content.addView(label("同步服务"));
        Button syncBtn = primaryButton("连接穿戴设备");
        syncBtn.setBackgroundColor(Color.rgb(0, 229, 178));
        syncBtn.setTextColor(Color.rgb(30, 42, 94));
        syncBtn.setOnClickListener(v -> runAction(() -> {
            User user = requireLogin();
            healthSyncService.addSyncSource(user.getUserId(), "wearable_watch");
            String result = healthSyncService.syncSingleSource(user.getUserId(), "wearable_watch");
            toast("同步" + ("SUCCESS".equals(result) ? "成功" : "失败"));
            showSyncSharePage();
        }));
        content.addView(syncBtn);

        content.addView(sectionTitle("运动成就分享"));

        List<WorkoutRecord> records = workoutService.findUserRecords(currentUser.getUserId());
        if (records.isEmpty()) {
            content.addView(label("暂无运动记录，快去跑步吧"));
            return;
        }

        for (WorkoutRecord rec : records) {
            if (rec.getStatus() != WorkoutStatus.COMPLETED) {
                continue;
            }
            LinearLayout recCard = new LinearLayout(this);
            recCard.setOrientation(LinearLayout.VERTICAL);
            recCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            recCard.setBackgroundColor(Color.WHITE);
            recCard.setLayoutParams(blockParams());

            TextView titleView = new TextView(this);
            titleView.setText("运动记录 #" + rec.getRecordId() + " - " + String.format("%.2f", rec.getTotalDistanceKm()) + "km");
            titleView.setTextColor(Color.rgb(30, 42, 94));
            titleView.setTextSize(14);
            recCard.addView(titleView);

            TextView detailView = new TextView(this);
            detailView.setText("时长: " + rec.formatDuration() + " | 配速: " + rec.formatPace()
                    + " | 消耗: " + (int) rec.getTotalCalories() + "kcal");
            detailView.setTextColor(Color.rgb(102, 102, 102));
            detailView.setTextSize(12);
            recCard.addView(detailView);

            content.addView(recCard);

            Button shareBtn = secondaryButton("分享成就");
            shareBtn.setOnClickListener(v -> shareAchievement(rec));
            content.addView(shareBtn);
        }
    }

    private void shareAchievement(WorkoutRecord record) {
        cleanupMapView();
        content.removeAllViews();
        content.addView(sectionTitle("成就海报预览"));

        LinearLayout posterCard = new LinearLayout(this);
        posterCard.setOrientation(LinearLayout.VERTICAL);
        posterCard.setPadding(dp(20), dp(24), dp(20), dp(24));
        posterCard.setBackgroundColor(Color.rgb(29, 122, 140));
        posterCard.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(260)));

        TextView posterTitle = new TextView(this);
        posterTitle.setText("运动成就");
        posterTitle.setTextColor(Color.WHITE);
        posterTitle.setTextSize(20);
        posterTitle.setGravity(Gravity.CENTER);
        posterCard.addView(posterTitle);

        TextView posterDist = new TextView(this);
        posterDist.setText(String.format("%.2f", record.getTotalDistanceKm()) + " KM");
        posterDist.setTextColor(Color.rgb(0, 229, 178));
        posterDist.setTextSize(40);
        posterDist.setGravity(Gravity.CENTER);
        posterDist.setPadding(0, dp(20), 0, dp(8));
        posterCard.addView(posterDist);

        TextView posterPace = new TextView(this);
        posterPace.setText("平均配速 " + record.formatPace()
                + " | " + (int) record.getTotalCalories() + " kcal");
        posterPace.setTextColor(Color.WHITE);
        posterPace.setTextSize(14);
        posterPace.setGravity(Gravity.CENTER);
        posterCard.addView(posterPace);

        TextView posterName = new TextView(this);
        posterName.setText("-- " + (currentUser != null ? currentUser.getNickname() : "运动者") + " --");
        posterName.setTextColor(Color.rgb(180, 200, 220));
        posterName.setTextSize(13);
        posterName.setGravity(Gravity.CENTER);
        posterName.setPadding(0, dp(28), 0, 0);
        posterCard.addView(posterName);

        content.addView(posterCard);

        Button shareBtn = primaryButton("分享到社交平台");
        shareBtn.setBackgroundColor(Color.rgb(0, 229, 178));
        shareBtn.setTextColor(Color.rgb(30, 42, 94));
        shareBtn.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "我在运动与健康App跑步" + String.format("%.2f", record.getTotalDistanceKm())
                            + "km！配速" + record.formatPace() + "，快来一起运动吧！");
            startActivity(Intent.createChooser(shareIntent, "分享成就"));
        });
        content.addView(shareBtn);

        Button backBtn = secondaryButton("返回");
        backBtn.setOnClickListener(v -> showSyncSharePage());
        content.addView(backBtn);
    }

    private void cleanupMapView() {
        if (mapSlot != null) {
            mapSlot.setVisibility(View.GONE);
        }
        trackLatLngs = new ArrayList<>();
        trackLineDirty = true;
        lastAnimatedPoint = null;
        Log.d(TAG, "cleanupMapView: mapSlot hidden");
    }

    private void refreshWorkoutPage() {
        cleanupMapView();
        showWorkoutPage();
    }

    private void initPersistentMapView() {
        Log.d(TAG, "initPersistentMapView: baiduSdkReady=" + baiduSdkReady);
        if (!baiduSdkReady) {
            Log.w(TAG, "initPersistentMapView: SDK not ready, skip map");
            return;
        }
        try {
            mapView = new MapView(this);
            mapView.onCreate(this, savedState);
            mapSlot.removeAllViews();
            mapSlot.addView(mapView, new LinearLayout.LayoutParams(-1, dp(300)));
            baiduMap = mapView.getMap();
            if (baiduMap == null) {
                Log.e(TAG, "initPersistentMapView: baiduMap is null");
                toast("地图服务暂不可用");
                mapReady = false;
                return;
            }
            baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(
                    new MapStatus.Builder().zoom(17).build()));
            baiduMap.setTrafficEnabled(false);
            mapReady = true;
            Log.i(TAG, "initPersistentMapView: SUCCESS, waiting for onResume");
        } catch (Throwable t) {
            Log.e(TAG, "initPersistentMapView failed: " + t.getClass().getSimpleName(), t);
            toast("地图初始化失败，将以纯数据模式记录");
            mapView = null;
            baiduMap = null;
            mapReady = false;
        }
    }

    private void updateMapTrajectory() {
        if (!mapReady || baiduMap == null) {
            return;
        }

        if (trackLatLngs == null) {
            trackLatLngs = new ArrayList<>();
        }

        if (trackLatLngs.isEmpty()) {
            WorkoutRecord rec = workoutService.getCurrentRecord();
            if (rec != null) {
                List<com.health.sports.model.TrackPoint> points = workoutService.findTrackPoints(rec.getRecordId());
                Log.d(TAG, "updateMapTrajectory: loading " + points.size() + " track points from store");
                for (com.health.sports.model.TrackPoint point : points) {
                    trackLatLngs.add(new LatLng(point.getLatitude(), point.getLongitude()));
                }
                if (!trackLatLngs.isEmpty()) {
                    trackLineDirty = true;
                }
            }
        }

        double lat = workoutService.getLatestLatitude();
        double lng = workoutService.getLatestLongitude();
        LatLng newPoint = new LatLng(lat, lng);

        boolean isNewTrackPoint = trackLatLngs.isEmpty();
        if (!isNewTrackPoint) {
            LatLng lastPoint = trackLatLngs.get(trackLatLngs.size() - 1);
            double dLat = Math.abs(lastPoint.latitude - lat);
            double dLng = Math.abs(lastPoint.longitude - lng);
            isNewTrackPoint = (dLat > 0.000005 || dLng > 0.000005);
        }
        if (isNewTrackPoint) {
            trackLatLngs.add(newPoint);
            trackLineDirty = true;
        }

        if (trackLineDirty) {
            baiduMap.clear();
            trackLineDirty = false;

            if (trackLatLngs.size() >= 1) {
                BitmapDescriptor startIcon = BitmapDescriptorFactory.fromResource(
                        android.R.drawable.ic_media_play);
                MarkerOptions startMarker = new MarkerOptions()
                        .position(trackLatLngs.get(0))
                        .icon(startIcon)
                        .title("起点");
                baiduMap.addOverlay(startMarker);
            }

            if (trackLatLngs.size() >= 2) {
                List<LatLng> smoothedPoints = generateBezierCurve(trackLatLngs);
                PolylineOptions polylineOptions = new PolylineOptions()
                        .width(8)
                        .color(Color.rgb(0, 229, 178))
                        .points(smoothedPoints);
                baiduMap.addOverlay(polylineOptions);
            }
        }

        BitmapDescriptor currentIcon = BitmapDescriptorFactory.fromResource(
                android.R.drawable.presence_online);
        MarkerOptions currentMarker = new MarkerOptions()
                .position(newPoint)
                .icon(currentIcon)
                .title("当前位置");
        baiduMap.addOverlay(currentMarker);

        boolean shouldAnimate = lastAnimatedPoint == null;
        if (!shouldAnimate) {
            double dLat = Math.abs(lastAnimatedPoint.latitude - lat);
            double dLng = Math.abs(lastAnimatedPoint.longitude - lng);
            shouldAnimate = (dLat > 0.0001 || dLng > 0.0001);
        }
        if (shouldAnimate) {
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(newPoint));
            lastAnimatedPoint = newPoint;
        }
    }

    private List<LatLng> generateBezierCurve(List<LatLng> points) {
        if (points.size() <= 2) {
            return points;
        }
        List<LatLng> result = new ArrayList<>();
        result.add(points.get(0));
        for (int i = 0; i < points.size() - 1; i++) {
            LatLng p0 = points.get(i);
            LatLng p1 = points.get(Math.min(i + 1, points.size() - 1));
            double ctrlLat = (p0.latitude + p1.latitude) / 2.0;
            double ctrlLon = (p0.longitude + p1.longitude) / 2.0;
            int segments = Math.max(2, (int) (DistanceCalculator.haversineDistance(
                    p0.latitude, p0.longitude, p1.latitude, p1.longitude) / 3.0));
            for (int t = 1; t <= segments; t++) {
                double ratio = (double) t / segments;
                double lat = (1 - ratio) * (1 - ratio) * p0.latitude
                           + 2 * (1 - ratio) * ratio * ctrlLat
                           + ratio * ratio * p1.latitude;
                double lon = (1 - ratio) * (1 - ratio) * p0.longitude
                           + 2 * (1 - ratio) * ratio * ctrlLon
                           + ratio * ratio * p1.longitude;
                result.add(new LatLng(lat, lon));
            }
        }
        return result;
    }

    private void updateWorkoutLiveData(WorkoutRecord record, double currentPace) {
        if (workoutDistanceView != null) {
            workoutDistanceView.setText("总距离\n" + String.format("%.2f", record.getTotalDistanceKm()) + " km");
        }
        if (workoutDurationView != null) {
            workoutDurationView.setText("时长\n" + record.formatDuration());
        }
        if (workoutPaceView != null) {
            String paceStr = currentPace > 0 ? formatPace(currentPace) : "--'--\"";
            workoutPaceView.setText("平均配速\n" + paceStr);
        }
        if (workoutCaloriesView != null) {
            workoutCaloriesView.setText("消耗\n" + (int) record.getTotalCalories() + " kcal");
        }
        if (workoutStatusView != null) {
            double km = record.getTotalDistanceKm();
            String hint;
            if (km < 1) {
                hint = "热身阶段，加油！";
            } else if (km < 3) {
                hint = "保持节奏，已跑 " + String.format("%.1f", km) + " km";
            } else if (km < 5) {
                hint = "突破 3 公里！继续坚持";
            } else {
                hint = "太棒了！已跑 " + String.format("%.1f", km) + " km";
            }
            workoutStatusView.setText(hint);
        }
        if (workoutGpsView != null) {
            workoutGpsView.setText(workoutService.getGpsStatusText());
        }
    }

    @Override
    public void onTick(WorkoutRecord record, double currentPace) {
        runOnUiThread(() -> {
            updateWorkoutLiveData(record, currentPace);
            updateMapTrajectory();
        });
    }

    @Override
    public void onStateChanged(WorkoutStatus status) {
        Log.d(TAG, "onStateChanged: " + status);
        if (status == WorkoutStatus.COMPLETED) {
            return;
        }
        runOnUiThread(() -> {
            refreshWorkoutPage();
        });
    }

    @Override
    public void onGpsFixAcquired(double lat, double lng, float accuracy) {
        runOnUiThread(() -> {
            toast("GPS 已定位！精度 " + String.format("%.0f", accuracy) + "m");
            if (mapReady) {
                updateMapTrajectory();
            }
            if (workoutGpsView != null) {
                workoutGpsView.setText(workoutService.getGpsStatusText());
            }
        });
    }

    private String formatPace(double secondsPerKm) {
        if (secondsPerKm <= 0) {
            return "--'--\"";
        }
        int minutes = (int) (secondsPerKm / 60);
        int seconds = (int) (secondsPerKm % 60);
        return minutes + "'" + String.format("%02d", seconds) + "\"";
    }

    private TextView dataMetric(String label, String value, int color) {
        TextView view = new TextView(this);
        view.setText(label + "\n" + value);
        view.setTextColor(color);
        view.setTextSize(16);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setBackgroundColor(Color.WHITE);
        view.setLayoutParams(blockParams());
        return view;
    }

    private LinearLayout reportCard(String label, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.rgb(102, 102, 102));
        labelView.setTextSize(13);
        labelView.setPadding(dp(12), dp(12), dp(12), dp(4));
        labelView.setBackgroundColor(Color.WHITE);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(color);
        valueView.setTextSize(28);
        valueView.setPadding(dp(12), 0, dp(12), dp(14));
        valueView.setBackgroundColor(Color.WHITE);

        card.addView(labelView);
        card.addView(valueView);
        card.setLayoutParams(reportCardParams());
        return card;
    }

    private LinearLayout.LayoutParams reportCardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(6), 0, dp(8));
        return params;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.rgb(29, 122, 140));
        button.setAllCaps(false);
        button.setBackgroundColor(Color.rgb(220, 237, 240));
        button.setLayoutParams(blockParams());
        return button;
    }

    private Button dangerButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundColor(Color.rgb(231, 76, 60));
        button.setLayoutParams(blockParams());
        return button;
    }

    private void runAction(Action action) {
        try {
            action.run();
        } catch (ApiException e) {
            toast(e.getMessage());
        } catch (Exception e) {
            toast("输入有误：" + e.getMessage());
        }
    }

    private void setCurrentUser(User user) {
        currentUser = user;
        currentUserView.setText("当前用户：" + user.getNickname() + "  ID：" + user.getUserId());
    }

    private User requireLogin() {
        if (currentUser == null) {
            throw new ApiException(401, "请先完成注册或登录");
        }
        return currentUser;
    }

    private void fillUserFields(User user) {
        nicknameInput.setText(user.getNickname());
        avatarInput.setText(user.getAvatarUrl());
    }

    private void showProfile(HealthProfile profile) {
        profileResult.setText("用户ID：" + profile.getUserId()
                + "\n性别：" + profile.getGender()
                + "\n出生日期：" + profile.getBirthDate()
                + "\n身高：" + profile.getHeightCm() + " cm"
                + "\n体重：" + profile.getWeightKg() + " kg"
                + "\n活动水平：" + profile.getActivityLevel()
                + "\nBMI：" + profile.calcBMI()
                + "\n状态：" + profile.bmiLevel());
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(31, 56, 73));
        view.setTextSize(18);
        view.setPadding(0, dp(16), 0, dp(10));
        return view;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(73, 88, 101));
        view.setPadding(0, dp(8), 0, dp(4));
        return view;
    }

    private EditText input(String hint, boolean password) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setTextSize(15);
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackgroundColor(Color.WHITE);
        if (password) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        editText.setLayoutParams(blockParams());
        return editText;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setLayoutParams(blockParams());
        return spinner;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundColor(Color.rgb(29, 122, 140));
        button.setLayoutParams(blockParams());
        return button;
    }

    private Button navButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams blockParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, dp(6), 0, dp(8));
        return params;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private interface Action {
        void run();
    }
}

