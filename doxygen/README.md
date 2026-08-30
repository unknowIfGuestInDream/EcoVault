# EcoVault Doxygen 文档生成

本目录包含 EcoVault 项目的 Doxygen 配置和相关文档资源。

## 目录结构

```
doxygen/
├── Doxyfile               # Doxygen 主配置文件
├── mainpage.dox           # 文档首页内容
├── architecture.dox       # 架构设计文档
├── custom.css             # 自定义样式表
├── custom.js              # 自定义 JavaScript
├── footer.html            # 页脚模板
├── images/                # 图片资源目录
└── pages/                 # 额外的文档页面
```

## 生成文档

### 前提条件

确保已安装以下工具：

- Doxygen 1.9.1+
- Graphviz（用于生成类图、调用图等）
- Java Runtime（用于 PlantUML）
- PlantUML JAR 文件（可选，用于 UML 图表）

#### Linux/Ubuntu 安装

```bash
sudo apt-get update
sudo apt-get install -y doxygen graphviz default-jre

# 下载 PlantUML（可选）
sudo mkdir -p /opt/plantuml
sudo wget -O /opt/plantuml/plantuml.jar \
  https://github.com/plantuml/plantuml/releases/download/v1.2024.8/plantuml-1.2024.8.jar
export PLANTUML_JAR_PATH=/opt/plantuml/plantuml.jar
```

#### macOS 安装

```bash
brew install doxygen graphviz

# 下载 PlantUML（可选）
mkdir -p ~/plantuml
wget -O ~/plantuml/plantuml.jar \
  https://github.com/plantuml/plantuml/releases/download/v1.2024.8/plantuml-1.2024.8.jar
export PLANTUML_JAR_PATH=~/plantuml/plantuml.jar
```

#### Windows 安装

1. 从 [Doxygen 官网](https://www.doxygen.nl/download.html) 下载并安装
2. 从 [Graphviz 官网](https://graphviz.org/download/) 下载并安装
3. 确保 `doxygen` 和 `dot` 在 PATH 中
4. 下载 [PlantUML JAR](https://plantuml.com/download)
5. 设置环境变量 `PLANTUML_JAR_PATH`

### 本地生成

从项目根目录执行：

```bash
# 设置 PlantUML JAR 路径（如果已安装）
export PLANTUML_JAR_PATH=/opt/plantuml/plantuml.jar

# 生成文档
doxygen doxygen/Doxyfile

# 查看生成的文档
open docs-gen/html/index.html    # macOS
xdg-open docs-gen/html/index.html  # Linux
start docs-gen/html/index.html     # Windows
```

### CI/CD 生成

#### GitHub Actions

项目包含 `.github/workflows/doxygen.yml` 工作流，会在以下情况自动生成文档：

- 推送到 `master` 分支
- 提交 PR 到 `master` 分支
- 修改了 `src/**`、`doxygen/**`、`README.md` 等文件

生成的文档会作为构建产物上传，保留 90 天。

#### Jenkins

Jenkinsfile 包含 `Generate Doxygen Docs` 阶段，会在每次构建时生成文档。

生成的文档会被打包为 `doxygen-docs.zip` 并归档。

## 配置说明

### Doxyfile

主要配置项：

- `PROJECT_NAME`: 项目名称（EcoVault）
- `PROJECT_NUMBER`: 项目版本号（1.0.1，与 pom.xml 保持一致）
- `OUTPUT_LANGUAGE`: 输出语言（Chinese，中文）
- `INPUT`: 输入源文件路径
- `EXCLUDE`: 排除的文件和目录（docs、deploy 等）
- `EXTRACT_ALL`: 提取所有代码元素（YES）
- `EXTRACT_PRIVATE`: 提取私有成员（YES）
- `GENERATE_HTML`: 生成 HTML 文档（YES）
- `GENERATE_LATEX`: 生成 LaTeX 文档（NO）
- `HAVE_DOT`: 使用 Graphviz 生成图表（YES）
- `PLANTUML_JAR_PATH`: PlantUML JAR 路径（从环境变量读取）

### 图表支持

Doxygen 支持以下类型的图表：

- **类图**：显示类的继承关系
- **协作图**：显示类的成员关系
- **调用图**：显示函数调用关系
- **被调用图**：显示函数被调用关系
- **目录图**：显示目录结构
- **PlantUML 图**：自定义 UML 图表

在文档中使用 PlantUML：

```java
/**
 * 认证流程：
 *
 * @startuml
 * actor 用户
 * participant "前端" as FE
 * participant "后端" as BE
 *
 * 用户 -> FE: 输入用户名密码
 * FE -> BE: POST /auth/login
 * BE -> BE: 验证密码
 * BE -> FE: 返回 JWT 令牌
 * @enduml
 */
```

## 自定义样式

### custom.css

提供自定义样式，包括：

- 现代化配色方案
- 渐变导航栏
- 优化的代码块样式
- 表格样式
- 警告框样式
- 暗色主题支持

### custom.js

提供额外的交互功能：

- 代码块复制按钮
- 平滑滚动
- 回到顶部按钮
- 图片灯箱效果
- 外部链接标记

## 文档内容

### mainpage.dox

项目主页，包含：

- 项目简介
- 技术栈
- 核心功能
- 架构概览
- 安全策略
- 部署运维
- 开发指南
- 模块说明

### architecture.dox

架构设计文档，包含：

- 架构分层（表现层、业务层、持久层）
- 安全架构（认证流程、授权流程、加密策略）
- 数据模型（用户、密码、工资、日志等实体）
- AOP 切面设计
- 部署架构
- CI/CD 流程
- 未来扩展规划

## 最佳实践

### Javadoc 注释规范

1. **类级别注释**：

```java
/**
 * 密码管理服务接口。
 *
 * <p>
 * 提供密码条目的增删改查、加解密等功能。
 * </p>
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public interface PasswordService {
    // ...
}
```

2. **方法级别注释**：

```java
/**
 * 创建密码条目。
 *
 * @param userId 用户 ID
 * @param request 密码条目请求
 * @return 创建的密码条目响应
 * @throws IllegalArgumentException 如果参数无效
 */
PasswordEntryResponse create(Long userId, PasswordEntryRequest request);
```

3. **字段注释**：

```java
/**
 * 密码服务，用于密码条目管理。
 */
private final PasswordService passwordService;
```

### 更新文档

修改代码后，记得更新相应的注释：

1. 新增类、方法、字段时，添加 Javadoc 注释
2. 修改功能时，更新相应的注释描述
3. 重构后，检查注释是否仍然准确
4. 提交前运行 Doxygen 检查警告

## 故障排查

### Doxygen 警告

如果生成文档时出现警告，检查：

- 所有公共类、方法、字段是否都有注释
- 注释格式是否正确（/** ... */）
- @param、@return 等标签是否完整
- 引用的类、方法是否存在

### 图表未生成

如果图表未生成，检查：

- Graphviz 是否已安装并在 PATH 中
- `HAVE_DOT` 配置是否为 YES
- 类之间是否有关系（继承、组合等）

### PlantUML 图表未生成

如果 PlantUML 图表未生成，检查：

- PlantUML JAR 文件是否存在
- `PLANTUML_JAR_PATH` 环境变量是否设置正确
- Java Runtime 是否已安装

## 维护

### 版本更新

修改项目版本时，同步更新以下文件：

- `doxygen/Doxyfile` 中的 `PROJECT_NUMBER`
- `doxygen/mainpage.dox` 中的 `@version` 标签
- `doxygen/architecture.dox` 中的 `@version` 标签

### 添加新页面

在 `doxygen/pages/` 目录下添加新的 `.dox` 或 `.md` 文件，然后在 `Doxyfile` 的 `INPUT` 中添加：

```
INPUT = ... \
        doxygen/pages/new-page.dox
```

## 参考资料

- [Doxygen 官方文档](https://www.doxygen.nl/manual/)
- [Javadoc 注释规范](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- [PlantUML 官方文档](https://plantuml.com/)
- [Graphviz 官方文档](https://graphviz.org/documentation/)

## 许可证

本文档配置遵循 EcoVault 项目的 MIT License。
