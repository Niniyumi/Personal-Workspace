# Personal Agent 前后端联调、Tomcat 部署与结构化周报设计

**日期：** 2026-08-24

**状态：** 待用户书面复核

**适用项目：** Personal Agent

**关联基础设计：** `docs/superpowers/specs/2026-08-23-personal-agent-design.md`

## 1. 本次增量目标

本次增量分为两个可独立验收的子项目，严格按顺序执行：

1. 完成 Vue 登录页、真实工作台与现有 Spring Boot 认证接口的端到端联调，并让项目可以使用 IntelliJ IDEA 中的外部 Tomcat 部署测试。
2. 将原先偏聊天式的周报设计重构为结构化周报助手，支持三栏填写、DOCX 智能填充、年月历史查询，以及季度和年度总结。

只有第一个子项目通过真实联调检查点，才开始第二个子项目。

## 2. 设计原则

- 优先实现常用主流程，不为极少数异常建立复杂备用链路。
- 数据库保存结构化业务数据，模型只负责分类和总结。
- 所有业务数据必须归属于当前 JWT 用户，接口不接受客户端传入的 `userId`。
- 模型调用只在用户明确点击时发生，不因普通保存自动调用。
- 当前只启用一个模型 Provider，不实现多模型自动重试和故障转移。
- 不引入 Redis、MinIO、消息队列或搜索引擎。
- 关键 Java、TypeScript 代码保留必要的中文注释，不写解释显而易见语句的冗余注释。

## 3. 子项目一：真实联调与 Tomcat 部署

### 3.1 当前状态

现有 Vue 认证模块已经实现以下请求：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

Vite 开发服务器通过 `/api` 代理访问 `http://127.0.0.1:8080`。登录页已经改为确认过的 Element Plus 日杂风格，但 `/preview` 工作台仍使用静态用户和静态进度数据，真实 `HomeView` 尚未采用这套视觉结构。

### 3.2 联调目标

- 注册成功后能够使用用户名或邮箱登录。
- 登录成功后进入受保护的真实工作台。
- 工作台展示 `/api/users/me` 返回的显示名称和邮箱。
- 浏览器刷新后能够恢复登录状态。
- Access Token 失效时只尝试一次 Refresh Token 更新，禁止循环重试。
- 退出后清理本地令牌并返回登录页。
- 未登录用户访问受保护页面时自动进入登录页。
- 静态预览视觉迁移完成并验证后删除 `/preview`，避免保留两套工作台代码。

### 3.3 开发与部署方式

项目保留两种用途明确的运行方式，但共用同一套业务代码：

- 日常前端开发：Vue Vite 开发服务器负责热更新，Spring Boot 应用提供 API。
- 阶段验收和面试演示：Vue 生产资源与 Spring Boot 一起打包成 WAR，由 IDEA 配置的外部 Tomcat 部署。

这不是两套实现，也不引入运行时自动切换逻辑。

### 3.4 WAR 改造

后端构建需要进行以下调整：

- Maven 包类型改为 `war`。
- `PersonalAgentApplication` 继承 `SpringBootServletInitializer` 并覆盖 `configure`。
- `spring-boot-starter-tomcat` 标记为 `provided`。
- Maven 打包时把已有的 `frontend/dist` 放入 WAR 根目录。
- 前端继续使用同源 `/api` 地址。
- Spring MVC 对明确的 Vue 页面路由转发到 `/index.html`，不得拦截 `/api/**` 和静态资源。
- WAR 的应用上下文固定为 `/`，避免前端 API 地址因上下文名称发生变化。

Spring Boot 3.5.16 使用 Java 21，并以 Tomcat 10.1.25 或更高的 10.1.x 版本作为外部容器。

### 3.5 IDEA 配置文档

新增 `docs/development/idea-tomcat.md`，记录：

- IDEA Ultimate 与 Tomcat 插件要求。
- Tomcat 10.1 本机目录配置。
- Project SDK 和 Tomcat JRE 均选择 Java 21。
- `war exploded` Artifact 的添加方式。
- Application Context 设置为 `/`。
- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 和模型配置的填写位置。
- MySQL `work` 数据库启动要求。
- 8080 端口冲突处理：外部 Tomcat 与内置 Tomcat 不同时启动。
- 启动后的首页、登录页和 API 健康检查步骤。

`.idea/workspace.xml` 等包含本机路径的文件不提交到 Git。

## 4. 子项目二：结构化周报助手

### 4.1 对旧设计的替代范围

本设计替代基础设计文档中的以下周报部分：

- 第 10 节“周报工具”的对话式素材收集与生成流程。
- 第 14.9 节 `weekly_reports` 字段设计。
- 第 14.10 节 `weekly_report_materials` 表设计。
- 第 15.3 节周报接口设计。

首期不创建周报聊天会话表和素材消息表。周报页面使用结构化表单；Agent 能力体现在 DOCX 分类和周期总结中。

### 4.2 周报表单

每份周报包含以下字段：

1. `本周核心工作`：必填。
2. `遇到的问题`：可选。
3. `下周工作计划`：可选。

用户选择周报所属周，界面默认当前周的星期一。系统用 `week_start_date` 表示该周；同一用户同一周只允许一份周报。月份查询以 `week_start_date` 所在月份为准。

用户可以新建、查看和修改周报。本次不增加发布状态、审批流程和复杂版本记录。

### 4.3 DOCX 智能填充

DOCX 导入流程如下：

1. 用户在周报编辑页面选择一个 DOCX 文件。
2. 后端使用 Apache POI 提取普通段落和表格单元格文字。
3. 周报 AI 服务将提取文字和分类规则发送给当前 Chat Provider。
4. Provider 返回严格的结构化结果：`coreWork`、`problems`、`nextWeekPlan`。
5. 无法确定分类的文字必须进入 `coreWork`。
6. 后端返回分类预览，不直接保存周报。
7. 前端把分类结果追加到对应输入框，不覆盖用户已经输入的内容。
8. 用户检查、编辑并主动保存。

如果模型请求失败或结构化结果无法解析，后端仍返回成功提取的全部文字，并把它放入 `coreWork`，避免文档内容丢失。

首期限制：

- 仅接受 `.docx`，不支持旧版 `.doc`。
- 单文件大小上限为 10 MB。
- 只保存提取结果和原始文件名，不永久保存原始 DOCX。
- 不做 OCR；图片中的文字不在本次范围内。

### 4.4 历史查询

周报历史页面提供年份和月份筛选。后端将年月转换为日期区间，并按 `week_start_date` 查询当前用户数据，结果按更新时间倒序返回。

用户可以：

- 查看某年某月的周报列表。
- 打开一份周报查看三个字段的完整内容。
- 返回编辑页面修改并保存。

数据库不重复保存可由 `week_start_date` 推导出的年份和月份字段。

### 4.5 季度与年度总结

用户可以选择某年某季度或某一整年，并主动点击生成。后端读取该周期内当前用户的周报，将以下内容交给周报 AI 服务：

- 本周核心工作。
- 遇到的问题。
- 下周工作计划。

Provider 返回固定结构：

1. `核心内容`：阶段内影响较大、完成度较高的工作。
2. `日常工作`：持续性、重复性或支持性工作。
3. `自我评分`：0 到 100 的整数。

自我评分是个人复盘建议，不表示正式绩效结论。用户可以修改生成内容和评分后保存。

生成规则：

- 只有点击“生成”或“重新生成”才调用模型。
- 保存或修改周报不会自动刷新已有总结。
- 页面显示总结最后生成时间。
- 同一用户同一周期只保留当前总结，重新生成时覆盖，不建立版本树。
- 所选周期没有周报时不调用模型，直接提示用户先填写周报。

## 5. 数据模型

### 5.1 `weekly_reports`

| 字段 | 类型与约束 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` 主键 | 周报 ID |
| `user_id` | `BIGINT NOT NULL` | 所属用户 |
| `week_start_date` | `DATE NOT NULL` | 周一日期 |
| `core_work` | `LONGTEXT NOT NULL` | 本周核心工作 |
| `problems` | `LONGTEXT NULL` | 遇到的问题 |
| `next_week_plan` | `LONGTEXT NULL` | 下周工作计划 |
| `source_file_name` | `VARCHAR(255) NULL` | 最近一次导入文件名 |
| `created_at` | `DATETIME(6) NOT NULL` | 创建时间 |
| `updated_at` | `DATETIME(6) NOT NULL` | 更新时间 |

约束与索引：

- 唯一索引：`(user_id, week_start_date)`。
- 查询索引：`(user_id, week_start_date DESC)`。
- `user_id` 外键引用 `users.id`。

### 5.2 `work_summaries`

| 字段 | 类型与约束 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` 主键 | 总结 ID |
| `user_id` | `BIGINT NOT NULL` | 所属用户 |
| `period_type` | `VARCHAR(16) NOT NULL` | `QUARTER` 或 `YEAR` |
| `period_start` | `DATE NOT NULL` | 周期开始日期 |
| `period_end` | `DATE NOT NULL` | 周期结束日期 |
| `core_content` | `LONGTEXT NOT NULL` | 核心内容 |
| `routine_work` | `LONGTEXT NOT NULL` | 日常工作 |
| `self_score` | `TINYINT UNSIGNED NOT NULL` | 0 到 100 |
| `generated_at` | `DATETIME(6) NOT NULL` | 最近生成时间 |
| `created_at` | `DATETIME(6) NOT NULL` | 创建时间 |
| `updated_at` | `DATETIME(6) NOT NULL` | 更新时间 |

约束与索引：

- 唯一索引：`(user_id, period_type, period_start, period_end)`。
- 查询索引：`(user_id, period_start DESC)`。
- `user_id` 外键引用 `users.id`。
- 应用层和数据库约束共同保证 `self_score` 在 0 到 100 之间。

## 6. 后端模块与接口

### 6.1 模块边界

新增 `weeklyreport` 包，沿用当前认证模块的分层方式：

- `api`：请求校验和响应 DTO。
- `application`：周报用例、DOCX 导入和周期总结。
- `domain`：周报、周期总结、仓储接口和业务规则。
- `infrastructure.persistence`：MyBatis-Plus 数据访问。
- `infrastructure.document`：Apache POI DOCX 文本提取。
- `infrastructure.ai`：OpenAI 兼容 Provider 适配器。

认证模块继续只负责用户身份，不直接依赖周报模块。

### 6.2 周报接口

```text
POST /api/weekly-reports
GET  /api/weekly-reports?year=2026&month=8
GET  /api/weekly-reports/{id}
PUT  /api/weekly-reports/{id}
POST /api/weekly-reports/import-docx
```

`import-docx` 使用 `multipart/form-data`，只返回填充建议，不直接创建或更新周报。

### 6.3 周期总结接口

```text
POST /api/work-summaries/generate
GET  /api/work-summaries?periodType=QUARTER&year=2026&quarter=3
GET  /api/work-summaries?periodType=YEAR&year=2026
PUT  /api/work-summaries/{id}
```

生成请求按季度或年份计算日期边界，客户端不直接提交任意开始和结束日期。

### 6.4 用户隔离

- Controller 从 `@AuthenticationPrincipal AuthenticatedUser` 取得用户 ID。
- Repository 查询和更新条件必须同时包含业务 ID 与用户 ID。
- 访问其他用户资源统一表现为资源不存在，不泄露资源是否真实存在。

## 7. 模型 Provider

业务层依赖 `ChatProvider` 接口，Provider 适配器通过配置读取：

- Base URL。
- API Key。
- Model Name。
- 请求超时时间。

周报业务负责提示词、结构定义、JSON 解析和业务校验；Provider 只负责发送一次请求并返回结果。首期使用 Spring `RestClient`，不引入 Spring AI，不自动切换备用模型，不进行无限重试。

模型调用采用同步接口，前端显示加载状态。单次调用设置明确超时；周报数据量较小，不引入异步任务表、Redis 或消息队列。课程长录音后续单独设计异步任务机制。

测试通过 Fake Provider 返回固定结果，不消耗真实模型额度；完整联调阶段在本地配置存在时增加一次真实 Provider 冒烟测试，任何密钥都不得写入测试代码、文档或 Git。

## 8. 前端页面

### 8.1 真实工作台

- 将已确认的 `/preview` 布局迁移至受保护的 `/`。
- 头像、名称、邮箱来自认证 Store。
- 周报入口跳转 `/weekly-reports`。
- 最近整理在周报接口完成后替换为真实数据。

### 8.2 周报页面

`/weekly-reports` 包含：

- 当前周报编辑区。
- 三个明确标注的多行输入框。
- DOCX 导入按钮和导入加载状态。
- 保存按钮。
- 年份、月份筛选器。
- 历史周报列表。

`本周核心工作` 空白时禁止保存；另外两个输入框允许为空。

### 8.3 周报详情

`/weekly-reports/:id` 展示完整字段并允许进入编辑状态。用户无权访问的 ID 显示统一的“周报不存在”。

### 8.4 周期总结

`/work-summaries` 提供季度和年度两个切换项，分别显示适用的年份、季度选择器。生成结果使用三个卡片或三个分区展示核心内容、日常工作和自我评分，并允许编辑保存。

页面继续使用已确认的 Element Plus、暖白背景、黑灰文字、橙黄点缀和圆角组件。

## 9. 错误处理

只处理主流程中明确会出现的错误：

- 核心工作为空：返回校验错误。
- 同一周重复创建：返回周报已存在，并由前端引导打开已有周报。
- 年月或季度参数非法：返回校验错误。
- DOCX 类型或大小不符合要求：返回明确提示。
- DOCX 无可提取文字：提示用户改为手动填写。
- 模型失败：DOCX 内容回填核心工作；总结生成则保留旧总结并提示稍后重试。
- 未登录或令牌失效：沿用现有认证错误处理。
- 访问其他用户数据：返回资源不存在。

不建立通用重试框架和复杂错误恢复流程。

## 10. 测试与验收

### 10.1 前后端联调验收

- 注册新测试用户。
- 分别使用用户名和邮箱登录。
- 工作台显示真实用户信息。
- 刷新页面保持登录。
- Refresh Token 只轮换一次。
- 退出后受保护页面不可访问。
- 前端测试、生产构建和后端测试全部通过。

### 10.2 Tomcat 验收

- Vue 生产资源进入 WAR。
- WAR 可在 Tomcat 10.1 根上下文启动。
- 直接访问 `/login` 和周报路由不会出现 404。
- 同域 `/api` 请求成功。
- Flyway 正常连接 MySQL `work` 并完成迁移。
- WAR 运行期间不启动另一个占用 8080 的后端进程。

### 10.3 周报后端验收

- 核心工作必填，问题和计划允许为空。
- 同一用户同一周不能重复创建。
- 不同用户可以保存同一周的独立周报。
- 年月筛选只返回当前用户和对应日期范围的数据。
- 详情和更新不能越权。
- DOCX 段落及表格文字可以提取。
- 不确定内容进入核心工作。

### 10.4 周期总结验收

- 季度日期范围正确。
- 年度日期范围正确。
- 只读取当前用户周报。
- 输出包含核心内容、日常工作和 0 到 100 的评分。
- 没有周报时不调用 Provider。
- 重新生成覆盖同周期旧总结。
- 用户可以修改并保存总结与评分。

### 10.5 浏览器验收主流程

```text
注册/登录
  → 进入真实工作台
  → 新建三栏周报
  → 上传 DOCX 并检查自动填充
  → 保存
  → 按年份和月份查询
  → 查看详情并修改
  → 生成季度总结
  → 生成年度总结
  → 退出登录
```

## 11. 实施拆分与检查点

设计批准后编写两份独立实施计划：

1. `frontend-backend-tomcat-integration`：保护当前 UI、真实认证联调、工作台迁移、WAR 打包与 Tomcat 验收。
2. `structured-weekly-report`：数据库、周报 API、DOCX 导入、Provider、前端页面、历史查询、季度年度总结与最终文档。

第一个计划完成并给出测试证据后，才开始第二个计划。

每个可验收增量单独提交，推送远程前向用户汇报。

## 12. 明确不在本次范围内

- Redis 与分布式缓存。
- MinIO 与原始 DOCX 永久存储。
- 飞书机器人。
- 课程录音、语音识别和长任务调度。
- 文献助手。
- OCR 和 `.doc` 文件解析。
- 周报聊天消息、素材消息表和对话历史。
- 周报审批流、复杂版本树和绩效考核系统。
- 多模型自动故障切换、自动重试链和并行模型调用。
- 云部署与多实例运行。

## 13. 官方兼容性依据

- Spring Boot 3.5.16 系统要求：<https://docs.spring.io/spring-boot/3.5/system-requirements.html>
- Spring Boot 传统 WAR 部署：<https://docs.spring.io/spring-boot/how-to/deployment/traditional-deployment.html>
- Tomcat 10.1 文档：<https://tomcat.apache.org/tomcat-10.1-doc/>
- IntelliJ IDEA Tomcat 配置：<https://www.jetbrains.com/help/idea/run-debug-configuration-tomcat-server.html>
