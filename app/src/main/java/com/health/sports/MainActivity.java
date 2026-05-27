package com.health.sports;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
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

import com.health.sports.model.ActivityLevel;
import com.health.sports.model.ApiException;
import com.health.sports.model.Gender;
import com.health.sports.model.HealthProfile;
import com.health.sports.model.User;
import com.health.sports.feature.account.AccountService;
import com.health.sports.feature.profile.HealthProfileService;
import com.health.sports.feature.account.PasswordHasher;
import com.health.sports.store.InMemoryStore;

public class MainActivity extends Activity {
    private AccountService accountService;
    private HealthProfileService profileService;
    private User currentUser;

    private LinearLayout content;
    private TextView currentUserView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InMemoryStore store = new InMemoryStore();
        accountService = new AccountService(store, new PasswordHasher());
        profileService = new HealthProfileService(store, accountService);
        setContentView(createRootView());
        showAccountPage();
    }

    private View createRootView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 248, 250));

        TextView title = new TextView(this);
        title.setText("运动与健康系统");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(18), dp(14), dp(18), dp(6));
        title.setBackgroundColor(Color.rgb(32, 83, 96));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(58)));

        currentUserView = new TextView(this);
        currentUserView.setText("Android客户端：模块A已完成，B/C可继续接入");
        currentUserView.setTextColor(Color.WHITE);
        currentUserView.setPadding(dp(18), 0, dp(18), dp(12));
        currentUserView.setBackgroundColor(Color.rgb(32, 83, 96));
        root.addView(currentUserView, new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(10), dp(8), dp(10), dp(8));
        nav.setBackgroundColor(Color.WHITE);
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(58)));

        Button account = navButton("账户");
        Button user = navButton("资料");
        Button profile = navButton("档案");
        nav.addView(account, weightParams());
        nav.addView(user, weightParams());
        nav.addView(profile, weightParams());

        account.setOnClickListener(v -> showAccountPage());
        user.setOnClickListener(v -> showUserPage());
        profile.setOnClickListener(v -> showProfilePage());

        ScrollView scrollView = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), dp(28));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private void showAccountPage() {
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

        content.addView(sectionTitle("账号密码登录"));
        loginMobile = input("手机号", false);
        loginPassword = input("密码", true);
        content.addView(loginMobile);
        content.addView(loginPassword);
        Button loginButton = primaryButton("登录");
        content.addView(loginButton);

        codeButton.setOnClickListener(v -> runAction(() -> {
            String code = accountService.sendVerificationCode(registerMobile.getText().toString().trim());
            registerCode.setText(code);
            toast("验证码已生成：" + code);
        }));
        registerButton.setOnClickListener(v -> runAction(() -> {
            User user = accountService.register(text(registerMobile), text(registerCode),
                    text(registerPassword), text(registerNickname));
            setCurrentUser(user);
            toast("注册成功");
        }));
        loginButton.setOnClickListener(v -> runAction(() -> {
            User user = accountService.login(text(loginMobile), text(loginPassword));
            setCurrentUser(user);
            toast("登录成功");
        }));
    }

    private void showUserPage() {
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

    private void runAction(Action action) {
        try {
            action.run();
        } catch (ApiException e) {
            toast(e.getMessage());
        } catch (Exception e) {
            toast("输入有误：" + e.getMessage());
        }
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

