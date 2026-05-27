package com.health.modulea.ui;

import com.health.modulea.model.ActivityLevel;
import com.health.modulea.model.ApiException;
import com.health.modulea.model.Gender;
import com.health.modulea.model.HealthProfile;
import com.health.modulea.model.User;
import com.health.modulea.service.AccountService;
import com.health.modulea.service.HealthProfileService;
import com.health.modulea.service.PasswordHasher;
import com.health.modulea.store.InMemoryStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;

public class HealthModuleAFrame extends JFrame {
    private final AccountService accountService;
    private final HealthProfileService profileService;

    private User currentUser;

    private final JTextField registerMobile = new JTextField();
    private final JTextField registerCode = new JTextField("123456");
    private final JPasswordField registerPassword = new JPasswordField();
    private final JTextField registerNickname = new JTextField();
    private final JTextField loginMobile = new JTextField();
    private final JPasswordField loginPassword = new JPasswordField();
    private final JLabel currentUserLabel = new JLabel("未登录", SwingConstants.LEFT);

    private final JTextField editNickname = new JTextField();
    private final JTextField avatarUrl = new JTextField();
    private final JComboBox<Gender> gender = new JComboBox<Gender>(Gender.values());
    private final JTextField birthDate = new JTextField("2003-01-01");
    private final JTextField heightCm = new JTextField("170");
    private final JTextField weightKg = new JTextField("60");
    private final JComboBox<ActivityLevel> activityLevel = new JComboBox<ActivityLevel>(ActivityLevel.values());
    private final JTextArea profileResult = new JTextArea();

    public HealthModuleAFrame() {
        InMemoryStore store = new InMemoryStore();
        this.accountService = new AccountService(store, new PasswordHasher());
        this.profileService = new HealthProfileService(store, accountService);

        setTitle("运动与健康系统 - 模块A");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createTabs(), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(18, 22, 14, 22));
        header.setBackground(new Color(36, 71, 96));

        JLabel title = new JLabel("运动与健康系统 - 用户账户与健康档案");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));

        currentUserLabel.setForeground(new Color(220, 232, 241));
        currentUserLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));

        header.add(title, BorderLayout.WEST);
        header.add(currentUserLabel, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        tabs.addTab("注册登录", createAccountPanel());
        tabs.addTab("个人资料", createUserProfilePanel());
        tabs.addTab("健康档案", createHealthProfilePanel());
        return tabs;
    }

    private JPanel createAccountPanel() {
        JPanel panel = createContentPanel();
        panel.add(sectionTitle("账号注册"));
        panel.add(formRow("手机号", registerMobile));
        panel.add(formRow("验证码", registerCode));
        panel.add(formRow("密码", registerPassword));
        panel.add(formRow("昵称", registerNickname));

        JPanel registerActions = new JPanel();
        registerActions.setOpaque(false);
        JButton sendCodeButton = new JButton("发送验证码");
        JButton registerButton = new JButton("注册");
        registerActions.add(sendCodeButton);
        registerActions.add(registerButton);
        panel.add(registerActions);

        panel.add(Box.createVerticalStrut(18));
        panel.add(sectionTitle("账号登录"));
        panel.add(formRow("手机号", loginMobile));
        panel.add(formRow("密码", loginPassword));

        JButton loginButton = new JButton("登录");
        JPanel loginActions = new JPanel();
        loginActions.setOpaque(false);
        loginActions.add(loginButton);
        panel.add(loginActions);

        sendCodeButton.addActionListener(e -> sendCode());
        registerButton.addActionListener(e -> register());
        loginButton.addActionListener(e -> login());
        return panel;
    }

    private JPanel createUserProfilePanel() {
        JPanel panel = createContentPanel();
        panel.add(sectionTitle("个人资料维护"));
        panel.add(formRow("昵称", editNickname));
        panel.add(formRow("头像地址", avatarUrl));

        JButton saveButton = new JButton("保存资料");
        JButton loadButton = new JButton("加载当前用户");
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(loadButton);
        actions.add(saveButton);
        panel.add(actions);

        loadButton.addActionListener(e -> loadCurrentUser());
        saveButton.addActionListener(e -> saveUserProfile());
        return panel;
    }

    private JPanel createHealthProfilePanel() {
        JPanel panel = createContentPanel();
        panel.add(sectionTitle("健康档案"));
        panel.add(formRow("性别", gender));
        panel.add(formRow("出生日期", birthDate));
        panel.add(formRow("身高(cm)", heightCm));
        panel.add(formRow("体重(kg)", weightKg));
        panel.add(formRow("活动水平", activityLevel));

        JButton saveButton = new JButton("保存健康档案");
        JButton loadButton = new JButton("查询健康档案");
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(saveButton);
        actions.add(loadButton);
        panel.add(actions);

        profileResult.setEditable(false);
        profileResult.setRows(7);
        profileResult.setFont(new Font("Consolas", Font.PLAIN, 14));
        profileResult.setBorder(BorderFactory.createLineBorder(new Color(204, 214, 224)));
        panel.add(profileResult);

        saveButton.addActionListener(e -> saveHealthProfile());
        loadButton.addActionListener(e -> loadHealthProfile());
        return panel;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 247, 250));
        panel.setBorder(new EmptyBorder(22, 26, 22, 26));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.BOLD, 17));
        label.setForeground(new Color(33, 53, 71));
        label.setBorder(new EmptyBorder(8, 0, 8, 0));
        return label;
    }

    private JPanel formRow(String labelText, java.awt.Component input) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(92, 28));
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.insets = new Insets(4, 0, 4, 10);
        labelConstraints.anchor = GridBagConstraints.WEST;
        row.add(label, labelConstraints);

        GridBagConstraints inputConstraints = new GridBagConstraints();
        inputConstraints.gridx = 1;
        inputConstraints.gridy = 0;
        inputConstraints.weightx = 1;
        inputConstraints.fill = GridBagConstraints.HORIZONTAL;
        inputConstraints.insets = new Insets(4, 0, 4, 0);
        row.add(input, inputConstraints);
        return row;
    }

    private void sendCode() {
        runAction(new Runnable() {
            public void run() {
                String code = accountService.sendVerificationCode(registerMobile.getText().trim());
                registerCode.setText(code);
                info("验证码已生成: " + code);
            }
        });
    }

    private void register() {
        runAction(new Runnable() {
            public void run() {
                User user = accountService.register(registerMobile.getText().trim(), registerCode.getText().trim(),
                        new String(registerPassword.getPassword()), registerNickname.getText().trim());
                setCurrentUser(user);
                loginMobile.setText(user.getMobile());
                info("注册成功，当前用户ID: " + user.getUserId());
            }
        });
    }

    private void login() {
        runAction(new Runnable() {
            public void run() {
                User user = accountService.login(loginMobile.getText().trim(), new String(loginPassword.getPassword()));
                setCurrentUser(user);
                info("登录成功");
            }
        });
    }

    private void loadCurrentUser() {
        runAction(new Runnable() {
            public void run() {
                User user = requireLogin();
                editNickname.setText(user.getNickname());
                avatarUrl.setText(user.getAvatarUrl());
            }
        });
    }

    private void saveUserProfile() {
        runAction(new Runnable() {
            public void run() {
                User user = requireLogin();
                User updated = accountService.updateUserProfile(user.getUserId(), editNickname.getText(), avatarUrl.getText());
                setCurrentUser(updated);
                info("个人资料已保存");
            }
        });
    }

    private void saveHealthProfile() {
        runAction(new Runnable() {
            public void run() {
                User user = requireLogin();
                HealthProfile profile = profileService.saveProfile(
                        user.getUserId(),
                        (Gender) gender.getSelectedItem(),
                        LocalDate.parse(birthDate.getText().trim()),
                        Integer.parseInt(heightCm.getText().trim()),
                        Double.parseDouble(weightKg.getText().trim()),
                        (ActivityLevel) activityLevel.getSelectedItem());
                showProfile(profile);
                info("健康档案已保存");
            }
        });
    }

    private void loadHealthProfile() {
        runAction(new Runnable() {
            public void run() {
                User user = requireLogin();
                showProfile(profileService.getProfile(user.getUserId()));
            }
        });
    }

    private User requireLogin() {
        if (currentUser == null) {
            throw new ApiException(401, "请先登录或注册");
        }
        return currentUser;
    }

    private void setCurrentUser(User user) {
        this.currentUser = user;
        currentUserLabel.setText("当前用户: " + user.getNickname() + " / ID " + user.getUserId());
        editNickname.setText(user.getNickname());
        avatarUrl.setText(user.getAvatarUrl());
    }

    private void showProfile(HealthProfile profile) {
        profileResult.setText("用户ID: " + profile.getUserId()
                + "\n性别: " + profile.getGender()
                + "\n出生日期: " + profile.getBirthDate()
                + "\n身高: " + profile.getHeightCm() + " cm"
                + "\n体重: " + profile.getWeightKg() + " kg"
                + "\n活动水平: " + profile.getActivityLevel()
                + "\nBMI: " + profile.calcBMI() + " (" + profile.bmiLevel() + ")");
    }

    private void runAction(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            error(e.getMessage());
        } catch (Exception e) {
            error("输入或操作失败: " + e.getMessage());
        }
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
