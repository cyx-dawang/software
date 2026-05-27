# 安装部署说明

## 1. 开发环境

- Android Studio
- JDK 17，Android Studio 自带 JBR 即可
- Android SDK Platform 35
- Windows PowerShell

## 2. 导入项目

1. 打开 Android Studio。
2. 选择 `Open`。
3. 选择本仓库根目录：`C:\Users\ROG\Desktop\SoftwareEngineering\实验六\health-module-a`。
4. 等待 Gradle Sync 完成。

## 3. 编译 APK

```powershell
.\gradlew.bat :app:assembleDebug
```

编译产物：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 4. 安装运行

连接 Android 手机或启动模拟器后执行：

```powershell
.\gradlew.bat :app:installDebug
```

也可以在 Android Studio 中点击运行按钮启动 `app`。

## 5. 模块说明

模块 A 当前使用内存存储模拟服务端数据，便于单人完成编码和课堂演示。后续与其他组员联调时，可以将 `service` 层替换为 HTTP API 调用，对接 Spring Boot 后端的认证服务和档案服务。
