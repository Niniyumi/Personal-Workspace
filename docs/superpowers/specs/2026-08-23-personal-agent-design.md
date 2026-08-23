# Personal Agent 系统设计

- 日期：2026-08-23
- 状态：待用户评审
- 首期目标：完成一个真实可用、适合 Java 后端面试展示的个人 AI Agent

## 1. 项目定位

Personal Agent 是一个多用户网页应用。用户通过统一的 Agent 对话入口调用专业工具，完成课程录音整理和周报生成。

首期产品包含两个核心工具：

1. 课程工具：录制最长两小时的课程音频，上传后异步转写，并生成结构化课程笔记。
2. 周报工具：通过对话持续提交文字和 DOCX 文件，按需生成、修改、保存和导出新周报。

第二阶段增加飞书机器人入口，第三阶段增加文献助手。后续入口和工具复用首期后端，不在首期提前实现相应代码和数据表。

## 2. 工程原则

### 2.1 必须遵守

- 遵循 YAGNI，只实现已确认的主流程。
- 优先保证响应速度、代码可读性和本地可运行性。
- 长任务异步执行，同步请求快速返回任务 ID。
- 模块之间通过明确接口协作，业务代码不直接依赖模型厂商。
- 用户、文件、任务和成果必须按用户隔离。
- 密码、数据库口令和模型密钥不写入源码或提交到版本控制。

### 2.2 首期明确不做

- 微服务、服务注册、API 网关和分布式事务。
- Redis、RabbitMQ、向量数据库和工作流引擎。
- 多模型自动路由、自动探活和多级故障转移。
- 文献解析、RAG、联网检索和飞书机器人。
- 多人协作编辑、管理员后台和复杂角色权限。
- 邮箱验证、找回密码和第三方登录。
- 每收到一条周报素材就重新生成整份周报。
- 为罕见异常预先设计大量分支和补偿流程。

## 3. 已核实的本地环境

- 操作系统：Windows
- `JAVA_HOME`：`C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\`
- Java：Eclipse Temurin 21.0.11 LTS
- MySQL 服务：`MySQL80`
- MySQL：8.0.30
- 数据库：`work`
- 数据库连接：已使用本地 `root` 用户验证成功

数据库密码仅通过环境变量提供，不记录在本文档中。

## 4. 总体架构

系统采用前后端分离的模块化单体架构。

```text
Vue 3 Web
   │ REST / SSE
   ▼
Spring Boot 模块化单体
   ├── auth            注册、登录、令牌刷新和用户隔离
   ├── agent           会话、消息、意图识别和工具调用
   ├── course          课程、音频分片、转写和课程笔记
   ├── weeklyreport    周报会话、素材、草稿和 DOCX 导出
   ├── task            异步任务状态、执行和进度推送
   ├── file            文件元数据和 MinIO 访问
   ├── aiprovider      对话模型和语音识别 Provider
   └── common          统一错误、时间和基础类型
        │
        ├── MySQL 8
        ├── MinIO
        └── 通义千问 / 阿里云语音 API
```

首期只部署一个 Spring Boot 实例。模块边界用于控制代码依赖，不增加独立服务和网络调用。

## 5. 技术栈

### 5.1 后端

- Java 21
- Spring Boot 3
- Spring MVC
- Spring Security
- JWT Access Token 与 Refresh Token
- MyBatis-Plus
- Flyway
- MySQL 8
- Apache POI
- MinIO Java SDK
- Spring `ThreadPoolTaskExecutor`
- SSE
- Maven

### 5.2 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- 浏览器 `MediaRecorder` API

### 5.3 测试

- JUnit 5
- Spring Boot Test
- Testcontainers MySQL
- MockWebServer，用于模拟模型服务
- Vitest
- 一组覆盖核心主流程的端到端测试

## 6. 代码结构

```text
personal-agent/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/personalagent/
│       │   ├── auth/
│       │   ├── agent/
│       │   ├── course/
│       │   ├── weeklyreport/
│       │   ├── task/
│       │   ├── file/
│       │   ├── aiprovider/
│       │   └── common/
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       └── test/
├── frontend/
├── deploy/
│   └── docker-compose.yml
├── docs/
└── README.md
```

每个后端业务模块内部采用 `api`、`application`、`domain`、`infrastructure` 四个包。仅在一个模块确实拥有相应职责时创建对应包，不为保持形式创建空层。

## 7. 用户与认证

### 7.1 注册与登录

- 用户使用用户名、邮箱和密码注册。
- 用户名与邮箱分别唯一。
- 密码使用 BCrypt 保存。
- 登录成功返回短期 Access Token 和长期 Refresh Token。
- Refresh Token 只以哈希摘要形式保存在 MySQL。
- 退出登录时撤销当前 Refresh Session。

### 7.2 数据隔离

- 所有业务表均直接或间接关联 `user_id`。
- 查询、修改、下载和重试操作必须同时校验资源 ID 与当前用户 ID。
- MinIO 对象不公开访问；下载通过鉴权接口生成短时签名地址。

## 8. Agent 设计

### 8.1 交互形式

首页以 Agent 对话为中心，左侧提供课程、周报和成果工作台，右侧显示正在处理的任务和最近成果。

### 8.2 Agent 职责

- 保存用户与助手消息。
- 识别普通对话、记录周报素材、生成周报和整理课程等意图。
- 调用注册的课程或周报工具。
- 返回任务状态、成果摘要和工作台入口。

### 8.3 Agent 限制

- Agent 不直接访问数据库或对象存储。
- Agent 只能调用显式注册的工具。
- 长任务由工具创建异步任务，Agent 请求不等待任务完成。
- 工作台中的明确按钮直接调用领域 API，不经过模型意图识别。

### 8.4 首期工具

```text
add_weekly_material(text, fileIds)
generate_weekly_report(reportId)
get_task_status(taskId)
get_course_result(courseId)
```

课程录音和分片上传由专业工作台完成。Agent 可以创建课程任务和查询结果，但不在聊天消息中承载两小时录音数据。

## 9. 课程工具

### 9.1 用户流程

1. 用户创建课程记录并输入课程名称。
2. 浏览器请求麦克风权限并开始录音。
3. `MediaRecorder` 每 30 秒产生一个音频分片。
4. 前端按顺序上传分片；服务端以课程 ID 和分片序号保证幂等。
5. 用户结束录音后，前端调用完成接口。
6. 服务端确认分片连续，组合对象并创建转写任务。
7. 转写完成后创建笔记生成任务。
8. 服务端保存原始转写和 Markdown 课程笔记。
9. 用户在课程工作台查看、修改和导出笔记。

### 9.2 分片策略

- 单个分片时长固定为 30 秒。
- 前端同一时刻只上传少量分片，避免录音时间增长导致浏览器内存持续增加。
- 分片上传成功后，前端释放已确认分片的 Blob 引用。
- 数据库对 `(course_id, part_number)` 建立唯一索引，重复上传返回已存在结果。
- 完成录音前校验分片编号连续；缺失分片时提示用户继续上传，不创建转写任务。

课程状态固定为 `RECORDING`、`PROCESSING`、`READY`、`FAILED`。录音分片上传期间保持 `RECORDING`；用户完成录音并成功创建转写任务后进入 `PROCESSING`。

### 9.3 首期客户端与输入限制

- 录音最长两小时，超过后前端自动结束并提交已有分片。
- 单个音频分片最大 10 MB。
- 首期录音只支持桌面版 Chrome 和 Edge 当前稳定版本，并使用浏览器支持的 `audio/webm` 格式。
- 移动浏览器和 Safari 不属于首期支持范围。
- DOCX 单文件最大 20 MB，只接受 `.docx`，不接受旧版 `.doc`。

### 9.4 笔记结构

首期课程笔记使用 Markdown 保存，固定包含：

- 课程摘要
- 核心知识点
- 重要概念与解释
- 示例或案例
- 待复习问题

用户可以直接修改生成结果。保存修改不再次调用模型。

## 10. 周报工具

### 10.1 对话式素材收集

每一周对应一条周报记录和一个关联的 Agent 会话。用户可以在会话中多次发送：

- 纯文字工作记录
- 文字加 DOCX 附件
- 仅 DOCX 附件

发送后，服务端立即保存消息和周报素材，并返回简短确认。普通素材消息不触发整份周报生成。

### 10.2 生成与更新

1. 用户点击“生成/更新周报”或发送明确指令。
2. 服务端读取当前周报的文字素材、DOCX 提取文本和现有草稿。
3. 服务端创建异步生成任务并立即返回任务 ID。
4. Chat Provider 输出固定结构的 Markdown 周报。
5. 新结果覆盖当前草稿，并记录最后生成时间。
6. 用户可以在线编辑草稿，编辑保存不调用模型。
7. 用户继续新增素材后，可主动再次生成更新版本。
8. 用户确认后保存最终内容并导出 DOCX。

### 10.3 周报结构

- 本周工作概述
- 已完成事项
- 进行中事项
- 问题与风险
- 下周计划

首期只保存当前草稿和最终内容，不实现复杂版本树。历史周报以不同周次的周报记录长期保存。

## 11. 文件存储

### 11.1 MySQL 保存

- 原始文件名
- MIME 类型
- 文件大小
- MinIO Bucket 与 Object Key
- 文件用途
- 所属用户
- 创建时间

### 11.2 MinIO 保存

- 课程音频分片和组合后的音频
- 用户上传的 DOCX
- 导出的课程笔记与周报 DOCX

Object Key 使用服务端生成的不可预测值，不使用用户原始文件名作为路径。

## 12. AI Provider

### 12.1 接口

```java
public interface ChatModelProvider {
    ChatResult generate(ChatRequest request);
}

public interface SpeechToTextProvider {
    TranscriptionResult transcribe(TranscriptionRequest request);
}
```

首个实现使用通义千问和阿里云语音服务。OpenAI 实现作为后续可选适配器，业务模块只依赖接口。

### 12.2 配置

```yaml
app:
  ai:
    chat-provider: qwen
    speech-provider: aliyun
```

首期由配置明确选择 Provider。切换配置后重启服务生效，不实现运行时动态路由。

### 12.3 调用记录

每次模型调用记录：

- Provider 与模型名称
- 业务类型和业务 ID
- 开始时间与耗时
- Token 用量；Provider 未返回时为空
- 成功或失败状态
- 错误码，不保存密钥

课程全文和周报全文不重复写入调用日志。

## 13. 异步任务

### 13.1 状态机

```text
QUEUED → RUNNING → SUCCEEDED
                 └→ FAILED
```

### 13.2 执行方式

- 业务事务先在 MySQL 创建 `QUEUED` 任务。
- 事务提交后将任务提交到有界 `ThreadPoolTaskExecutor`。
- Worker 通过条件更新把任务从 `QUEUED` 改为 `RUNNING`，避免重复领取。
- 有界线程池限制同时执行的模型任务数量，防止大量长任务耗尽应用线程。
- 定时恢复器仅扫描遗留的 `QUEUED` 任务并重新提交。
- 应用异常退出时遗留的 `RUNNING` 任务在启动后标记为 `FAILED` 且 `retryable=true`，由用户手动重试。

### 13.3 重试

- 网络超时、HTTP 429 和模型服务 5xx 最多自动重试一次。
- 鉴权失败、参数错误、文件无法解析和内容为空不自动重试。
- 手动重试复用已上传文件并创建新任务，原任务保留失败记录。

### 13.4 进度

- MySQL 是任务状态的唯一事实来源。
- SSE 只推送任务 ID、状态、阶段和整数进度。
- SSE 断开后不补发历史事件；前端重新进入页面时读取任务最新状态。

## 14. MySQL 数据模型

所有表使用 `BIGINT` 主键、`DATETIME(3)` 时间字段和 `utf8mb4` 字符集。

### 14.1 `users`

- `id`
- `username`
- `email`
- `password_hash`
- `display_name`
- `status`
- `created_at`
- `updated_at`

唯一索引：`username`、`email`。

### 14.2 `refresh_sessions`

- `id`
- `user_id`
- `token_hash`
- `expires_at`
- `revoked_at`
- `created_at`

### 14.3 `agent_conversations`

- `id`
- `user_id`
- `title`
- `scene`：`GENERAL`、`WEEKLY_REPORT`、`COURSE`
- `business_id`
- `created_at`
- `updated_at`

`business_id` 在专业场景中指向对应周报或课程，由应用层校验归属关系；首期不增加数据库多态外键。

### 14.4 `agent_messages`

- `id`
- `conversation_id`
- `user_id`
- `role`：`USER`、`ASSISTANT`、`TOOL`
- `content`
- `created_at`

索引：`(conversation_id, id)`。

### 14.5 `stored_files`

- `id`
- `user_id`
- `original_name`
- `content_type`
- `size_bytes`
- `bucket_name`
- `object_key`
- `purpose`
- `created_at`

### 14.6 `processing_tasks`

- `id`
- `user_id`
- `task_type`
- `business_type`
- `business_id`
- `status`
- `stage`
- `progress`
- `attempt_count`
- `error_code`
- `error_message`
- `retryable`
- `started_at`
- `finished_at`
- `created_at`

索引：`(user_id, created_at)`、`(status, created_at)`、`(business_type, business_id)`。

### 14.7 `courses`

- `id`
- `user_id`
- `title`
- `status`
- `combined_audio_file_id`
- `transcript_text`：`LONGTEXT`
- `note_markdown`：`LONGTEXT`
- `recording_started_at`
- `recording_ended_at`
- `created_at`
- `updated_at`

### 14.8 `course_audio_parts`

- `id`
- `course_id`
- `user_id`
- `part_number`
- `file_id`
- `duration_seconds`
- `created_at`

唯一索引：`(course_id, part_number)`。

### 14.9 `weekly_reports`

- `id`
- `user_id`
- `year`
- `week_number`
- `title`
- `status`：`COLLECTING`、`DRAFT`、`FINAL`
- `draft_markdown`：`LONGTEXT`
- `final_markdown`：`LONGTEXT`
- `last_generated_at`
- `created_at`
- `updated_at`

唯一索引：`(user_id, year, week_number)`。

### 14.10 `weekly_report_materials`

- `id`
- `weekly_report_id`
- `user_id`
- `source_type`：`TEXT`、`DOCX`
- `text_content`：`LONGTEXT`
- `file_id`
- `created_at`

TEXT 素材必须包含 `text_content`；DOCX 素材必须包含 `file_id`，解析后的正文保存到 `text_content`。

### 14.11 `ai_call_logs`

- `id`
- `user_id`
- `provider`
- `model`
- `business_type`
- `business_id`
- `status`
- `duration_ms`
- `input_tokens`
- `output_tokens`
- `error_code`
- `created_at`

## 15. API 边界

### 15.1 认证

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/users/me
```

### 15.2 Agent

```text
POST /api/conversations
GET  /api/conversations
GET  /api/conversations/{id}/messages
POST /api/conversations/{id}/messages
```

### 15.3 周报

```text
POST /api/weekly-reports
GET  /api/weekly-reports
GET  /api/weekly-reports/{id}
POST /api/weekly-reports/{id}/materials
POST /api/weekly-reports/{id}/generate
PUT  /api/weekly-reports/{id}/draft
POST /api/weekly-reports/{id}/finalize
GET  /api/weekly-reports/{id}/export
```

### 15.4 课程

```text
POST /api/courses
GET  /api/courses
GET  /api/courses/{id}
PUT  /api/courses/{id}/audio-parts/{partNumber}
POST /api/courses/{id}/recording-complete
PUT  /api/courses/{id}/note
GET  /api/courses/{id}/export
```

### 15.5 任务

```text
GET  /api/tasks/{id}
POST /api/tasks/{id}/retry
GET  /api/tasks/events
```

所有 `{id}` 资源接口均校验当前用户所有权。错误响应使用统一错误码、用户可理解的信息和请求追踪 ID。

## 16. 性能策略

- 登录、查询和纯文字素材保存不调用模型。
- 创建异步任务的接口目标是在 1 秒内返回，不包含文件上传耗时。
- 周报生成只读取当前周报素材和必要历史周报，不扫描用户全部数据。
- 音频上传采用分片，浏览器不长期持有两小时完整音频。
- Agent 消息只加载当前会话的有限最近消息；专业工具直接读取结构化业务数据。
- 有界线程池限制模型并发，避免长任务阻塞普通 Web 请求。
- 首期不增加缓存；出现经过测量的热点查询后再评估 Redis。

## 17. 错误处理

- 请求参数、文件类型、文件大小和资源归属在同步接口中校验。
- DOCX 解析失败时保存明确错误，不调用模型。
- 分片缺失时不创建转写任务，并返回缺失序号。
- Provider 错误转换为内部错误码，前端不显示厂商堆栈或密钥。
- 任务失败不删除原始文件和已保存素材。
- 用户手动重试前检查原始输入是否仍然存在。

## 18. 测试策略

### 18.1 后端单元测试

- 密码和令牌规则
- 用户资源隔离
- 周报素材追加与生成条件
- 音频分片连续性和重复上传
- 任务状态转换与重试判断
- Provider 错误映射

### 18.2 后端集成测试

- 使用 Testcontainers MySQL 执行 Flyway 和 Mapper 测试。
- 使用 MockWebServer 模拟模型成功、超时、429 和 5xx。
- 验证注册登录、创建周报和创建课程任务三条主流程。

### 18.3 前端测试

- 登录状态和令牌刷新
- 录音开始、分片上传和结束状态
- 周报消息与附件提交
- SSE 状态更新和页面重新加载后的任务恢复

首期测试集中在主流程和常见错误，不为极少发生的浏览器或模型异常穷举用例。

## 19. 本地配置与部署

后端通过环境变量读取：

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/work
DB_USERNAME=root
DB_PASSWORD=<local-secret>
JWT_SECRET=<local-secret>
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=<local-secret>
MINIO_SECRET_KEY=<local-secret>
QWEN_API_KEY=<local-secret>
ALIYUN_ASR_API_KEY=<local-secret>
```

本机已有 MySQL，开发阶段直接连接 `work` 数据库。`deploy/docker-compose.yml` 首期只启动 MinIO；README 同时提供使用外部 MySQL 的启动步骤。

Flyway 负责在现有 `work` 数据库中创建和升级业务表，不创建数据库，不删除用户已有数据。首次执行迁移前需要确认 `work` 数据库没有同名业务表。

## 20. 分阶段路线

### 第一阶段：网页 MVP

- 注册登录
- Agent 首页
- 对话式周报
- 课程录音与笔记
- MySQL、MinIO、任务中心和 Provider

### 第二阶段：飞书机器人

- 网站账号与飞书用户绑定
- 接收私聊消息和 DOCX
- 调用首期 Agent 工具
- 使用消息卡片推送任务进度和成果链接

两小时录音仍在网页工作台完成，飞书机器人提供入口和完成通知。

### 第三阶段：文献助手

- PDF 和 DOI 输入
- 文献元数据与结构化笔记
- 多文献对比和带来源问答
- 文献专题知识库

第三阶段开始前单独设计文献数据模型和检索方案，不复用未经验证的首期抽象。

## 21. 首期验收标准

满足以下条件时，首期才算完成：

1. 新用户可以注册、登录和退出，无法访问其他用户资源。
2. 用户可以录制并上传长音频，关闭普通请求不会中断已经创建的后台任务。
3. 用户可以查看转写进度、原始转写和结构化课程笔记。
4. 用户可以在同一周报会话中多次发送文字和 DOCX。
5. 用户可以主动生成、编辑、保存并导出新周报。
6. Agent 首页可以调用周报工具、查询课程结果和展示任务进度。
7. Chat 与 Speech Provider 可以通过配置替换，业务模块不依赖具体厂商类型。
8. 常见失败会给出明确提示，允许重试的任务可以复用原始输入。
9. 核心后端测试、数据库迁移和前端构建通过。
10. README 可以指导开发者在本机 MySQL `work` 数据库上启动项目。
