# EcoVault 入职指南

> 面向新加入团队、初次接触 EcoVault 的开发者。本文基于项目知识图谱自动生成，涵盖架构分层、核心概念、推荐学习路径、文件地图与复杂度热点。

## 1. 项目概览

| 项目 | 说明 |
| --- | --- |
| **名称** | EcoVault（生态保险箱） |
| **定位** | 个人数据安全存储与管理平台，面向个人、家庭与小团队私有化部署 |
| **语言** | Java 25（主）、HTML、CSS、JavaScript、YAML、XML |
| **框架** | Spring Boot 4、Spring MVC、Spring Security、Spring Data JPA、Thymeleaf、Hibernate、SQLite、JWT |
| **构建/测试** | Maven、JUnit 5、JaCoCo |
| **数据库** | SQLite 3（嵌入式，`data/ecovault.db`） |

核心能力：用户管理（RBAC）、密码管理、财务管理（工资/流水）、操作日志、管理后台。前端以 Thymeleaf 服务端渲染为主，Chart.js 负责图表。

## 2. 架构分层

系统按经典的 Web 分层组织，共 12 层：

| 分层 | 说明 | 关键文件 |
| --- | --- | --- |
| 应用入口 | Spring Boot 启动引导 | `EcoVaultApplication.java` |
| 控制器层 | 页面渲染与 REST API 路由 | 7 个 `*Controller.java` |
| 服务层 | 业务接口与实现（核心逻辑） | 7 个 `*Service` + 7 个 `*ServiceImpl` |
| 数据访问层 | Spring Data JPA 仓储 | 7 个 `*Repository.java` |
| 领域模型层 | JPA 实体与数据模型 | 10 个实体类 |
| 数据传输层 | 请求/响应 DTO | 20 个 DTO |
| 安全组件 | 认证、授权、JWT、CSRF | 6 个安全类 |
| 配置层 | Spring 配置与运行时/构建配置 | 配置类、`application.yml`、`pom.xml` 等 |
| 横切关注点 | AOP 切面与自定义注解 | `OperationLogAspect`、`OperationLogRecord` |
| 通用与工具 | 通用组件与工具类 | `ApiResponse`、`AesUtil`、`PasswordStrengthUtil` 等 |
| 前端资源 | Thymeleaf 模板与静态资源 | 13 个模板 + CSS/JS |
| 测试 | 单元测试与集成测试 | 37 个测试文件 |

## 3. 核心概念

- **RBAC 权限模型**：内置 `USER` 与 `ADMIN` 两种角色，`RolePermission` 维护角色与页面的权限矩阵，管理员在后台手动创建用户（无自助注册）。
- **JWT 认证**：`JwtTokenProvider` 负责令牌生成与校验，`JwtAuthenticationFilter` 在过滤器链中解析，`CustomUserDetailsService` 加载用户；令牌通过 HttpOnly Cookie 下发，配合 CSRF Token。
- **BCrypt 密码哈希**：登录密码仅存哈希，绝不保存明文。
- **AES-GCM 加密存储**：密码条目的密码、备注、标签等敏感字段使用 AES-GCM 加密，每次随机 IV（见 `AesUtil`）。
- **CSRF 防护**：`CsrfCookieFilter` 提供 CSRF Token，配合 Cookie 中的 JWT。
- **AOP 操作日志**：`OperationLogRecord` 注解 + `OperationLogAspect` 切面自动记录关键操作，支持按用户/模块/动作/时间检索与导出脱敏。
- **SQLite 方言**：`EcoVaultSQLiteDialect` 提供 Hibernate 社区方言；连接池上限为 1，避免 SQLite 写锁冲突。
- **分层约定**：每个业务模块遵循 `Controller → Service（接口）→ ServiceImpl（实现）→ Repository → Entity` 的调用链。
- **DTO 解耦**：请求/响应使用独立 DTO（record），与 JPA 实体分离。
- **服务端渲染**：页面以 Thymeleaf 为主，`PageController` 负责页面路由，REST 控制器返回 JSON。

## 4. 引导式导览（推荐学习顺序）

1. **项目概览** — 从 `pom.xml` 与入口类了解整体定位与启动方式。
2. **应用入口与配置** — 阅读 `EcoVaultApplication` 与 `application.yml`/`application-prod.yml`/`logback-spring.xml`，理解数据源、端口、安全等设置。
3. **安全认证链路** — 沿 `SecurityConfig` → `JwtTokenProvider` → `JwtAuthenticationFilter` → `CsrfCookieFilter` → `CustomUserDetailsService` 理解登录与会话控制。
4. **领域数据模型** — 浏览 10 个 JPA 实体，掌握用户、密码、财务、日志与权限的数据结构。
5. **数据访问层** — 查看 7 个 Repository，理解持久化与查询能力。
6. **业务服务层** — 阅读 Service 接口与实现，理解密码加密、工资统计、日志记录等核心逻辑。
7. **控制器与 API** — 浏览控制器与 DTO，了解页面路由与 REST API 请求处理。
8. **前端界面** — 查看 Thymeleaf 模板与静态资源，了解页面结构、样式与交互。
9. **测试覆盖** — 浏览测试代码，了解各层的测试写法与覆盖。

## 5. 文件地图

### 应用入口
- `EcoVaultApplication.java` — Spring Boot 启动入口，负责初始化数据库目录并引导应用。

### 控制器层（`controller/`）
- `AuthController` — 登录、登出、当前用户、资料/密码修改、密码验证（`/api/auth`）。
- `PasswordController` — 密码条目增删改查（`/api/passwords`）。
- `SalaryController` — 工资数据、统计与导出（`/api/finance/salaries`）。
- `LedgerController` — 财务流水、统计与导出（`/api/finance/ledger`）。
- `AdminController` — 用户管理、角色权限、构建信息与 Actuator 入口（`/api/admin`）。
- `LogController` — 操作日志查询/编辑/删除/导出（`/api/logs`）。
- `PageController` — Thymeleaf 页面渲染（`/`、`/login`、`/dashboard` 等）。

### 服务层（`service/` + `service/impl/`）
- `AuthService` / `AuthServiceImpl` — 登录、注册（管理员）、资料与密码管理、设备数控制。
- `PasswordService` / `PasswordServiceImpl` — 密码条目业务，含 AES-GCM 加密与强度检测。
- `SalaryService` / `SalaryServiceImpl` — 工资增删改查、月度/年度统计、CSV 导出。
- `LedgerService` / `LedgerServiceImpl` — 财务流水统计与导出。
- `AdminService` / `AdminServiceImpl` — 用户/角色权限管理。
- `OperationLogService` / `OperationLogServiceImpl` — 操作日志查询与导出。
- `RolePermissionService` / `RolePermissionServiceImpl` — 角色权限矩阵。

### 数据访问层（`repository/`）
- `UserRepository`、`UserSessionRepository`、`PasswordEntryRepository`、`SalaryRecordRepository`、`LedgerEntryRepository`、`OperationLogRepository`、`RolePermissionRepository` — 对应的 Spring Data JPA 仓储。

### 领域模型层（`entity/`）
- `User`、`UserSession`、`PasswordEntry`、`SalaryRecord`、`LedgerEntry`、`LedgerType`、`OperationLog`、`Role`、`RolePermission`、`MenuPage`。

### 安全组件（`security/`）
- `SecurityConfig` — 安全过滤链、CSRF、JWT 过滤器、Actuator 的 ADMIN 限制。
- `JwtTokenProvider` / `JwtAuthenticationFilter` — JWT 生成/校验与认证过滤。
- `CustomUserDetailsService` / `SecurityUser` — 用户加载与安全主体。
- `CsrfCookieFilter`、`SecurityUtils` — CSRF 与当前用户工具方法。

### 配置层（`config/` + resources）
- `SecurityConfig`、`WebMvcConfig`、`DateTimeConfig`、`DataInitializer`、`EcoVaultSQLiteDialect`。
- `application.yml`、`application-prod.yml`、`logback-spring.xml`、`banner.txt`、`pom.xml`、`.mvn/jvm.config`。

### 横切关注点（`aspect/` + `annotation/`）
- `OperationLogAspect` / `OperationLogRecord` — AOP 操作日志。

### 通用与工具（`common/` + `utils/`）
- `ApiResponse`、`BusinessException`、`GlobalExceptionHandler`、`AesUtil`、`PasswordStrengthUtil`、`WebUtil`。

### 前端资源（`templates/` + `static/`）
- 页面模板：`login`、`dashboard`、`passwords`、`finance`、`ledger`、`logs`、`users`、`roles`、`admin`、`profile`、`index`、`error`、`fragments/common`。
- 静态资源：`style.css`、`privacy.css`、`app.js`、`privacy.js`。

## 6. 复杂度热点（建议谨慎对待）

**生产代码**
- `SalaryRecord.java` — 工资实体，字段与计算逻辑最多。
- `SalaryServiceImpl.java` — 工资统计、年度奖金与 CSV 导出逻辑集中。
- `LedgerServiceImpl.java` — 财务流水统计逻辑。
- `style.css` / `passwords.html` / `users.html` / `logs.html` / `finance.html` / `ledger.html` / `app.js` — 前端样式与交互最复杂。
- `pom.xml` — 构建配置、插件与依赖声明较多。

**测试代码**
- `AuthServiceImplTest`、`SalaryServiceImplTest`、`PasswordServiceImplTest`、`JwtAuthenticationFilterTest`、`OperationLogAspectTest`、`EntityTest`、`AuthControllerTest`、`AdminServiceImplTest`、`LedgerServiceImplTest` — 覆盖核心业务与安全链路的复杂测试，修改对应逻辑时需同步关注。
