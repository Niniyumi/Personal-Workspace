# Course Recording and Notes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让登录用户在网页录制最多 90 分钟课程，结束后异步转写并生成可编辑课程笔记。

**Architecture:** Vue 使用 `MediaRecorder` 生成独立 WebM 分片并串行上传；Spring Boot 将分片存入本地目录，使用轻量线程池在结束后逐片转写，再复用 `ChatProvider` 生成 Markdown 笔记。MySQL 保存课程状态、转写与笔记，前端轮询处理状态。

**Tech Stack:** Java 21、Spring Boot 3.5、MyBatis-Plus、MySQL/Flyway、Vue 3、Pinia、Element Plus、Vitest

**Spec:** `docs/superpowers/specs/2026-08-24-course-recording-notes-design.md`

## Global Constraints

- 仅支持桌面 Chrome/Edge，最长录音 5400 秒。
- 录音期间只上传分片，结束后统一转写，不实现实时字幕。
- 使用本地文件、Spring 异步任务和前端 5 秒轮询；不引入 Redis、消息队列、MinIO 或 SSE。
- 所有课程访问按 JWT 当前用户隔离。
- Provider 不配置时任务进入 `FAILED` 并保留音频，允许手动重试。

---

### Task 1: 课程数据模型与持久化

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__create_course_tables.sql`
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/domain/Course*.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/infrastructure/persistence/*.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/application/CourseServiceTest.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/infrastructure/persistence/CourseMigrationContractTest.java`

**Interfaces:**
- Produces: `CourseService.create/list/get/saveNote`、`CourseRepository`、`CourseAudioPartRepository`。

- [ ] 先写失败测试：用户只能查询自己的课程；标题必填；笔记只能在自己的课程上保存。
- [ ] 运行目标测试并确认因课程类尚不存在而失败。
- [ ] 创建 `CourseStatus`、`Course`、`CourseAudioPart`、Repository 与 MyBatis-Plus 实现。
- [ ] 创建 V3 表结构和唯一索引 `(course_id, part_number)`。
- [ ] 运行课程 Service 与迁移测试并转绿。

### Task 2: 分片存储与课程处理

**Files:**
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/application/CourseProcessingService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/infrastructure/storage/LocalCourseAudioStorage.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/infrastructure/speech/SpeechProvider*.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/infrastructure/config/CourseProperties.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/PersonalAgentApplication.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/application/CourseProcessingServiceTest.java`

**Interfaces:**
- Consumes: `CourseRepository`、`CourseAudioPartRepository`、现有 `ChatProvider`。
- Produces: `CourseService.uploadPart/complete/retry`、`SpeechProvider.transcribe(Path)`。

- [ ] 先写失败测试：分片幂等、编号连续、总时长限制、成功清理文件、失败保留文件。
- [ ] 运行目标测试并确认缺少处理实现。
- [ ] 实现安全本地路径、串行分片转写、Markdown 笔记提示词与状态变化。
- [ ] 实现 OpenAI 兼容 `/audio/transcriptions` Provider 和单一异步执行器。
- [ ] 运行处理测试并转绿。

### Task 3: 认证课程 REST API

**Files:**
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/api/CourseController.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/course/api/dto/*.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/security/SecurityConfig.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/api/CourseControllerTest.java`

**Interfaces:**
- Produces: `POST /api/courses`、`POST /{id}/parts|complete|retry`、`GET /api/courses|/{id}`、`PUT /{id}/note`。

- [ ] 先写失败 MockMvc 测试，覆盖创建、上传、完成、列表、详情、重试、保存笔记与未登录拒绝。
- [ ] 运行测试并确认接口不存在。
- [ ] 实现 DTO、Controller 和稳定错误码。
- [ ] 运行课程 Controller 测试与后端完整测试。

### Task 4: 前端课程数据层与录音控制

**Files:**
- Create: `frontend/src/features/course/types.ts`
- Create: `frontend/src/features/course/courseApi.ts`
- Create: `frontend/src/features/course/courseStore.ts`
- Create: `frontend/src/features/course/useCourseRecorder.ts`
- Test: `frontend/src/features/course/courseApi.test.ts`
- Test: `frontend/src/features/course/courseStore.test.ts`
- Test: `frontend/src/features/course/useCourseRecorder.test.ts`

**Interfaces:**
- Produces: 课程 API、状态 Store，以及 `start/pause/resume/stop` 录音组合函数。

- [ ] 先写失败测试：multipart 字段、轮询终止、分片串行上传、结束后调用 complete、90 分钟自动停止。
- [ ] 运行测试并确认模块不存在。
- [ ] 实现最小 API、Store 与 MediaRecorder 包装。
- [ ] 运行目标测试并转绿。

### Task 5: 课程工作台、详情页与导航

**Files:**
- Create: `frontend/src/views/CoursesView.vue`
- Create: `frontend/src/views/CourseDetailView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/common/web/SpaForwardController.java`
- Test: `frontend/src/views/CoursesView.test.ts`
- Test: `frontend/src/views/CourseDetailView.test.ts`
- Test: `frontend/src/router/router.test.ts`

**Interfaces:**
- Consumes: `useCourseStore`、`useCourseRecorder`。
- Produces: `/courses` 与 `/courses/:id` 两个受保护页面。

- [ ] 先写失败测试：路由保护、录音按钮状态、历史列表、处理状态、笔记编辑保存和失败重试。
- [ ] 运行测试并确认页面/路由缺失。
- [ ] 按现有暖灰、圆角、橙黄强调风格实现两个响应式页面和首页入口。
- [ ] 运行前端完整测试与生产构建。
- [ ] 浏览器冒烟检查无横向滚动、按钮可达且状态反馈清楚。

### Task 6: 配置、说明与最终验证

**Files:**
- Modify: `.env.example`
- Modify: `README.md`
- Create: `docs/testing/course-recording-test-cases.md`
- Modify: `docs/progress/2026-08-24.md`

- [ ] 补充 `COURSE_STORAGE_DIR`、`SPEECH_*` 配置和本地测试说明。
- [ ] 运行后端完整测试、前端完整测试与构建。
- [ ] 检查 Git diff 只包含课程功能与必要导航改动。
- [ ] 更新进度文档并提交当前功能分支。
