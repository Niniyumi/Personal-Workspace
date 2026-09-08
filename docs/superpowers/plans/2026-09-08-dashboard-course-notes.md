# Dashboard and Course Notes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the static home screen with a real personal dashboard and split course transcription from user-triggered AI note generation while retaining audio, transcript, and notes.

**Architecture:** Add one lightweight dashboard query service that aggregates existing user-owned reports and courses in memory. Reuse the existing segmented audio storage and course table, add only a `TRANSCRIBED` state, expose authenticated audio reads, and separate ASR from note generation.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus, MySQL/Flyway, Vue 3, Pinia, Element Plus, Vitest.

**Spec:** `docs/superpowers/specs/2026-09-08-dashboard-course-notes-design.md`

## Global Constraints

- Do not add chart, queue, cache, retry, or analytics dependencies.
- Do not rewrite existing database tables or delete existing user data.
- Retain original audio, full transcript, and generated note.
- Automated tests must mock external speech and chat providers.
- Keep pages concise and remove inactive or development-only UI.

---

### Task 1: Persist transcription before note generation

**Files:**
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/domain/CourseStatus.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/application/CourseProcessingService.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/application/CourseService.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/api/CourseController.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/application/CourseProcessingServiceTest.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/application/CourseServiceTest.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/api/CourseControllerTest.java`

**Interfaces:**
- Produces: `CourseStatus.TRANSCRIBED`.
- Produces: `CourseService.beginNoteGeneration(long userId, long courseId): Course`.
- Produces: `CourseProcessingService.generateNoteAsync(long userId, long courseId): void`.
- Produces: `POST /api/courses/{courseId}/note/generate` returning HTTP 202.

- [ ] Write a failing processing test proving transcription saves `transcript`, leaves `noteContent` null, sets `TRANSCRIBED`, and does not call `ChatProvider` or delete audio.
- [ ] Run the targeted backend test and verify it fails because the current service immediately generates a note and deletes audio.
- [ ] Add `TRANSCRIBED` and split `process` into transcription and note-generation operations.
- [ ] Run the targeted test and verify it passes.
- [ ] Write failing tests proving note generation requires `TRANSCRIBED`, success retains transcript, and AI failure returns to `TRANSCRIBED` with transcript intact.
- [ ] Add `beginNoteGeneration` and the authenticated generation endpoint, then run targeted tests to green.

### Task 2: Retain and securely expose original audio

**Files:**
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/infrastructure/storage/CourseAudioStorage.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/infrastructure/storage/LocalCourseAudioStorage.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/api/CourseController.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/course/domain/CourseAudioPartRepository.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/api/CourseControllerTest.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/course/infrastructure/storage/LocalCourseAudioStorageTest.java`

**Interfaces:**
- Produces: `CourseAudioStorage.read(Path path): byte[]`.
- Produces: `GET /api/courses/{courseId}/parts`.
- Produces: `GET /api/courses/{courseId}/parts/{partNumber}/audio` with `audio/webm`.

- [ ] Write failing tests for ordered part metadata and for reading audio only after course ownership has been checked.
- [ ] Run targeted tests and verify the new endpoints/storage method are missing.
- [ ] Implement storage reading and controller responses using existing course-scoped repository lookups.
- [ ] Run targeted tests to green and verify audio cleanup is absent from successful processing.

### Task 3: Add dashboard aggregation API

**Files:**
- Create: `backend/src/main/java/com/niniyumi/personalagent/dashboard/application/DashboardService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/dashboard/api/DashboardController.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/dashboard/api/dto/DashboardResponse.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/domain/WeeklyReportRepository.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/MybatisWeeklyReportRepository.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/dashboard/application/DashboardServiceTest.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/dashboard/api/DashboardControllerTest.java`

**Interfaces:**
- Produces: `GET /api/dashboard` with total counts, current-month counts, six monthly activity points, total recording seconds, and recent items.
- Produces: `WeeklyReportRepository.findAllByUserId(long userId): List<WeeklyReport>`.

- [ ] Write a failing service test using literal report/course dates and expected totals for one current user.
- [ ] Run the test and verify failure due to missing dashboard types.
- [ ] Implement the in-memory aggregation with exactly six chronological month buckets and at most five recent items.
- [ ] Add a controller test proving the authenticated user ID is used and run targeted tests to green.

### Task 4: Implement the real Vue dashboard

**Files:**
- Create: `frontend/src/features/dashboard/types.ts`
- Create: `frontend/src/features/dashboard/dashboardApi.ts`
- Create: `frontend/src/features/dashboard/dashboardStore.ts`
- Create: `frontend/src/components/ActivityChart.vue`
- Modify: `frontend/src/views/HomeView.vue`
- Test: `frontend/src/features/dashboard/dashboardApi.test.ts`
- Test: `frontend/src/features/dashboard/dashboardStore.test.ts`
- Test: `frontend/src/views/AuthViews.test.ts`

**Interfaces:**
- Consumes: `GET /api/dashboard` response from Task 3.
- Produces: real totals, current-month overview, six-month chart, recent links, loading/error/empty states.

- [ ] Write failing API/store tests for authenticated loading and a failing home-view test proving static phase content and disabled controls are gone.
- [ ] Run the three targeted frontend test files and verify the expected failures.
- [ ] Implement API/store types and render a native CSS/SVG chart with visible numeric labels.
- [ ] Replace the static home cards, preserve navigation/logout, and run targeted tests to green.

### Task 5: Implement the staged course UI

**Files:**
- Modify: `frontend/src/features/course/types.ts`
- Modify: `frontend/src/features/course/courseApi.ts`
- Modify: `frontend/src/features/course/courseStore.ts`
- Modify: `frontend/src/views/CoursesView.vue`
- Modify: `frontend/src/views/CourseDetailView.vue`
- Test: `frontend/src/features/course/courseApi.test.ts`
- Test: `frontend/src/features/course/courseStore.test.ts`
- Test: `frontend/src/views/CoursesView.test.ts`
- Test: `frontend/src/views/CourseDetailView.test.ts`

**Interfaces:**
- Consumes: `TRANSCRIBED`, generation endpoint, audio-part list, and audio blobs from Tasks 1-2.
- Produces: audio playback/download, transcription status, explicit “生成笔记”, and preserved editable notes.

- [ ] Write failing tests for the `TRANSCRIBED` button state, explicit note generation, and authenticated audio retrieval.
- [ ] Run targeted tests and verify failure because these interfaces do not exist.
- [ ] Add API/store methods, blob URL lifecycle cleanup, and concise phase labels.
- [ ] Update list/detail pages and run targeted tests to green.

### Task 6: Remove stale and repetitive page content

**Files:**
- Modify: `frontend/src/components/AuthShell.vue`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/views/RegisterView.vue`
- Modify: `frontend/src/views/ForgotPasswordView.vue`
- Modify: `frontend/src/views/WeeklyReportsView.vue`
- Modify: `frontend/src/views/WeeklyReportDetailView.vue`
- Modify: `frontend/src/views/WorkSummariesView.vue`
- Modify: `frontend/src/router/index.ts`
- Test: existing view and router test files under `frontend/src/views` and `frontend/src/router`.

**Interfaces:**
- Produces: function-first Chinese page headings and lazy-loaded routes.

- [ ] Update affected tests only where a user-visible action or route behavior changes; do not add tests that merely freeze prose.
- [ ] Simplify headings and descriptions, remove `ISSUE 01`, Java 21 footer, decorative IDs, repeated English labels, and inactive controls.
- [ ] Convert route components to `() => import(...)` without changing route names or guards.
- [ ] Run all frontend tests and production build.

### Task 7: Full verification and handoff

**Files:**
- Modify: `docs/progress/2026-09-08.md`

- [ ] Run backend full tests offline and confirm zero failures.
- [ ] Run frontend full tests and `npm run build` without installing packages.
- [ ] Build the backend WAR offline.
- [ ] Run `git diff --check` and scan staged changes for literal secrets.
- [ ] Record implemented behavior, automated results, and the remaining real microphone/DashScope acceptance step in the progress report.
- [ ] Review the final diff against every acceptance criterion in the spec before offering commit/push options.

