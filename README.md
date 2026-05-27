# 运动与健康系统 Android客户端

本仓库是实验六编码与测试阶段的小组 Android 客户端工程。前序实验文档明确系统基线为“Android 客户端 + Spring Boot 后端”，因此本仓库以 Android 原生 Java 作为客户端开局。

当前已完成模块 A：用户账户与健康档案。模块 B/C 的协作入口见 [MODULES.md](MODULES.md)。

## 已完成内容

- Android 工程骨架，可直接由 Android Studio 打开。
- 模块 A 注册登录页面。
- 手机号验证码模拟注册。
- 账号密码登录。
- 个人资料维护。
- 健康档案填写与查询。
- BMI 自动计算与合理性校验。
- 为模块 B/C 预留 `feature/workout` 和 `feature/syncplan` 目录。

## 技术栈

- Android 原生 Java
- Android Gradle Plugin 8.8.0
- compileSdk 35
- minSdk 23
- 无第三方依赖

## 目录说明

- `app/src/main/java/com/health/sports/MainActivity.java`：当前客户端入口。
- `app/src/main/java/com/health/sports/feature/account`：模块 A 账户功能。
- `app/src/main/java/com/health/sports/feature/profile`：模块 A 健康档案功能。
- `app/src/main/java/com/health/sports/feature/workout`：模块 B 运动记录入口。
- `app/src/main/java/com/health/sports/feature/syncplan`：模块 C 同步、计划与分享入口。
- `app/src/main/java/com/health/sports/model`：公共领域对象。
- `app/src/main/java/com/health/sports/store`：课堂演示用本地内存存储。

## 构建

```powershell
.\gradlew.bat :app:assembleDebug
```

生成 APK：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 演示流程

1. 打开应用，进入账户页。
2. 输入手机号、密码、昵称。
3. 点击“发送验证码”，演示验证码为 `123456`。
4. 点击“注册并进入”。
5. 在资料页维护昵称和头像地址。
6. 在档案页填写性别、出生日期、身高、体重、活动水平。
7. 保存后显示 BMI 和体重状态。

## 与后端联调

当前模块 A 使用内存存储，目的是保证 Android 客户端可独立演示。后续如果小组补 Spring Boot 后端，应在各 `feature` 模块内部封装 API 调用，再替换当前本地服务实现。
