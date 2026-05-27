# 安装部署说明

## 1. 环境准备

安装 JDK 8 或以上，并确认命令可用：

```powershell
java -version
javac -version
```

本软件不需要数据库、中间件、Web 服务器、Maven 或 Gradle。

## 2. 编译

进入项目目录：

```powershell
cd C:\Users\ROG\Desktop\SoftwareEngineering\实验六\health-module-a
.\scripts\build.ps1
```

编译后的 class 文件位于 `out\main`。

## 3. 启动桌面软件

```powershell
.\scripts\run.ps1
```

启动后会弹出桌面窗口。窗口包含三个页签：

- 注册登录
- 个人资料
- 健康档案

## 4. 功能演示建议

注册演示：

- 手机号：`13800138000`
- 验证码：点击“发送验证码”后自动填入 `123456`
- 密码：`Pass1234`
- 昵称：`alice`

健康档案演示：

- 性别：`FEMALE`
- 出生日期：`2003-01-01`
- 身高：`165`
- 体重：`55`
- 活动水平：`MODERATE`

保存后软件会显示 BMI 结果和体重状态。

## 5. 测试

```powershell
.\scripts\test.ps1
```

出现以下输出表示测试通过：

```text
All module A tests passed.
```
