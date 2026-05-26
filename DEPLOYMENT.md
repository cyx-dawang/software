# 安装部署说明

## 1. 环境准备

安装 JDK 8 或以上，并确认 PowerShell 中可以执行：

```powershell
java -version
javac -version
```

本项目只使用 Java 标准库，不需要安装 Maven、Gradle、MySQL 或 Redis。

## 2. 编译项目

进入项目根目录：

```powershell
cd C:\Users\ROG\Desktop\SoftwareEngineering\实验六\health-module-a
.\scripts\build.ps1
```

编译产物输出到 `out\main`。

## 3. 启动服务

```powershell
.\scripts\run.ps1
```

启动成功后访问：

```http
http://localhost:8080/health
```

## 4. 执行测试

```powershell
.\scripts\test.ps1
```

看到 `All module A tests passed.` 表示模块 A 的核心测试通过。

## 5. 示例调用

发送验证码：

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/send-code -Body "mobile=13800138000"
```

注册：

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/register -Body "mobile=13800138000&code=123456&password=Pass1234&nickname=alice"
```

登录：

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/login -Body "mobile=13800138000&password=Pass1234"
```

保存健康档案：

```powershell
Invoke-RestMethod -Method Put -Uri http://localhost:8080/profiles/1001 -Body "gender=FEMALE&birthDate=2002-06-01&heightCm=165&weightKg=55&activityLevel=MODERATE"
```
