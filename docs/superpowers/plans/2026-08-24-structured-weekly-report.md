# Structured Weekly Report and Work Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a multi-user weekly-report assistant with three structured fields, DOCX-to-form classification, year/month history lookup, and editable quarterly and yearly AI summaries.

**Architecture:** Store weekly reports and period summaries as user-owned structured MySQL records. Apache POI extracts DOCX text; a business-level `WeeklyReportAiService` builds prompts and validates JSON while a small `ChatProvider` adapter hides the configured OpenAI-compatible model. Vue uses focused Pinia/API modules and Element Plus pages; model calls occur only on explicit import or generate actions.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC, Spring Security, MyBatis-Plus, Flyway, MySQL 8, Apache POI, Spring RestClient, Jackson, Vue 3, TypeScript, Pinia, Element Plus, Axios, Vitest

**Spec:** `docs/superpowers/specs/2026-08-24-structured-weekly-report-and-tomcat-design.md`

## Global Constraints

- Start only after the completion gate in `2026-08-24-frontend-backend-tomcat-integration.md` passes.
- `本周核心工作` is required; `遇到的问题` and `下周工作计划` are optional.
- One weekly report per `(user_id, week_start_date)`; `week_start_date` must be Monday.
- Month membership is derived from `week_start_date`; do not add redundant year or month columns.
- Every read and write is scoped by the authenticated user ID; never accept `userId` from the client.
- Accept `.docx` only, maximum 10 MB; extract paragraphs and tables but do not add OCR.
- Do not permanently store the original DOCX; save only extracted/form content and the source file name selected by the user.
- Uncertain or failed DOCX classification returns all extracted text in `coreWork`.
- Summary generation is explicit and synchronous; no Redis, task queue, automatic generation, provider chain, or infinite retry.
- One OpenAI-compatible Provider is active through configuration; secrets remain outside Git.
- Quarterly and yearly summaries contain `coreContent`, `routineWork`, and editable integer `selfScore` from 0 through 100.
- Follow TDD and commit each independently testable task.

---

### Task 1: Create weekly-report schema, domain, and persistence

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__create_weekly_report_tables.sql`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/WeeklyReportMigrationContractTest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/domain/WeeklyReport.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/domain/WeeklyReportRepository.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/WeeklyReportRow.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/WeeklyReportMapper.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/MybatisWeeklyReportRepository.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/MybatisWeeklyReportRepositoryTest.java`

**Interfaces:**
- Produces: `WeeklyReport` and `WeeklyReportRepository` used by Task 2 and Task 6.
- Repository signatures:

```java
WeeklyReport save(WeeklyReport report);
WeeklyReport update(WeeklyReport report);
Optional<WeeklyReport> findByIdAndUserId(long id, long userId);
Optional<WeeklyReport> findByUserIdAndWeekStartDate(long userId, LocalDate weekStartDate);
List<WeeklyReport> findByUserIdAndWeekStartDateBetween(long userId, LocalDate start, LocalDate end);
```

- [ ] **Step 1: Write the failing migration contract test**

Assert the V2 SQL contains both tables and their ownership/uniqueness constraints:

```java
String sql = Files.readString(Path.of("src/main/resources/db/migration/V2__create_weekly_report_tables.sql"));
assertThat(sql).contains("CREATE TABLE weekly_reports", "CREATE TABLE work_summaries")
        .contains("UNIQUE KEY uk_weekly_reports_user_week")
        .contains("CONSTRAINT fk_weekly_reports_user")
        .contains("CHECK (self_score BETWEEN 0 AND 100)");
```

- [ ] **Step 2: Write failing repository tests**

Mock `WeeklyReportMapper` and capture wrappers to prove ownership and dates are always included:

```java
repository.findByIdAndUserId(9L, 42L);
assertThat(captured.getSqlSegment()).contains("id", "user_id");

repository.findByUserIdAndWeekStartDateBetween(
        42L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
assertThat(captured.getSqlSegment()).contains("user_id", "week_start_date", "ORDER BY updated_at DESC");
```

- [ ] **Step 3: Run the focused tests and verify they fail**

Run:

```powershell
cd backend
./mvnw.cmd -Dtest=WeeklyReportMigrationContractTest,MybatisWeeklyReportRepositoryTest test
```

Expected: FAIL because the migration and weekly-report types do not exist.

- [ ] **Step 4: Create the V2 migration**

Use the exact data shape:

```sql
CREATE TABLE weekly_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    core_work LONGTEXT NOT NULL,
    problems LONGTEXT NULL,
    next_week_plan LONGTEXT NULL,
    source_file_name VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_weekly_reports_user_week (user_id, week_start_date),
    CONSTRAINT fk_weekly_reports_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE work_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    core_content LONGTEXT NOT NULL,
    routine_work LONGTEXT NOT NULL,
    self_score TINYINT UNSIGNED NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_summaries_user_period (user_id, period_type, period_start, period_end),
    CONSTRAINT chk_work_summaries_score CHECK (self_score BETWEEN 0 AND 100),
    CONSTRAINT fk_work_summaries_user FOREIGN KEY (user_id) REFERENCES users (id)
);
```

- [ ] **Step 5: Implement the weekly-report domain contract**

```java
public record WeeklyReport(
        Long id,
        long userId,
        LocalDate weekStartDate,
        String coreWork,
        String problems,
        String nextWeekPlan,
        String sourceFileName,
        Instant createdAt,
        Instant updatedAt) {
}
```

Implement `WeeklyReportRow.fromDomain` and `toDomain`, a MyBatis-Plus mapper, and repository methods that always include `user_id` in lookup/update wrappers.

- [ ] **Step 6: Run focused and migration tests**

Run:

```powershell
./mvnw.cmd -Dtest=WeeklyReportMigrationContractTest,AuthMigrationContractTest,MybatisWeeklyReportRepositoryTest test
```

Expected: PASS.

- [ ] **Step 7: Commit schema and persistence**

```powershell
git add backend/src/main/resources/db/migration/V2__create_weekly_report_tables.sql backend/src/main/java/com/niniyumi/personalagent/weeklyreport/domain backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence backend/src/test/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence
git commit -m "feat: add weekly report persistence"
```

---

### Task 2: Add weekly-report application and authenticated REST API

**Files:**
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/SaveWeeklyReportCommand.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/WeeklyReportService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/WeeklyReportNotFoundException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/WeeklyReportAlreadyExistsException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/InvalidWeekStartException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/WeeklyReportController.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/dto/WeeklyReportRequest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/dto/WeeklyReportResponse.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/application/WeeklyReportServiceTest.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/api/WeeklyReportControllerTest.java`

**Interfaces:**
- Consumes: `WeeklyReportRepository`, `AuthenticatedUser`.
- Produces:

```java
WeeklyReport create(long userId, SaveWeeklyReportCommand command);
WeeklyReport update(long userId, long reportId, SaveWeeklyReportCommand command);
WeeklyReport get(long userId, long reportId);
List<WeeklyReport> list(long userId, int year, int month);
```

- [ ] **Step 1: Write failing service tests**

Cover these exact behaviors:

```java
assertThatThrownBy(() -> service.create(42L, commandWithSunday()))
        .isInstanceOf(InvalidWeekStartException.class);

when(repository.findByUserIdAndWeekStartDate(42L, MONDAY)).thenReturn(Optional.of(existing));
assertThatThrownBy(() -> service.create(42L, validCommand()))
        .isInstanceOf(WeeklyReportAlreadyExistsException.class);

verify(repository).findByUserIdAndWeekStartDateBetween(
        42L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
```

Also verify blank optional text becomes `null`, and updates use `findByIdAndUserId(reportId, userId)`.

- [ ] **Step 2: Write failing controller tests**

Use `@WebMvcTest(WeeklyReportController.class)` with a mocked service and authenticated principal. Verify:

```text
POST /api/weekly-reports                  → 201
GET  /api/weekly-reports?year=2026&month=8 → 200 list
GET  /api/weekly-reports/9                → 200
PUT  /api/weekly-reports/9                → 200
blank coreWork                            → 400 VALIDATION_ERROR
missing principal                         → 401 with filters enabled security test
```

- [ ] **Step 3: Run focused tests and verify they fail**

Run:

```powershell
cd backend
./mvnw.cmd -Dtest=WeeklyReportServiceTest,WeeklyReportControllerTest test
```

Expected: FAIL because service, DTOs, controller, and exception mappings are absent.

- [ ] **Step 4: Implement commands and service rules**

```java
public record SaveWeeklyReportCommand(
        LocalDate weekStartDate,
        String coreWork,
        String problems,
        String nextWeekPlan,
        String sourceFileName) {
}
```

The service must:

```java
if (command.weekStartDate().getDayOfWeek() != DayOfWeek.MONDAY) {
    throw new InvalidWeekStartException();
}
```

Use `YearMonth.of(year, month)` to calculate an inclusive date range. Trim required text and normalize blank optional strings to `null`.

- [ ] **Step 5: Implement authenticated controller and DTOs**

Controller method shape:

```java
@PostMapping
public ResponseEntity<WeeklyReportResponse> create(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody WeeklyReportRequest request) {
    WeeklyReport report = service.create(user.userId(), request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(WeeklyReportResponse.from(report));
}
```

`WeeklyReportRequest.coreWork` uses `@NotBlank`; the controller never reads a user ID from path, query, or request body.

- [ ] **Step 6: Add explicit API error mappings**

Map:

```text
WeeklyReportNotFoundException       → 404 WEEKLY_REPORT_NOT_FOUND
WeeklyReportAlreadyExistsException  → 409 WEEKLY_REPORT_EXISTS
InvalidWeekStartException           → 400 INVALID_WEEK_START
```

- [ ] **Step 7: Run weekly API and full backend tests**

Run:

```powershell
./mvnw.cmd -Dtest=WeeklyReportServiceTest,WeeklyReportControllerTest test
./mvnw.cmd test
```

Expected: PASS.

- [ ] **Step 8: Commit the weekly-report API**

```powershell
git add backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java backend/src/test/java/com/niniyumi/personalagent/weeklyreport/application backend/src/test/java/com/niniyumi/personalagent/weeklyreport/api
git commit -m "feat: add authenticated weekly report api"
```

---

### Task 3: Extract and classify DOCX content

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/document/DocxTextExtractor.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/ai/ChatProvider.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/ai/AiProviderProperties.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/ai/OpenAiCompatibleChatProvider.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/ai/AiProviderException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/DocumentClassification.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/WeeklyReportAiService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/InvalidDocxException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/dto/DocxImportResponse.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/WeeklyReportController.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/infrastructure/document/DocxTextExtractorTest.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/application/WeeklyReportAiServiceTest.java`
- Modify: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/api/WeeklyReportControllerTest.java`

**Interfaces:**
- Produces:

```java
String DocxTextExtractor.extract(MultipartFile file);
String ChatProvider.complete(String systemPrompt, String userPrompt);
DocumentClassification WeeklyReportAiService.classifyDocument(String extractedText);
```

- [ ] **Step 1: Add Apache POI dependency and write failing extraction tests**

Add `org.apache.poi:poi-ooxml` to the Maven dependencies. Build a DOCX in memory in the test containing two paragraphs and one table, then assert:

```java
assertThat(extractor.extract(file))
        .contains("完成登录模块", "修复接口问题", "下周实现周报", "表格内容");
```

Also verify empty, non-DOCX, and files over 10 MB throw `InvalidDocxException`.

- [ ] **Step 2: Write failing AI classification tests**

Use a fake `ChatProvider` lambda:

```java
ChatProvider provider = (system, user) -> """
        {"coreWork":"完成登录","problems":"接口超时","nextWeekPlan":"开发周报"}
        """;
assertThat(service.classifyDocument("raw").coreWork()).isEqualTo("完成登录");
```

When the provider throws or returns invalid JSON, assert all extracted text becomes `coreWork` and the optional fields are `null`.

- [ ] **Step 3: Write failing multipart controller tests**

Verify `POST /api/weekly-reports/import-docx` returns:

```json
{
  "coreWork": "完成登录",
  "problems": "接口超时",
  "nextWeekPlan": "开发周报",
  "sourceFileName": "week-34.docx"
}
```

The endpoint requires authentication and does not call `WeeklyReportService.create` or `update`.

- [ ] **Step 4: Run focused tests and verify they fail**

Run:

```powershell
cd backend
./mvnw.cmd -Dtest=DocxTextExtractorTest,WeeklyReportAiServiceTest,WeeklyReportControllerTest test
```

Expected: FAIL because extractor, provider, and import endpoint are absent.

- [ ] **Step 5: Implement DOCX extraction**

Use `XWPFDocument` and preserve simple reading order:

```java
try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
    List<String> parts = new ArrayList<>();
    document.getParagraphs().stream()
            .map(XWPFParagraph::getText)
            .map(String::trim)
            .filter(text -> !text.isEmpty())
            .forEach(parts::add);
    document.getTables().forEach(table -> table.getRows().forEach(row ->
            row.getTableCells().stream().map(XWPFTableCell::getText)
                    .map(String::trim).filter(text -> !text.isEmpty()).forEach(parts::add)));
    return String.join("\n", parts);
}
```

Reject a wrong extension, wrong OOXML content, empty extracted text, or size above `10 * 1024 * 1024` bytes.

- [ ] **Step 6: Implement the Provider boundary and configuration**

Configuration keys:

```yaml
app:
  ai:
    base-url: ${AI_BASE_URL:https://api.openai.com/v1}
    api-key: ${AI_API_KEY:}
    model: ${AI_MODEL:}
    timeout-seconds: ${AI_TIMEOUT_SECONDS:30}
```

Register `AiProviderProperties` with `@EnableConfigurationProperties`. `OpenAiCompatibleChatProvider` uses `RestClient` to post once to `/chat/completions`, sending `model`, one system message, one user message, and `temperature: 0`. Configure connect and read timeouts on `SimpleClientHttpRequestFactory` from `timeoutSeconds`, then read `choices[0].message.content`. If API key or model is blank, throw `AiProviderException` only when invoked; application startup must still succeed.

- [ ] **Step 7: Implement classification and fallback**

The system prompt requires a JSON object with exactly three string-or-null fields and states that uncertain content belongs in `coreWork`. Parse with Jackson, trim values, and reject a blank `coreWork`. Catch provider and parsing failures only inside `classifyDocument` and return:

```java
new DocumentClassification(extractedText, null, null)
```

- [ ] **Step 8: Implement the authenticated import endpoint and error mapping**

```java
@PostMapping(value = "/import-docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public DocxImportResponse importDocx(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestPart("file") MultipartFile file) {
    String text = extractor.extract(file);
    return DocxImportResponse.from(aiService.classifyDocument(text), file.getOriginalFilename());
}
```

The principal is required by security even though no database row is written. Map invalid files to `400 INVALID_DOCX`.

- [ ] **Step 9: Run focused and full backend tests**

Run:

```powershell
./mvnw.cmd -Dtest=DocxTextExtractorTest,WeeklyReportAiServiceTest,WeeklyReportControllerTest test
./mvnw.cmd test
```

Expected: PASS.

- [ ] **Step 10: Commit DOCX import**

```powershell
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/main/java/com/niniyumi/personalagent/weeklyreport backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java backend/src/test/java/com/niniyumi/personalagent/weeklyreport
git commit -m "feat: classify docx content for weekly reports"
```

---

### Task 4: Add the frontend weekly-report data module

**Files:**
- Create: `frontend/src/features/weeklyReport/types.ts`
- Create: `frontend/src/features/weeklyReport/weeklyReportApi.ts`
- Create: `frontend/src/features/weeklyReport/weeklyReportApi.test.ts`
- Create: `frontend/src/features/weeklyReport/weeklyReportStore.ts`
- Create: `frontend/src/features/weeklyReport/weeklyReportStore.test.ts`

**Interfaces:**
- Consumes: `useAuthStore().tokens?.accessToken` and backend weekly-report endpoints.
- Produces: `useWeeklyReportStore()` actions `loadMonth`, `loadOne`, `create`, `update`, and `importDocx`.

- [ ] **Step 1: Define frontend types and write failing API tests**

Use these types:

```ts
export interface WeeklyReport {
  id: number
  weekStartDate: string
  coreWork: string
  problems: string | null
  nextWeekPlan: string | null
  sourceFileName: string | null
  createdAt: string
  updatedAt: string
}

export interface WeeklyReportInput {
  weekStartDate: string
  coreWork: string
  problems: string
  nextWeekPlan: string
  sourceFileName: string | null
}
```

Use Axios Mock Adapter to verify URL, bearer header, JSON body, and multipart upload for every endpoint.

- [ ] **Step 2: Write failing Pinia store tests**

Mock the API and auth store. Verify:

```ts
await store.loadMonth(2026, 8)
expect(api.list).toHaveBeenCalledWith('access-token', 2026, 8)
expect(store.reports).toEqual([report])

await store.importDocx(file)
expect(store.imported).toEqual(classification)
```

Also verify error text is set and loading returns to false after failure.

- [ ] **Step 3: Run focused tests and verify they fail**

Run:

```powershell
cd frontend
npm test -- --run src/features/weeklyReport/weeklyReportApi.test.ts src/features/weeklyReport/weeklyReportStore.test.ts
```

Expected: FAIL because the weekly-report module does not exist.

- [ ] **Step 4: Implement the API adapter**

Create a small Axios client with `baseURL: '/api'`. Each protected method accepts an access token and sends:

```ts
headers: { Authorization: `Bearer ${accessToken}` }
```

`importDocx` creates `FormData`, appends the file under `file`, and does not set a manual multipart boundary.

- [ ] **Step 5: Implement the Pinia store**

Store state:

```ts
{
  reports: [] as WeeklyReport[],
  current: null as WeeklyReport | null,
  imported: null as DocxImportResult | null,
  loading: false,
  error: null as string | null,
}
```

Use one helper that requires `authStore.tokens?.accessToken`; if missing, set the error to `登录状态已失效，请重新登录` and do not issue a request.

- [ ] **Step 6: Run focused and full frontend tests**

Run:

```powershell
npm test -- --run src/features/weeklyReport/weeklyReportApi.test.ts src/features/weeklyReport/weeklyReportStore.test.ts
npm test -- --run
```

Expected: PASS.

- [ ] **Step 7: Commit the data module**

```powershell
git add frontend/src/features/weeklyReport
git commit -m "feat: add weekly report frontend data module"
```

---

### Task 5: Build weekly-report form, history, and detail pages

**Files:**
- Create: `frontend/src/components/WeeklyReportForm.vue`
- Create: `frontend/src/components/WeeklyReportForm.test.ts`
- Create: `frontend/src/views/WeeklyReportsView.vue`
- Create: `frontend/src/views/WeeklyReportsView.test.ts`
- Create: `frontend/src/views/WeeklyReportDetailView.vue`
- Create: `frontend/src/views/WeeklyReportDetailView.test.ts`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/router/router.test.ts`
- Modify: `frontend/src/styles/main.css`

**Interfaces:**
- Consumes: `useWeeklyReportStore()` and authenticated router guard.
- Produces: protected `/weekly-reports` and `/weekly-reports/:id` pages.

- [ ] **Step 1: Write failing form component tests**

Verify the exact labels and validation:

```ts
expect(wrapper.text()).toContain('本周核心工作')
expect(wrapper.text()).toContain('遇到的问题')
expect(wrapper.text()).toContain('下周工作计划')

await wrapper.get('form').trigger('submit')
expect(wrapper.text()).toContain('请填写本周核心工作')
expect(wrapper.emitted('save')).toBeUndefined()
```

When import data arrives, assert text is appended with one newline and existing input is not overwritten.

- [ ] **Step 2: Write failing page and router tests**

Verify:

- Year/month changes call `loadMonth`.
- History rows render week date, core-work preview, and detail links.
- Detail route calls `loadOne(route.params.id)` and saves with `update`.
- Both routes carry `meta.requiresAuth: true`.
- Home weekly button routes to `weekly-reports`.

- [ ] **Step 3: Run focused tests and verify they fail**

Run:

```powershell
cd frontend
npm test -- --run src/components/WeeklyReportForm.test.ts src/views/WeeklyReportsView.test.ts src/views/WeeklyReportDetailView.test.ts src/router/router.test.ts
```

Expected: FAIL because components and routes are missing.

- [ ] **Step 4: Implement `WeeklyReportForm.vue`**

The form owns an editable copy of `WeeklyReportInput` and emits:

```ts
defineEmits<{
  save: [input: WeeklyReportInput]
  importDocx: [file: File]
}>()
```

Use Element Plus date picker and three textarea inputs. Mark only core work as required. Use `ElUpload` with `:auto-upload="false"`, `:limit="1"`, and accept `.docx`. Show loading state while importing and saving.

- [ ] **Step 5: Implement history and detail pages**

`WeeklyReportsView` initializes year/month from the current date, loads the list, renders the form and history in one responsive layout, and creates a report. On a `WEEKLY_REPORT_EXISTS` error, reload the month instead of creating duplicates.

`WeeklyReportDetailView` loads one report by route ID, passes it to the form, updates it, and shows `周报不存在` for the backend not-found error.

- [ ] **Step 6: Add protected routes and real home navigation**

```ts
{
  path: '/weekly-reports',
  name: 'weekly-reports',
  component: WeeklyReportsView,
  meta: { requiresAuth: true },
},
{
  path: '/weekly-reports/:id',
  name: 'weekly-report-detail',
  component: WeeklyReportDetailView,
  meta: { requiresAuth: true },
}
```

Replace the inactive weekly-report button in `HomeView` with a router link.

- [ ] **Step 7: Apply the approved visual system**

Reuse the existing color tokens and rounded Element Plus styling. Keep the desktop form/history split and collapse to one column below 900 px. Do not add a new UI dependency.

- [ ] **Step 8: Run focused tests, full tests, and build**

Run:

```powershell
npm test -- --run src/components/WeeklyReportForm.test.ts src/views/WeeklyReportsView.test.ts src/views/WeeklyReportDetailView.test.ts src/router/router.test.ts
npm test -- --run
npm run build
```

Expected: PASS.

- [ ] **Step 9: Commit the weekly-report pages**

```powershell
git add frontend/src/components/WeeklyReportForm.vue frontend/src/components/WeeklyReportForm.test.ts frontend/src/views/WeeklyReportsView.vue frontend/src/views/WeeklyReportsView.test.ts frontend/src/views/WeeklyReportDetailView.vue frontend/src/views/WeeklyReportDetailView.test.ts frontend/src/views/HomeView.vue frontend/src/router/index.ts frontend/src/router/router.test.ts frontend/src/styles/main.css
git commit -m "feat: add structured weekly report pages"
```

---

### Task 6: Add quarterly and yearly summary backend

**Files:**
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/domain/SummaryPeriodType.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/domain/WorkSummary.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/domain/WorkSummaryRepository.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/WorkSummaryRow.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/WorkSummaryMapper.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/MybatisWorkSummaryRepository.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/GeneratedWorkSummary.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/WeeklyReportAiService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/WorkSummaryService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/NoWeeklyReportsForPeriodException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/WorkSummaryNotFoundException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/application/SummaryGenerationException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/WorkSummaryController.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/dto/GenerateWorkSummaryRequest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/dto/UpdateWorkSummaryRequest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/weeklyreport/api/dto/WorkSummaryResponse.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/application/WorkSummaryServiceTest.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/api/WorkSummaryControllerTest.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/weeklyreport/infrastructure/persistence/MybatisWorkSummaryRepositoryTest.java`

**Interfaces:**
- Produces:

```java
WorkSummary generateQuarter(long userId, int year, int quarter);
WorkSummary generateYear(long userId, int year);
WorkSummary get(long userId, SummaryPeriodType type, LocalDate start, LocalDate end);
WorkSummary update(long userId, long id, String coreContent, String routineWork, int selfScore);
```

Repository signatures used by the service are:

```java
WorkSummary save(WorkSummary summary);
WorkSummary update(WorkSummary summary);
Optional<WorkSummary> findByUserIdAndPeriod(long userId, SummaryPeriodType type, LocalDate start, LocalDate end);
Optional<WorkSummary> findByIdAndUserId(long id, long userId);
```

- [ ] **Step 1: Write failing date-boundary and generation tests**

Verify exact ranges:

```java
service.generateQuarter(42L, 2026, 3);
verify(weeklyRepository).findByUserIdAndWeekStartDateBetween(
        42L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));

service.generateYear(42L, 2026);
verify(weeklyRepository).findByUserIdAndWeekStartDateBetween(
        42L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
```

Verify no reports throws before calling the Provider, score outside 0–100 is rejected, and regeneration updates the existing period row rather than inserting another.

- [ ] **Step 2: Write failing repository and controller tests**

Repository queries must include `user_id`, `period_type`, `period_start`, and `period_end`. Controller tests cover:

```text
POST /api/work-summaries/generate with QUARTER → 200
POST /api/work-summaries/generate with YEAR    → 200
GET  /api/work-summaries?...                   → 200 or 404
PUT  /api/work-summaries/{id}                  → 200
invalid quarter or score                       → 400
other user's ID                                → 404
```

- [ ] **Step 3: Run focused tests and verify they fail**

Run:

```powershell
cd backend
./mvnw.cmd -Dtest=WorkSummaryServiceTest,WorkSummaryControllerTest,MybatisWorkSummaryRepositoryTest test
```

Expected: FAIL because summary types and endpoints are absent.

- [ ] **Step 4: Implement summary domain and persistence**

```java
public enum SummaryPeriodType { QUARTER, YEAR }

public record WorkSummary(
        Long id, long userId, SummaryPeriodType periodType,
        LocalDate periodStart, LocalDate periodEnd,
        String coreContent, String routineWork, int selfScore,
        Instant generatedAt, Instant createdAt, Instant updatedAt) {
}
```

Repository `find` and update methods must scope by user ID.

- [ ] **Step 5: Extend the AI service for fixed summary JSON**

```java
public record GeneratedWorkSummary(String coreContent, String routineWork, int selfScore) {
}
```

Build input from weekly reports with clear labels for week, core work, problems, and next plan. Require strict JSON and validate nonblank content plus score range. Unlike DOCX classification, a provider/parsing failure throws a summary-generation exception so an existing saved summary is not overwritten.

- [ ] **Step 6: Implement period service**

Use `YearMonth`/`LocalDate` calculations for quarters and years. Before generation, fetch current-user reports; when empty throw `NoWeeklyReportsForPeriodException`. Save a new row or update the existing unique period row after a successful Provider result.

- [ ] **Step 7: Implement authenticated summary endpoints and mappings**

Request shape:

```java
public record GenerateWorkSummaryRequest(
        @NotNull SummaryPeriodType periodType,
        @Min(2000) @Max(2100) int year,
        @Min(1) @Max(4) Integer quarter) {
}
```

For `YEAR`, `quarter` must be null; for `QUARTER`, it must be present. Map empty period to `409 NO_WEEKLY_REPORTS_FOR_PERIOD` and missing summary to `404 WORK_SUMMARY_NOT_FOUND`.

- [ ] **Step 8: Run focused and full backend tests**

Run:

```powershell
./mvnw.cmd -Dtest=WorkSummaryServiceTest,WorkSummaryControllerTest,MybatisWorkSummaryRepositoryTest,WeeklyReportAiServiceTest test
./mvnw.cmd test
```

Expected: PASS.

- [ ] **Step 9: Commit the summary backend**

```powershell
git add backend/src/main/java/com/niniyumi/personalagent/weeklyreport backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java backend/src/test/java/com/niniyumi/personalagent/weeklyreport
git commit -m "feat: add quarterly and yearly work summaries"
```

---

### Task 7: Add quarterly and yearly summary frontend

**Files:**
- Create: `frontend/src/features/workSummary/types.ts`
- Create: `frontend/src/features/workSummary/workSummaryApi.ts`
- Create: `frontend/src/features/workSummary/workSummaryApi.test.ts`
- Create: `frontend/src/features/workSummary/workSummaryStore.ts`
- Create: `frontend/src/features/workSummary/workSummaryStore.test.ts`
- Create: `frontend/src/views/WorkSummariesView.vue`
- Create: `frontend/src/views/WorkSummariesView.test.ts`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/router/router.test.ts`
- Modify: `frontend/src/styles/main.css`

**Interfaces:**
- Consumes: summary backend endpoints and current access token.
- Produces: protected `/work-summaries` page with generate, view, edit, and save actions.

- [ ] **Step 1: Write failing API and store tests**

Define:

```ts
export type SummaryPeriodType = 'QUARTER' | 'YEAR'

export interface WorkSummary {
  id: number
  periodType: SummaryPeriodType
  periodStart: string
  periodEnd: string
  coreContent: string
  routineWork: string
  selfScore: number
  generatedAt: string
}
```

Verify generate body, GET query parameters, update body, bearer header, loading state, and the `NO_WEEKLY_REPORTS_FOR_PERIOD` user message.

- [ ] **Step 2: Write failing view and router tests**

Verify:

- Switching between quarterly and yearly modes changes visible controls.
- Quarter mode requires a quarter selection.
- Generate calls the store once.
- Result renders `核心内容`, `日常工作`, and `自我评分`.
- Score input limits are 0 and 100.
- Save emits edited content to the store.
- `/work-summaries` is protected.

- [ ] **Step 3: Run focused tests and verify they fail**

Run:

```powershell
cd frontend
npm test -- --run src/features/workSummary src/views/WorkSummariesView.test.ts src/router/router.test.ts
```

Expected: FAIL because the module and page are absent.

- [ ] **Step 4: Implement API and Pinia store**

Use the same explicit access-token pattern as the weekly-report module. Store state includes `summary`, `loading`, `saving`, and `error`. `generate`, `load`, and `update` replace the current summary only after successful responses.

- [ ] **Step 5: Implement `WorkSummariesView.vue`**

Use Element Plus segmented controls/tabs, year input, optional quarter select, generate button, and three editable result sections. Disable generate while loading and keep the previous visible result when regeneration fails.

- [ ] **Step 6: Add route and home navigation**

```ts
{
  path: '/work-summaries',
  name: 'work-summaries',
  component: WorkSummariesView,
  meta: { requiresAuth: true },
}
```

Add a clear summary entry in the real home dashboard without adding another dashboard card system.

- [ ] **Step 7: Run focused tests, full tests, and build**

Run:

```powershell
npm test -- --run src/features/workSummary src/views/WorkSummariesView.test.ts src/router/router.test.ts
npm test -- --run
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit the summary frontend**

```powershell
git add frontend/src/features/workSummary frontend/src/views/WorkSummariesView.vue frontend/src/views/WorkSummariesView.test.ts frontend/src/views/HomeView.vue frontend/src/router/index.ts frontend/src/router/router.test.ts frontend/src/styles/main.css
git commit -m "feat: add work summary workspace"
```

---

### Task 8: Run the complete user workflow and update project documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/development/local-setup.md`
- Create: `docs/testing/weekly-report-test-cases.md`
- Modify: `docs/progress/2026-08-24.md`

**Interfaces:**
- Consumes: completed backend, frontend, MySQL V2 schema, local Provider configuration when available, and WAR packaging.
- Produces: verified weekly-report MVP with honest documentation and a final reviewable commit history.

- [ ] **Step 1: Run static verification from a clean process state**

Run:

```powershell
cd backend
./mvnw.cmd test
cd ../frontend
npm test -- --run
npm run build
cd ../backend
./mvnw.cmd clean package
```

Expected: all commands pass and the WAR contains the latest frontend build.

- [ ] **Step 2: Start MySQL and the application, then verify Flyway V2**

Expected schema contains:

```text
users
refresh_sessions
weekly_reports
work_summaries
flyway_schema_history version 2
```

Do not print credentials in logs or documentation.

- [ ] **Step 3: Execute the authenticated browser workflow**

```text
login
→ open weekly reports
→ save core work with empty optional fields
→ reopen and add problems/next plan
→ upload a DOCX and verify text is appended
→ filter the current year/month
→ open detail and edit
→ generate quarterly summary
→ edit score and save
→ generate yearly summary
→ logout and confirm protected routes redirect
```

- [ ] **Step 4: Verify user isolation with a second account**

Create a second test account and confirm it cannot list or open the first account's report or summary IDs. Expected response for direct ID access: 404.

- [ ] **Step 5: Verify Provider behavior honestly**

With a configured real Provider, perform one DOCX classification and one period summary call and record only pass/fail, model name, and elapsed time. Without a configured Provider, verify Fake Provider tests plus DOCX fallback and record real-provider smoke testing as pending; do not claim it passed.

- [ ] **Step 6: Verify WAR after the final frontend build**

Check `jar tf backend/target/personal-agent.war` and, when Tomcat 10.1 is installed, repeat the browser workflow from the external Tomcat root context.

- [ ] **Step 7: Update user-facing documentation**

Update `README.md` to replace conversational-weekly-report, MinIO, and task-queue claims with the implemented structured workflow. Document environment variables without values:

```text
AI_BASE_URL
AI_API_KEY
AI_MODEL
AI_TIMEOUT_SECONDS
```

Create `weekly-report-test-cases.md` with automated commands and manual browser cases. Add the final progress entry with completed, pending, and deliberately excluded scope.

- [ ] **Step 8: Inspect the complete change set**

Run:

```powershell
git status --short
git diff --check
git log --oneline --decorate -12
```

Expected: no generated `dist`, `target`, secret file, local database file, or `.idea` path is tracked.

- [ ] **Step 9: Commit the final documentation**

```powershell
git add README.md docs/development/local-setup.md docs/testing/weekly-report-test-cases.md docs/progress/2026-08-24.md
git commit -m "docs: record structured weekly report delivery"
```

---

## Final Completion Gate

- Three-field weekly report form works; only core work is required.
- DOCX paragraphs and tables are extracted, classified, and appended without overwriting existing input.
- Classification failure preserves all extracted text in core work.
- Year/month history and detail/update flows work for the current user.
- Quarterly and yearly summaries contain the required three modules and an editable 0–100 score.
- Empty periods do not call the Provider.
- Cross-user list, detail, update, and summary access is blocked.
- One configured Provider is used without retry chains or automatic failover.
- Backend tests, frontend tests, frontend build, and WAR package pass.
- Real Provider and external Tomcat verification status are reported truthfully.
- Documentation and progress records match the actual implementation.
- No Redis, MinIO, queue, OCR, `.doc`, Feishu, recording, literature, or complex versioning code was added.
