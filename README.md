# EcoVault（生态保险箱）

![Java 25](https://img.shields.io/badge/Java-25-orange.svg)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4-brightgreen.svg)
![License MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![Build](https://img.shields.io/badge/Build-Maven%20%7C%20Jenkins-informational.svg)

EcoVault（生态保险箱）是一个个人数据安全存储与管理平台，面向个人、家庭与小团队私有化部署场景，提供密码、财务、日志与后台管理能力。项目强调安全存储、权限隔离、可审计、易部署与现代化用户体验。

```mermaid
graph TD
    A[浏览器] --> B[Thymeleaf 页面]
    A --> C[RESTful API]
    B --> D[Controller]
    C --> D
    D --> E[Service 业务层]
    E --> F[Repository 数据访问]
    F --> G[(SQLite 3\ndata/ecovault.db)]
    E --> H[Security\nJWT / RBAC / 单设备登录]
    E --> I[AOP 操作日志]
    E --> J[AES 加密工具]
    K[ADMIN] --> L[管理后台 / Actuator]
```

## 项目简介

EcoVault 使用 Java 25 与 Spring Boot 4 构建，主类为 `com.tlcsdm.ecovault.EcoVaultApplication`，Jar artifact id 为 `ecovault`。系统默认使用嵌入式 SQLite 数据库文件 `data/ecovault.db`，前端以 Thymeleaf 服务端渲染为主。

## 功能特性

### 用户管理

- RBAC 权限模型，内置 `USER` 与 `ADMIN`。
- JWT 登录认证，支持 `ecovault.security.max-devices` 配置登录设备数。
- BCrypt 密码哈希，禁止保存明文登录密码。
- 不提供外部自助注册，用户仅能由管理员在后台手动创建。
- 用户启用、禁用与会话失效。

### 密码管理

- 密码条目新增、编辑、删除、分页与搜索。
- 标签分类、密码强度检测与安全提示。
- 敏感密码内容使用 AES-GCM 加密存储。

### 财务管理

- 工资数据录入、编辑、删除与查询。
- 月度、年度统计分析与图表展示。
- CSV 导出，预留消费数据扩展能力。

### 日志管理

- AOP 自动记录关键操作。
- 支持按用户、模块、动作、时间筛选和搜索。
- 支持日志导出与敏感字段脱敏。

### 管理后台

- 管理员手动创建用户、启用禁用用户。
- 系统状态、构建版本、构建信息查看。
- Actuator 端点限制 `ADMIN` 访问。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 | Java 25 |
| 框架 | Spring Boot 4、Spring Security、Spring MVC |
| 数据库 | SQLite 3（嵌入式，`data/ecovault.db`） |
| 前端 | Thymeleaf、Chart.js |
| UI | 玻璃拟态、渐变、暗色/亮色主题 |
| 安全 | JWT、BCrypt、AES-GCM、CSRF、XSS、SQL 注入防护 |
| 构建测试 | Maven、JUnit 5、JaCoCo、GitHub Actions、Jenkins |
| 部署 | `deploy/deploy.sh`、Actuator |

## 项目结构

```text
EcoVault/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   ├── CODEOWNERS
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── copilot-instructions.md
├── deploy/
│   └── deploy.sh
├── docs/
│   ├── 设计文档.md
│   └── 开发规范.md
├── src/
│   ├── main/java/com/tlcsdm/ecovault/...
│   └── main/resources/templates/...
├── CHANGELOG.md
├── Jenkinsfile
├── LICENSE
├── README.md
└── CONTRIBUTING.md
```

## 快速开始

### 环境要求

- Java 25
- Maven 3.9+
- SQLite 3（嵌入式使用，通常无需单独启动）

### 构建

```bash
mvn clean package
```

### 运行

```bash
java -jar target/ecovault.jar
```

### 访问

```text
http://localhost:8100
```

默认仅初始化管理员账号；普通用户需由管理员登录后台后手动创建。

## 配置说明

关键配置由 `application.yml` 提供，建议包含：

```yaml
server:
  port: 8100
  shutdown: graceful

spring:
  jackson:
    time-zone: GMT+8
    date-format: yyyy/MM/dd HH:mm:ss
  mvc:
    format:
      date: yyyy/MM/dd
      date-time: yyyy/MM/dd HH:mm:ss
      time: HH:mm:ss

ecovault:
  security:
    jwt-secret: "请替换为生产环境强随机密钥"
    max-devices: 1
  crypto:
    secret: "请替换为生产环境 AES 密钥占位符"
```

- `server.port`：默认服务端口为 `8100`。
- `server.shutdown`：启用优雅停机，避免部署时中断正在处理的请求。
- `spring.jackson.*`：统一 JSON 时区为 `GMT+8`，格式为 `yyyy/MM/dd HH:mm:ss`。
- `spring.mvc.format.*`：统一 Spring MVC 参数绑定与表单时间格式。
- `ecovault.security.jwt-secret`：JWT 签名密钥，生产环境必须使用强随机值。
- `ecovault.security.max-devices`：单用户允许同时登录设备数，默认建议为 `1`。
- `ecovault.crypto.secret`：密码库 AES 主密钥，必须通过环境变量安全注入。

## 加密设计说明

### 登录密码

- 用户登录密码只保存 BCrypt 哈希值。
- 管理员在后台创建用户时，服务端立即使用 `BCryptPasswordEncoder` 处理初始密码。
- 登录阶段仅做哈希比对，不会回显或记录明文密码。

### 密码条目敏感字段

- 密码条目的密码、备注、标签等敏感字段统一使用 AES-GCM 加密后落库。
- 每次加密都生成新的随机 IV，并与密文一起存储，避免相同明文产生相同密文。
- 认证标签由 GCM 自带，解密时会自动校验完整性，防止密文被篡改后静默通过。
- AES 密钥仅从 `ECOVAULT_CRYPTO_SECRET` 读取，示例配置只能使用占位符，生产环境应接入安全密钥管理方案。
- 由于系统可能保存银行卡密码等高度敏感信息，文档与实现都避免输出任何明文、密钥、Token 或可逆线索。

### 相关安全控制

- JWT 仅通过 `HttpOnly` Cookie 下发，配合 CSRF Token 防止跨站请求伪造。
- 操作日志只记录参数类型，不记录敏感字段值。
- 数据访问通过 JPA 参数化能力执行，避免拼接 SQL。

## 测试与覆盖率

```bash
mvn test
```

JaCoCo 报告位置：

```text
target/site/jacoco/index.html
```

## 部署

### Jenkins

`Jenkinsfile` 包含检出、构建、测试、JaCoCo、归档与 `master` 分支部署阶段。

### deploy.sh

```bash
bash deploy/deploy.sh
```

脚本会停止旧服务、备份旧 Jar、部署 `target/ecovault.jar`，以 `prod` 配置启动，并通过 `http://127.0.0.1:8100/actuator/health` 执行健康检查。

## Actuator / 构建信息

- `/actuator/health`：健康检查。
- `/actuator/info`：构建版本、构建时间等信息。
- 管理员可通过 Actuator 与后台页面查看构建信息。
- 除健康检查必要场景外，Actuator 信息必须限制为 `ADMIN`。

## 安全说明

- JWT 用于认证，结合服务端会话记录实现单设备登录。
- BCrypt 用于登录密码哈希。
- AES-GCM 用于加密密码条目敏感字段。
- 页面表单启用 CSRF 防护，输出内容进行 HTML 转义防止 XSS。
- 数据访问必须使用参数绑定或 ORM 参数化能力，防止 SQL 注入。
- 日志中禁止输出密钥、Token、明文密码、数据库文件内容等敏感信息。

## 贡献指南

请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)、[CHANGELOG.md](CHANGELOG.md) 与 [docs/开发规范.md](docs/开发规范.md)。每次变更都应同步更新文档并补充必要测试。

## 开源协议

本项目基于 MIT 协议开源。版权所有 © unknowIfGuestInDream（2026）。
