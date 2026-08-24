# 课程录音与笔记设计

- 日期：2026-08-24
- 状态：待用户评审
- 目标：在现有多用户 Personal Agent 中增加轻量、可真实演示的课程录音转写与笔记功能

## 1. 范围

首版支持桌面 Chrome 和 Edge。用户输入课程名称后在网页录音，最长 90 分钟；录音期间后台上传音频分片，但不实时转写。用户结束录音后，后端异步完成语音识别和笔记生成，页面显示处理状态及最终结果。

首版不实现实时字幕、说话人识别、移动端与 Safari 适配、Redis、消息队列、MinIO、SSE、音频剪辑、自动重试链和运行时切换 Provider。

## 2. 方案选择

### 采用：后台分片上传，结束后统一处理

浏览器每 5 分钟结束当前 `MediaRecorder` 并立即开启下一段，形成可独立识别的 `audio/webm` 文件。分片按序上传，避免浏览器内存保存整节课，也满足免费语音接口的单文件限制。结束录音后才开始识别，不产生实时字幕相关状态。

### 不采用的方案

- 结束后上传一个完整文件：代码最少，但长时间录音一直占用浏览器内存，且容易超过语音接口单文件限制。
- WebSocket 实时字幕：体验更即时，但需要流式音频、临时与最终字幕合并、重连和 Provider 专用协议，超出轻量首版范围。

## 3. 用户流程

1. 用户进入课程笔记页，输入课程名称并点击开始录音。
2. 浏览器请求麦克风权限，显示计时、暂停和结束按钮。
3. 每 5 分钟产生一个独立 WebM 分片，前端通过单队列按序上传。
4. 录音达到 90 分钟时自动结束；用户也可以提前结束。
5. 前端等待当前分片上传完成，然后调用完成接口。
6. 后端把课程状态从 `RECORDING` 改为 `PROCESSING`，在线程池中依次转写分片。
7. 后端按分片编号拼接转写文本，再调用现有文字模型生成课程笔记。
8. 成功后保存转写和笔记，将状态改为 `READY` 并删除临时音频；失败则改为 `FAILED` 并保留音频。
9. 页面轮询状态。完成后可查看、编辑和保存笔记；失败时可手动重新处理。

## 4. 系统结构

继续使用 Spring Boot 模块化单体，不新增独立服务。

```text
Vue 课程工作台
  ├── MediaRecorder 录音
  ├── 单队列分片上传
  └── 5 秒轮询课程状态
            │ REST
            ▼
Spring Boot course 模块
  ├── 课程与分片接口
  ├── 本地音频文件存储
  ├── Spring 异步处理器
  ├── SpeechProvider（Groq Whisper）
  └── 现有 ChatProvider（课程笔记）
            │
            ├── MySQL：课程、状态、转写和笔记
            └── 本地目录：处理中或失败的音频分片
```

后端新增 `course` 包，保持现有 `api`、`application`、`domain`、`infrastructure` 分层。前端新增 `features/course` 数据模块以及课程列表/录音页和详情页。

## 5. 数据设计

Flyway V3 新增两张表。

### `courses`

- `id`
- `user_id`
- `title`
- `status`：`RECORDING`、`PROCESSING`、`READY`、`FAILED`
- `duration_seconds`，最大 5400
- `transcript`，处理完成前为空
- `note_content`，使用可编辑 Markdown 文本
- `error_message`，只保存适合用户理解的简短错误
- `created_at`、`updated_at`

### `course_audio_parts`

- `id`
- `course_id`
- `part_number`，从 1 开始
- `duration_seconds`
- `storage_path`，服务端生成，不使用原始文件名
- `file_size`
- `created_at`

`(course_id, part_number)` 建立唯一索引。所有课程查询都同时使用课程 ID 和当前用户 ID；分片必须通过归属课程间接校验用户。

## 6. 文件存储

音频保存在配置项 `COURSE_STORAGE_DIR` 指定的本地目录，默认使用项目运行目录下的 `data/course-audio`。路径由服务端按照用户和课程生成随机目录，客户端不能传入路径。

成功生成笔记后删除该课程的音频文件和分片记录。处理失败时保留分片，供手动重新处理；重新处理成功后再删除。首版不提供音频播放和永久归档。

## 7. API

所有接口要求登录，并从 JWT 获取当前用户。

```text
POST /api/courses                         创建课程
POST /api/courses/{id}/parts              上传一个分片
POST /api/courses/{id}/complete           结束录音并开始处理
POST /api/courses/{id}/retry              重新处理失败课程
GET  /api/courses                         查询当前用户课程列表
GET  /api/courses/{id}                    查询状态、转写和笔记
PUT  /api/courses/{id}/note               保存用户修改后的笔记
```

上传接口接收 `partNumber`、`durationSeconds` 和 `file`。完成接口校验分片编号连续、总时长不超过 5400 秒且课程仍为 `RECORDING`。重复分片返回已有结果，不重复写文件。

## 8. Provider

新增独立 `SpeechProvider`：

```java
String transcribe(Path audioFile);
```

首个实现调用 OpenAI 兼容的 `/audio/transcriptions`，开发期使用 Groq `whisper-large-v3-turbo`。语音 Provider 使用独立配置，避免与文字总结模型互相绑定：

```yaml
app:
  speech:
    base-url: ${SPEECH_BASE_URL:https://api.groq.com/openai/v1}
    api-key: ${SPEECH_API_KEY:}
    model: ${SPEECH_MODEL:whisper-large-v3-turbo}
    timeout-seconds: ${SPEECH_TIMEOUT_SECONDS:120}
```

课程笔记继续复用现有 `ChatProvider`。提示词要求返回 Markdown，固定包含课程摘要、核心知识点、重要概念、示例或案例、复习提纲。

## 9. 异步处理与错误

使用 Spring `ThreadPoolTaskExecutor`，不引入外部队列。完成接口只创建处理任务并立即返回。前端每 5 秒读取课程详情，直到状态变为 `READY` 或 `FAILED`。

- 麦克风不允许或浏览器不支持：前端直接提示，不创建无效录音。
- 分片上传失败：暂停结束流程，保留当前 Blob，并提供再次上传按钮。
- Provider 未配置、超时或返回空文本：课程标记为 `FAILED`，保留分片，页面显示可重新处理。
- 笔记生成失败：同样标记为 `FAILED`，不保存不完整结果。
- 不做自动重试；用户只通过明确的重新处理按钮触发。

## 10. 页面

- `/courses`：创建课程、录音控制、上传进度和历史课程列表。
- `/courses/:id`：状态、完整转写、可编辑笔记和重新处理按钮。
- 首页课程卡片与侧边栏跳转到 `/courses`。

视觉沿用现有暖灰背景、白色圆角卡片、橙色主强调和黄色状态色，不引入新的 UI 依赖。

## 11. 测试与验收

后端自动化测试使用临时目录和 Fake Provider，覆盖用户隔离、分片顺序与幂等、90 分钟限制、状态变化、失败保留文件、成功清理文件、笔记保存和接口鉴权。

前端测试覆盖录音状态、分片上传队列、结束后触发处理、状态轮询、结果展示和错误提示。浏览器麦克风与真实 Groq 调用作为手工冒烟测试，不在自动化测试中伪造通过。

首版验收条件：一个登录用户能够完成一次短录音，看到 `RECORDING → PROCESSING → READY`，查看完整转写并编辑保存课程笔记；另一用户不能访问该课程。
