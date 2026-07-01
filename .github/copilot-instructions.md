# EcoVault AI 协作指令

你正在协助开发 EcoVault（生态保险箱），这是一个个人数据安全存储与管理平台。所有生成内容必须遵循以下规则。

## 基础信息

- Java 基础包名：`com.tlcsdm.ecovault`。
- 应用主类：`com.tlcsdm.ecovault.EcoVaultApplication`。
- Jar artifact id：`ecovault`。
- 技术栈：Java 25、Spring Boot 4.1.x、SQLite 3、Thymeleaf、Maven、JUnit 5、JaCoCo、Jenkins。
- 前端图表可使用 Chart.js 或 ECharts。
- UI 风格应现代、响应式，支持玻璃拟态、渐变、暗色/亮色主题。

## 语言与编码

- 所有注释、文档与面向用户的说明使用中文。
- 文件编码统一为 UTF-8。
- Java 代码遵循《阿里巴巴 Java 开发手册》。

## 架构与功能要求

- 使用 RBAC 权限模型，角色包括 `USER` 与 `ADMIN`。
- JWT 用于认证，并支持单设备登录或通过 `ecovault.security.max-devices` 配置设备数量。
- 用户密码必须使用 BCrypt 哈希。
- 密码条目敏感字段必须使用 AES 加密。
- 使用 AOP 自动记录关键操作日志，并进行敏感信息脱敏。
- API 设计必须符合 RESTful 规范。
- 数据库查询必须使用参数绑定，并根据查询场景优化索引。
- Actuator 端点限制为 ADMIN 访问，健康检查端点按部署需要最小暴露。
- 不提供外部自助注册，普通用户仅能由管理员在后台创建。

## 测试要求

- 新增或修改业务逻辑必须补充完整单元测试。
- 使用 JUnit 5 编写测试。
- 使用 JaCoCo 生成覆盖率报告。
- 安全、加密、权限、单设备登录、CSV 导出与统计逻辑必须覆盖边界场景。

## 部署与运维要求

- 保持 `Jenkinsfile` 与 `deploy/deploy.sh` 可用。
- 应用以 `prod` profile 启动。
- Jenkins 流水线应包含构建、测试、覆盖率、归档与主分支部署。
- 部署脚本必须包含停止旧服务、备份旧版本、部署新 Jar、启动服务与健康检查。
- 健康检查使用 Actuator `/actuator/health`。

## 文档与安全规则

- 每次都要更新 `docs` 文档，尤其是功能、接口、数据库、安全策略与部署流程变更。
- 检查代码不要包含私密信息，包括真实密钥、Token、密码、数据库文件、证书与生产配置。
- 日志、异常、导出文件与页面展示不得泄露敏感信息。
- 生成示例配置时只能使用占位符。
