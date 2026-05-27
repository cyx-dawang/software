# 安装部署说明

## 1. 开发环境

- Android Studio
- Android Studio 自带 JBR/JDK 17
- Android SDK Platform 35
- Windows PowerShell

## 2. 导入工程

1. 打开 Android Studio。
2. 选择 `Open`。
3. 选择仓库根目录：`C:\Users\ROG\Desktop\SoftwareEngineering\实验六\health-module-a`。
4. 等待 Gradle Sync 完成。

## 3. 命令行构建

```powershell
.\gradlew.bat :app:assembleDebug
```

如果项目位于中文路径，`gradle.properties` 中已配置：

```properties
android.overridePathCheck=true
```

## 4. 安装到设备或模拟器

连接 Android 手机或启动模拟器：

```powershell
.\gradlew.bat :app:installDebug
```

也可以在 Android Studio 中选择 `app` 后点击运行。

## 5. 提交内容说明

提交实验六源代码时，建议包含：

- `app/`
- `gradle/`
- `build.gradle`
- `settings.gradle`
- `gradlew`
- `gradlew.bat`
- `gradle.properties`
- `README.md`
- `DEPLOYMENT.md`
- `MODULES.md`

不要提交 `app/build/`、`.gradle/`、`local.properties` 等本机构建产物和本地配置。
