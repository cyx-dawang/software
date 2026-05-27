# 运动与健康系统 - 模块A Android客户端

本仓库实现实验六中本人负责的模块 A：用户账户与健康档案。根据前序需求规格说明书和软件设计文档，系统开发平台为 Android 客户端，服务端建议采用 Spring Boot。本仓库当前提交的是 Android 客户端模块代码。

## 功能范围

- 手机号验证码模拟注册
- 账号密码登录
- 当前用户状态显示
- 个人资料维护
- 健康档案填写与查询
- BMI 自动计算和体重状态展示
- 健康档案输入范围校验

## 技术栈

- Android 原生 Java
- Android Gradle Plugin 8.8.0
- compileSdk 35
- minSdk 23
- 无第三方依赖

## 项目结构

- `app/src/main/java/com/health/modulea/MainActivity.java`：模块 A Android 页面入口
- `app/src/main/java/com/health/modulea/model`：用户和健康档案领域对象
- `app/src/main/java/com/health/modulea/service`：账户服务与健康档案服务
- `app/src/main/java/com/health/modulea/store`：课堂演示用内存存储

## 运行方式

使用 Android Studio 打开本仓库根目录，等待 Gradle 同步完成后运行 `app`。

也可以在命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

生成 APK：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 演示流程

1. 打开应用后进入“注册登录”页。
2. 输入手机号、密码和昵称。
3. 点击“发送验证码”，演示验证码为 `123456`。
4. 点击“注册并进入”，顶部显示当前用户。
5. 切换到“个人资料”页维护昵称和头像地址。
6. 切换到“健康档案”页填写性别、出生日期、身高、体重、活动水平。
7. 点击“保存健康档案”，页面显示 BMI 和体重状态。
