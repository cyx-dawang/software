package com.health.sports;

import android.app.Activity;
import android.Manifest;
import android.content.pm.ApplicationInfo;
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
        workoutService = new WorkoutService(store, accountService, profileService,
                getApplicationContext(), baiduSdkReady);
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
            String ak = getBaiduMapAk();
            if (ak == null || ak.isEmpty() || ak.startsWith("${")) {
                baiduSdkReady = false;
                Log.w(TAG, "Baidu AK is not configured; map is disabled and workout uses demo location");
                return;
            }

            SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
            SDKInitializer.initialize(getApplicationContext());
            LocationClient.setAgreePrivacy(true);
            baiduSdkReady = true;

            Log.i(TAG, "Baidu SDK initialized | AK length=" + (ak != null ? ak.length() : 0)
                    + " | pkg=" + getPackageName());
        } catch (Throwable t) {
            Log.e(TAG, "Baidu SDK init failed: " + t.getClass().getSimpleName(), t);
            baiduSdkReady = false;
        }
    }

    private String getBaiduMapAk() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData == null) {
                return "";
            }
            String ak = ai.metaData.getString("com.baidu.lbsapi.API_KEY");
            if (ak == null || ak.isEmpty()) {
                ak = ai.metaData.getString("com.baidu.android.lbs.API_KEY");
            }
            return ak == null ? "" : ak.trim();
        } catch (Exception e) {
            Log.w(TAG, "Unable to read Baidu AK from manifest: " + e.getMessage());
            return "";
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

        account.setOnClickListener(v -> {
            if (currentUser == null) {
                showLoginPage();
            } else {
                showUserPage();
            }
        });
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
        View registerPage = getLayoutInflater().inflate(R.layout.page_register, content, false);
        content.addView(registerPage);

        registerMobile = registerPage.findViewById(R.id.registerMobile);
        registerCode = registerPage.findViewById(R.id.registerCode);
        registerPassword = registerPage.findViewById(R.id.registerPassword);
        registerNickname = registerPage.findViewById(R.id.registerNickname);
        registerCode.setText("123456");

        Button codeButton = registerPage.findViewById(R.id.sendCodeBtn);
        Button registerButton = registerPage.findViewById(R.id.registerBtn);
        Button switchToLogin = registerPage.findViewById(R.id.switchToLogin);

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
            showUserPage();
        }));
        switchToLogin.setOnClickListener(v -> showLoginPage());
    }

    private void showLoginPage() {
        cleanupMapView();
        content.removeAllViews();
        if (currentUser != null) {
            showUserPage();
            return;
        }
        View loginPage = getLayoutInflater().inflate(R.layout.page_login, content, false);
        content.addView(loginPage);

        loginMobile = loginPage.findViewById(R.id.loginMobile);
        loginPassword = loginPage.findViewById(R.id.loginPassword);

        Button loginButton = loginPage.findViewById(R.id.loginBtn);
        Button switchToRegister = loginPage.findViewById(R.id.switchToRegister);

        loginButton.setOnClickListener(v -> runAction(() -> {
            User user = accountService.login(text(loginMobile), text(loginPassword));
            setCurrentUser(user);
            toast("登录成功，欢迎回来 " + user.getNickname());
            showUserPage();
        }));
        switchToRegister.setOnClickListener(v -> showRegisterPage());
    }

    private void showUserPage() {
        cleanupMapView();
        content.removeAllViews();
        if (currentUser == null) {
            showLoginPage();
            return;
        }
        View userPage = getLayoutInflater().inflate(R.layout.page_user, content, false);
        content.addView(userPage);

        nicknameInput = userPage.findViewById(R.id.nicknameInput);
        avatarInput = userPage.findViewById(R.id.avatarInput);
        fillUserFields(currentUser);

        Button loadButton = userPage.findViewById(R.id.loadUserBtn);
        Button saveButton = userPage.findViewById(R.id.saveUserBtn);

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
        View profilePage = getLayoutInflater().inflate(R.layout.page_health_profile, content, false);
        content.addView(profilePage);

        genderInput = profilePage.findViewById(R.id.genderInput);
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"MALE", "FEMALE", "UNKNOWN"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderInput.setAdapter(genderAdapter);

        birthDateInput = profilePage.findViewById(R.id.birthDateInput);
        heightInput = profilePage.findViewById(R.id.heightInput);
        weightInput = profilePage.findViewById(R.id.weightInput);
        activityInput = profilePage.findViewById(R.id.activityInput);

        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"LOW", "MODERATE", "HIGH"});
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityInput.setAdapter(activityAdapter);

        birthDateInput.setText("2003-01-01");
        heightInput.setText("170");
        weightInput.setText("60");

        profileResult = profilePage.findViewById(R.id.profileResult);

        Button saveButton = profilePage.findViewById(R.id.saveProfileBtn);
        Button loadButton = profilePage.findViewById(R.id.loadProfileBtn);

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
                if (!workoutService.isGpsProviderEnabled()) {
                    toast("请先开启手机GPS定位服务");
                    return;
                }
                workoutService.startWorkout(user.getUserId());
            }));
            return;
        }

        LinearLayout workoutPage = new LinearLayout(this);
        workoutPage.setOrientation(LinearLayout.VERTICAL);
        workoutPage.setPadding(0, 0, 0, dp(18));
        content.addView(workoutPage, new LinearLayout.LayoutParams(-1, -2));

        TextView workoutTitle = sectionTitle(status == WorkoutStatus.PAUSED ? "运动已暂停" : "运动进行中");
        workoutTitle.setPadding(0, dp(4), 0, dp(4));
        workoutPage.addView(workoutTitle);

        workoutGpsView = new TextView(this);
        workoutGpsView.setText(workoutService.getGpsStatusText());
        workoutGpsView.setTextColor(Color.rgb(102, 102, 102));
        workoutGpsView.setTextSize(12);
        workoutGpsView.setPadding(0, 0, 0, dp(8));
        workoutPage.addView(workoutGpsView);

        if (mapSlot != null && mapView != null) {
            mapSlot.setVisibility(View.VISIBLE);
        }

        workoutDataPanel = new LinearLayout(this);
        workoutDataPanel.setOrientation(LinearLayout.VERTICAL);
        workoutPage.addView(workoutDataPanel);

        LinearLayout metricRow1 = new LinearLayout(this);
        metricRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout metricRow2 = new LinearLayout(this);
        metricRow2.setOrientation(LinearLayout.HORIZONTAL);
        workoutDataPanel.addView(metricRow1);
        workoutDataPanel.addView(metricRow2);

        workoutDistanceView = dataMetric("总距离", "-- km", Color.rgb(0, 229, 178));
        workoutDurationView = dataMetric("时长", "--:--", Color.rgb(30, 42, 94));
        workoutPaceView = dataMetric("平均配速", "--'--\"", Color.rgb(255, 127, 80));
        workoutCaloriesView = dataMetric("消耗", "-- kcal", Color.rgb(100, 100, 100));
        workoutStatusView = new TextView(this);
        workoutStatusView.setTextSize(13);
        workoutStatusView.setPadding(0, dp(8), 0, dp(4));

        metricRow1.addView(workoutDistanceView, metricParams(true));
        metricRow1.addView(workoutDurationView, metricParams(false));
        metricRow2.addView(workoutPaceView, metricParams(true));
        metricRow2.addView(workoutCaloriesView, metricParams(false));
        workoutDataPanel.addView(workoutStatusView);

        workoutControls = new LinearLayout(this);
        workoutControls.setOrientation(LinearLayout.HORIZONTAL);
        workoutControls.setPadding(0, dp(4), 0, dp(12));
        workoutPage.addView(workoutControls);

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
        View reportPage = getLayoutInflater().inflate(R.layout.page_report, content, false);
        content.addView(reportPage);

        double distanceKm = record.getTotalDistanceKm();

        TextView dateView = reportPage.findViewById(R.id.reportDate);
        dateView.setText("运动记录 #" + record.getRecordId());

        TextView distanceView = reportPage.findViewById(R.id.reportDistance);
        distanceView.setText(String.format("%.2f km", distanceKm));

        TextView durationView = reportPage.findViewById(R.id.reportDuration);
        durationView.setText(record.formatDuration());

        TextView paceView = reportPage.findViewById(R.id.reportPace);
        paceView.setText(record.formatPace());

        TextView caloriesView = reportPage.findViewById(R.id.reportCalories);
        caloriesView.setText((int) record.getTotalCalories() + " kcal");

        TextView stepsView = reportPage.findViewById(R.id.reportSteps);
        stepsView.setText("\uD83D\uDC63 步数: " + workoutService.getStepCount() + " 步");

        TextView speedView = reportPage.findViewById(R.id.reportSpeed);
        double speedKmh = distanceKm > 0 ? distanceKm / (record.getTotalDurationSeconds() / 3600.0) : 0;
        speedView.setText("\uD83D\uDCC8 平均速度: " + String.format("%.1f km/h", speedKmh));

        Button saveBtn = reportPage.findViewById(R.id.reportSaveBtn);
        Button discardBtn = reportPage.findViewById(R.id.reportDiscardBtn);
        Button againBtn = reportPage.findViewById(R.id.reportAgainBtn);

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
        View dashboardPage = getLayoutInflater().inflate(R.layout.page_dashboard, content, false);
        content.addView(dashboardPage);

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

        TextView stepsView = dashboardPage.findViewById(R.id.stepsValue);
        stepsView.setText(data.getStepCount() + "");

        TextView caloriesDashView = dashboardPage.findViewById(R.id.caloriesValue);
        caloriesDashView.setText(String.format("%.0f", data.getCaloriesBurned()));

        TextView hrView = dashboardPage.findViewById(R.id.heartRateValue);
        hrView.setText("均" + data.getHeartRateAvg() + " 高" + data.getHeartRateMax());

        TextView sleepView = dashboardPage.findViewById(R.id.sleepValue);
        sleepView.setText(String.format("%.1f", data.getSleepDurationMinutes() / 60.0));

        if (data.getSourceType() != null) {
            TextView sourceView = dashboardPage.findViewById(R.id.sourceView);
            sourceView.setText("数据来源: " + data.getSourceType() + " | 同步: " + data.getSyncTime());
        }

        Button connectBtn = dashboardPage.findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(v -> runAction(() -> {
            User user = requireLogin();
            healthSyncService.addSyncSource(user.getUserId(), "wearable_watch");
            healthSyncService.syncSingleSource(user.getUserId(), "wearable_watch");
            toast("已连接并同步数据");
            showDashboardPage();
        }));

        Button refreshBtn = dashboardPage.findViewById(R.id.refreshDashboardBtn);
        refreshBtn.setOnClickListener(v -> runAction(() -> {
            User user = requireLogin();
            healthSyncService.syncSingleSource(user.getUserId(), "wearable_watch");
            toast("数据已更新");
            showDashboardPage();
        }));
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
        View syncPage = getLayoutInflater().inflate(R.layout.page_sync_share, content, false);
        content.addView(syncPage);

        if (currentUser == null) {
            content.addView(label("请先登录"));
            return;
        }

        Button syncBtn = syncPage.findViewById(R.id.connectWearBtn);
        syncBtn.setOnClickListener(v -> runAction(() -> {
            User user = requireLogin();
            healthSyncService.addSyncSource(user.getUserId(), "wearable_watch");
            String result = healthSyncService.syncSingleSource(user.getUserId(), "wearable_watch");
            toast("同步" + ("SUCCESS".equals(result) ? "成功" : "失败"));
            showSyncSharePage();
        }));

        LinearLayout recordsList = syncPage.findViewById(R.id.recordsList);
        TextView noRecordsHint = syncPage.findViewById(R.id.noRecordsHint);

        List<WorkoutRecord> records = workoutService.findUserRecords(currentUser.getUserId());
        boolean hasCompleted = false;
        for (WorkoutRecord rec : records) {
            if (rec.getStatus() != WorkoutStatus.COMPLETED) {
                continue;
            }
            hasCompleted = true;
            LinearLayout recCard = new LinearLayout(this);
            recCard.setOrientation(LinearLayout.VERTICAL);
            recCard.setBackground(getResources().getDrawable(R.drawable.card_bg));
            recCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            recCard.setLayoutParams(blockParams());

            TextView titleView = new TextView(this);
            titleView.setText("\uD83C\uDFC3 运动记录 #" + rec.getRecordId() + " - " + String.format("%.2f", rec.getTotalDistanceKm()) + "km");
            titleView.setTextColor(getResources().getColor(R.color.primaryDark));
            titleView.setTextSize(14);
            recCard.addView(titleView);

            TextView detailView = new TextView(this);
            detailView.setText("时长: " + rec.formatDuration() + " | 配速: " + rec.formatPace()
                    + " | 消耗: " + (int) rec.getTotalCalories() + "kcal");
            detailView.setTextColor(getResources().getColor(R.color.textSecondary));
            detailView.setTextSize(12);
            recCard.addView(detailView);

            Button shareBtn = new Button(this);
            shareBtn.setText("\uD83D\uDCF2 分享成就");
            shareBtn.setTextColor(getResources().getColor(R.color.primaryDark));
            shareBtn.setBackgroundTintList(getResources().getColorStateList(R.color.accent));
            shareBtn.setTextSize(13);
            shareBtn.setOnClickListener(v -> shareAchievement(rec));
            recCard.addView(shareBtn);

            recordsList.addView(recCard);
        }
        if (!hasCompleted) {
            noRecordsHint.setVisibility(View.VISIBLE);
        } else {
            noRecordsHint.setVisibility(View.GONE);
        }
    }

    private void shareAchievement(WorkoutRecord record) {
        cleanupMapView();
        content.removeAllViews();
        View posterPage = getLayoutInflater().inflate(R.layout.page_poster, content, false);
        content.addView(posterPage);

        TextView posterDist = posterPage.findViewById(R.id.posterDistance);
        posterDist.setText(String.format("%.2f KM", record.getTotalDistanceKm()));

        TextView posterPace = posterPage.findViewById(R.id.posterPace);
        posterPace.setText("平均配速 " + record.formatPace()
                + " | " + (int) record.getTotalCalories() + " kcal");

        TextView posterName = posterPage.findViewById(R.id.posterName);
        posterName.setText("-- " + (currentUser != null ? currentUser.getNickname() : "运动者") + " --");

        Button shareBtn = posterPage.findViewById(R.id.posterShareBtn);
        shareBtn.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "\uD83C\uDFC6 我在运动与健康App跑步" + String.format("%.2f", record.getTotalDistanceKm())
                            + "km！配速" + record.formatPace() + "，快来一起运动吧！");
            startActivity(Intent.createChooser(shareIntent, "分享成就"));
        });

        Button backBtn = posterPage.findViewById(R.id.posterBackBtn);
        backBtn.setOnClickListener(v -> showSyncSharePage());
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
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setBackgroundColor(Color.WHITE);
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
        button.setTextColor(getResources().getColor(R.color.white));
        button.setBackgroundColor(getResources().getColor(R.color.deep_sea_blue));
        button.setTextSize(12);
        return button;
    }

    private LinearLayout.LayoutParams blockParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, dp(6), 0, dp(8));
        return params;
    }

    private LinearLayout.LayoutParams metricParams(boolean left) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(72), 1);
        int right = left ? dp(6) : 0;
        int leftMargin = left ? 0 : dp(6);
        params.setMargins(leftMargin, dp(5), right, dp(7));
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

