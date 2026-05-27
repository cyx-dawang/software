# 小组模块边界

本仓库按实验四、实验五文档中的 Android 客户端基线组织。服务端在设计文档中建议使用 Spring Boot，但当前仓库先作为 Android 客户端主工程，便于三名编码成员并行开发。

## 模块 A：用户账户与健康档案

负责人：当前已完成基础实现。

代码位置：

- `app/src/main/java/com/health/sports/feature/account`
- `app/src/main/java/com/health/sports/feature/profile`
- `app/src/main/java/com/health/sports/model`
- `app/src/main/java/com/health/sports/store`

已实现：

- 手机号验证码模拟注册
- 账号密码登录
- 个人资料维护
- 健康档案填写与查询
- BMI 计算与合理性校验

## 模块 B：运动记录

建议代码位置：

- `app/src/main/java/com/health/sports/feature/workout`

建议实现：

- 开始、暂停、继续、结束运动
- GPS 权限申请与轨迹点采集
- 距离、时长、配速、卡路里摘要
- 运动结束报告页
- 弱网或未登录状态下的本地缓存

## 模块 C：同步、训练计划与分享

建议代码位置：

- `app/src/main/java/com/health/sports/feature/syncplan`

建议实现：

- 穿戴设备健康数据同步模拟
- 步数、心率、睡眠数据展示
- 基于健康档案生成训练计划
- 计划启用、调整和执行状态
- 成就海报预览和 Android 系统分享

## 协作规则

- 不要直接重命名 `com.health.sports` 包名。
- B/C 模块优先在各自 `feature` 目录中开发，避免改动模块 A 的服务逻辑。
- 公共实体放在 `model`，临时本地数据放在 `store`。
- 如果后续接 Spring Boot 后端，先在各模块内部封装接口调用，不要把 HTTP 逻辑写进 `MainActivity`。
