# 变更日志

所有重要变更都会记录在本文件中。

## [Unreleased]

### Changed

- JWT 默认登录有效期调整为 24 小时，并统一使用 `ecovault.security.jwt-expiration-ms` 控制令牌与 Cookie 失效时间。
- 主分支相关说明统一调整为 `master`。
- 默认服务端口从 `8080` 调整为 `8100`，部署健康检查地址同步更新。
- 用户自助注册入口下线，改为管理员在后台手动创建用户。
- Spring Boot 升级为 `4.1.0`，`sqlite-jdbc` 升级为 `3.53.2.0`，`jjwt` 升级为 `0.13.0`。
- 补充 `maven-enforcer-plugin`、`maven-compiler-plugin`、`maven-surefire-plugin`、`maven-javadoc-plugin`、`maven-source-plugin`、`maven-jar-plugin`、`maven-help-plugin`、`spring-javaformat-maven-plugin`。
- GitHub Actions 版本更新为当前最新稳定标签。
- 文档新增 AES-GCM 加密流程、安全边界、GMT+8 时间格式与优雅停机说明。

### Added

- 新增 `.github/dependabot.yml`，每周检查 Maven 依赖与 GitHub Actions 更新。
- 新增统一错误页 `templates/error.html`。
- 新增 `DateTimeConfig`，统一 Jackson 与 Spring MVC 时间格式。
- 新增后台创建用户能力。

### Removed

- 删除 `Dockerfile` 与 `.dockerignore`。
