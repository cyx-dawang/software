# 运动与健康系统 - 模块A

模块 A 覆盖用户账户与健康档案功能，用于实验六“编码与测试”的代码提交。

## 功能范围

- 手机号注册与登录
- 验证码模拟发送与校验
- 用户资料查询与修改
- 健康档案创建、查询与修改
- BMI 自动计算与健康档案合法性校验
- 基于 Java 8 标准库的轻量 HTTP 接口

## 运行环境

- JDK 8 或以上
- Windows PowerShell
- 不依赖 Maven、Gradle 或第三方库

## 编译与运行

```powershell
.\scripts\build.ps1
.\scripts\run.ps1
```

服务默认监听 `http://localhost:8080`。

## 接口说明

### 健康检查

```http
GET /health
```

### 发送验证码

```http
POST /auth/send-code

mobile=13800138000
```

演示环境固定返回验证码 `123456`。

### 注册

```http
POST /auth/register

mobile=13800138000&code=123456&password=Pass1234&nickname=zhangsan
```

### 登录

```http
POST /auth/login

mobile=13800138000&password=Pass1234
```

### 查询用户资料

```http
GET /users/{userId}
```

### 修改用户资料

```http
PUT /users/{userId}

nickname=newName&avatarUrl=https://example.com/a.png
```

### 保存健康档案

```http
PUT /profiles/{userId}

gender=MALE&birthDate=2003-05-20&heightCm=175&weightKg=68&activityLevel=MODERATE
```

### 查询健康档案

```http
GET /profiles/{userId}
```

## 测试

```powershell
.\scripts\test.ps1
```

测试覆盖账户注册登录、重复注册、档案校验、BMI 计算和资料更新。
