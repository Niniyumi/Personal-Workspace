# Frontend, Backend, and Tomcat Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the approved static Element Plus dashboard into the authenticated home page, verify the complete login flow against MySQL, and package the Vue and Spring Boot application as a deployable WAR for Tomcat 10.1.

**Architecture:** Keep Vite and Spring Boot separate during development, with `/api` proxied to port 8080. For integration and interview demos, build Vue into `frontend/dist`, include those files in the backend WAR, and forward explicit SPA routes to `index.html`. The same Spring Boot application remains executable and deployable; no second backend implementation is introduced.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring Security, Maven, Tomcat 10.1, Vue 3, TypeScript, Vite, Pinia, Element Plus, Vitest, MySQL 8, Flyway

**Spec:** `docs/superpowers/specs/2026-08-24-structured-weekly-report-and-tomcat-design.md`

## Global Constraints

- Finish and verify this plan before starting the structured weekly-report plan.
- Do not add Redis, CORS configuration, a generic retry framework, or a second dashboard implementation.
- Access-token recovery remains a single refresh attempt during application bootstrap.
- Keep secrets in environment variables; never commit local passwords, JWTs, refresh tokens, or model keys.
- Use Tomcat 10.1.25 or newer in the 10.1 line and Java 21.
- Package the production SPA at the root context `/`; `/api/**` must remain backend endpoints.
- Preserve the approved warm-white, black, orange, yellow, rounded Element Plus visual language.
- Add Chinese comments only where they explain an architectural or security decision.

---

### Task 1: Capture the approved UI baseline

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/styles/main.css`
- Modify: `frontend/src/components/AuthShell.vue`
- Modify: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/PreviewView.vue`
- Create: `frontend/src/views/PreviewView.test.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/router/router.test.ts`
- Modify: `frontend/src/views/AuthViews.test.ts`

**Interfaces:**
- Consumes: existing `useAuthStore()` login and logout actions.
- Produces: a tested, committed visual baseline containing Element Plus and the approved `/preview` dashboard.

- [ ] **Step 1: Run the focused preview and authentication view tests**

Run:

```powershell
cd frontend
npm test -- --run src/views/PreviewView.test.ts src/views/AuthViews.test.ts src/router/router.test.ts
```

Expected: all preview, login, home, and router tests pass.

- [ ] **Step 2: Run the complete frontend test suite**

Run:

```powershell
npm test -- --run
```

Expected: all existing frontend tests pass with no unhandled promise rejection.

- [ ] **Step 3: Run the production build**

Run:

```powershell
npm run build
```

Expected: `vue-tsc` and Vite finish successfully and create `frontend/dist`.

- [ ] **Step 4: Review the staged file list before committing**

Run:

```powershell
git diff --check
git status --short
```

Expected: only the approved UI files listed in this task are modified or untracked; the design and plan documents remain separate commits.

- [ ] **Step 5: Commit the approved UI baseline**

```powershell
git add frontend/package.json frontend/package-lock.json frontend/src/main.ts frontend/src/styles/main.css frontend/src/components/AuthShell.vue frontend/src/views/LoginView.vue frontend/src/views/PreviewView.vue frontend/src/views/PreviewView.test.ts frontend/src/router/index.ts frontend/src/router/router.test.ts frontend/src/views/AuthViews.test.ts
git commit -m "feat: add approved element plus workspace preview"
```

---

### Task 2: Replace the old authenticated home with the approved dashboard

**Files:**
- Modify: `frontend/src/views/AuthViews.test.ts`
- Modify: `frontend/src/router/router.test.ts`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/router/index.ts`
- Delete: `frontend/src/views/PreviewView.vue`
- Delete: `frontend/src/views/PreviewView.test.ts`

**Interfaces:**
- Consumes: `useAuthStore().user`, `useAuthStore().logout()`, and router route name `login`.
- Produces: authenticated `/` dashboard; the temporary public `/preview` route no longer exists.

- [ ] **Step 1: Write failing home and router tests**

Add assertions to `AuthViews.test.ts` that the authenticated home uses real account data and can log out:

```ts
expect(wrapper.text()).toContain('今天想整理些什么？')
expect(wrapper.text()).toContain('Nini')
expect(wrapper.text()).toContain('nini@example.com')
expect(wrapper.text()).not.toContain('静态预览模式')
```

Replace the public preview router test with:

```ts
it('does not expose the temporary static preview route', async () => {
  const store = useAuthStore()
  store.status = 'authenticated'
  const router = createAppRouter(createMemoryHistory())

  await router.push('/preview')

  expect(router.currentRoute.value.path).toBe('/')
})
```

- [ ] **Step 2: Run the focused tests and verify the new expectations fail**

Run:

```powershell
cd frontend
npm test -- --run src/views/AuthViews.test.ts src/router/router.test.ts
```

Expected: FAIL because `HomeView` still uses the old green layout and `/preview` is still registered.

- [ ] **Step 3: Move the approved dashboard into `HomeView.vue`**

Use the existing `PreviewView.vue` markup and styles as the visual source, then replace static account content with authenticated state:

```ts
const store = useAuthStore()
const router = useRouter()

const avatarText = computed(() => store.user?.displayName.trim().slice(0, 1) || 'U')

async function logout() {
  await store.logout()
  await router.push({ name: 'login' })
}
```

The profile section must render:

```vue
<ElAvatar :size="42">{{ avatarText }}</ElAvatar>
<div>
  <strong>{{ store.user?.displayName }}</strong>
  <span>{{ store.user?.email }}</span>
</div>
```

Keep weekly-report and course buttons visually present but non-destructive; weekly routing is added by the next implementation plan.

- [ ] **Step 4: Remove the preview route and duplicate files**

Remove the `PreviewView` import and route from `frontend/src/router/index.ts`, then delete `PreviewView.vue` and its standalone test. The catch-all route continues redirecting to `/`.

- [ ] **Step 5: Run focused and full frontend verification**

Run:

```powershell
npm test -- --run src/views/AuthViews.test.ts src/router/router.test.ts
npm test -- --run
npm run build
```

Expected: all tests and the production build pass.

- [ ] **Step 6: Commit the real dashboard**

```powershell
git add frontend/src/views/HomeView.vue frontend/src/views/AuthViews.test.ts frontend/src/router/index.ts frontend/src/router/router.test.ts
git add -u frontend/src/views/PreviewView.vue frontend/src/views/PreviewView.test.ts
git commit -m "feat: connect authenticated workspace dashboard"
```

---

### Task 3: Verify the real authentication flow against MySQL

**Files:**
- Modify: `docs/testing/login-ui-test-cases.md`
- Modify: `docs/progress/2026-08-24.md`

**Interfaces:**
- Consumes: MySQL database `work`, backend port 8080, frontend Vite port selected at runtime, and authentication endpoints already implemented.
- Produces: recorded end-to-end evidence that frontend, backend, and MySQL are connected.

- [ ] **Step 1: Run all backend tests before starting local services**

Run:

```powershell
cd backend
./mvnw.cmd test
```

Expected: Maven reports `BUILD SUCCESS`.

- [ ] **Step 2: Start the backend with local environment values**

Use the existing untracked/local environment configuration and never print secret values. Run the Spring Boot application on port 8080 and verify Flyway reaches schema version 1.

Expected log evidence:

```text
Tomcat started on port 8080
Successfully validated 1 migration
```

- [ ] **Step 3: Start the Vite development server**

Run from `frontend`:

```powershell
npm run dev -- --host 127.0.0.1
```

Expected: Vite prints the local URL and proxies `/api` to port 8080.

- [ ] **Step 4: Execute the browser authentication scenario**

Use a unique test identity such as `codex_e2e_<timestamp>` and perform:

```text
register → username login → real home → browser refresh → logout
login again with email → real home → logout
```

Verify the home page shows the registered display name and email, refresh remains authenticated, logout returns to `/login`, and navigating to `/` after logout redirects to `/login`.

- [ ] **Step 5: Inspect browser and server evidence**

Expected:

- Browser console contains no uncaught error.
- Network calls to `/api/auth/login` and `/api/users/me` return 200.
- Logout returns 204.
- No secret or token appears in committed files.

- [ ] **Step 6: Record the exact verification results**

Append a dated section to `docs/testing/login-ui-test-cases.md` and `docs/progress/2026-08-24.md` containing commands, pass/fail status, browser URL, and any remaining limitation. Do not record passwords or tokens.

- [ ] **Step 7: Commit the integration evidence**

```powershell
git add docs/testing/login-ui-test-cases.md docs/progress/2026-08-24.md
git commit -m "docs: record frontend backend integration verification"
```

---

### Task 4: Produce a deployable WAR with Vue SPA routing

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/PersonalAgentApplication.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/common/web/SpaForwardController.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/common/web/SpaForwardControllerTest.java`
- Create: `backend/src/test/java/com/niniyumi/personalagent/WarPackagingContractTest.java`
- Create: `docs/development/idea-tomcat.md`
- Modify: `docs/development/local-setup.md`

**Interfaces:**
- Consumes: `frontend/dist/index.html`, Spring Boot MVC, and the existing `/api/**` security chain.
- Produces: `backend/target/personal-agent.war`, explicit SPA route forwarding, and reproducible IDEA Tomcat setup instructions.

- [ ] **Step 1: Write failing WAR packaging contract tests**

Create `WarPackagingContractTest` that reads `pom.xml` and verifies the required packaging contract:

```java
@Test
void pomBuildsDeployableWarWithFrontendResources() throws IOException {
    String pom = Files.readString(Path.of("pom.xml"));
assertThat(pom).contains("<packaging>war</packaging>")
            .contains("<artifactId>spring-boot-starter-tomcat</artifactId>")
            .contains("<scope>provided</scope>")
            .contains("<finalName>personal-agent</finalName>")
            .contains("../frontend/dist");
}
```

Update `PersonalAgentApplicationTests` or add a reflection assertion that `PersonalAgentApplication` extends `SpringBootServletInitializer`.

- [ ] **Step 2: Write a failing SPA forwarding controller test**

Create a `@WebMvcTest(SpaForwardController.class)` test with filters disabled:

```java
mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));

mockMvc.perform(get("/weekly-reports/42"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
```

- [ ] **Step 3: Run the focused backend tests and verify they fail**

Run:

```powershell
cd backend
./mvnw.cmd -Dtest=WarPackagingContractTest,SpaForwardControllerTest,PersonalAgentApplicationTests test
```

Expected: FAIL because WAR packaging, the initializer base class, and the forwarding controller are missing.

- [ ] **Step 4: Convert the application to an executable and deployable WAR**

Add to `pom.xml`:

```xml
<packaging>war</packaging>
```

Add the explicit provided container dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

Set the stable artifact name under `<build>` and configure `maven-war-plugin` to include Vue output without invoking Node from Maven:

```xml
<finalName>personal-agent</finalName>
<plugin>
    <artifactId>maven-war-plugin</artifactId>
    <configuration>
        <failOnMissingWebXml>false</failOnMissingWebXml>
        <webResources>
            <resource>
                <directory>../frontend/dist</directory>
                <targetPath>/</targetPath>
            </resource>
        </webResources>
    </configuration>
</plugin>
```

Update the application class:

```java
public class PersonalAgentApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(PersonalAgentApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(PersonalAgentApplication.class, args);
    }
}
```

- [ ] **Step 5: Implement explicit SPA route forwarding**

Create `SpaForwardController` with only UI routes known in this increment:

```java
@Controller
public class SpaForwardController {
    @GetMapping({"/", "/login", "/register", "/weekly-reports", "/weekly-reports/{id}", "/work-summaries"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
```

Do not use a catch-all mapping that could capture `/api/**` or files containing an extension.

- [ ] **Step 6: Permit SPA resources while keeping APIs protected**

Update the authorization rules in `SecurityConfig`:

```java
.requestMatchers(
        "/", "/index.html", "/favicon.svg", "/assets/**",
        "/login", "/register", "/weekly-reports/**", "/work-summaries")
    .permitAll()
.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
    .permitAll()
.anyRequest().authenticated()
```

Only the HTML/static routes are public; weekly-report and summary APIs remain protected because they use the `/api/...` prefix.

- [ ] **Step 7: Run focused and complete backend tests**

Run:

```powershell
./mvnw.cmd -Dtest=WarPackagingContractTest,SpaForwardControllerTest,PersonalAgentApplicationTests test
./mvnw.cmd test
```

Expected: all tests pass.

- [ ] **Step 8: Build the Vue application and WAR**

Run from the repository root:

```powershell
cd frontend
npm run build
cd ../backend
./mvnw.cmd clean package
```

Expected: `backend/target/personal-agent.war` exists and contains `index.html` plus hashed frontend assets.

- [ ] **Step 9: Verify the WAR contents**

Run:

```powershell
jar tf target/personal-agent.war
```

Expected output includes:

```text
index.html
assets/
WEB-INF/classes/com/niniyumi/personalagent/PersonalAgentApplication.class
```

- [ ] **Step 10: Write the IDEA Tomcat guide**

Document the exact build order, Tomcat 10.1 server directory, Java 21 JRE, `war exploded` deployment, root `/` context, environment variables, browser URL, redeploy action, and port-conflict rule in `docs/development/idea-tomcat.md`. Update `local-setup.md` to link to it.

- [ ] **Step 11: Run the external Tomcat smoke test when Tomcat is available**

Deploy the WAR at root context and verify:

```text
GET /login                         → Vue login page
POST /api/auth/login               → authentication response
GET /weekly-reports/42             → Vue application, not Tomcat 404
GET /api/users/me without token    → 401 JSON response
```

If local Tomcat is not installed yet, record that only the WAR-content and executable-WAR checks passed; do not claim external-container verification.

- [ ] **Step 12: Commit the WAR and Tomcat work**

```powershell
git add backend/pom.xml backend/src/main/java/com/niniyumi/personalagent/PersonalAgentApplication.java backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/security/SecurityConfig.java backend/src/main/java/com/niniyumi/personalagent/common/web/SpaForwardController.java backend/src/test/java/com/niniyumi/personalagent/common/web/SpaForwardControllerTest.java backend/src/test/java/com/niniyumi/personalagent/WarPackagingContractTest.java docs/development/idea-tomcat.md docs/development/local-setup.md
git commit -m "build: support idea tomcat war deployment"
```

---

## Plan Completion Gate

Before starting `2026-08-24-structured-weekly-report.md`, verify all statements are true:

- Approved UI baseline is committed separately.
- `/` uses the approved dashboard and real authenticated user data.
- `/preview` and its duplicate component are removed.
- Browser registration, login, refresh-on-bootstrap, and logout pass against MySQL `work`.
- Frontend tests and production build pass.
- Backend tests pass.
- `personal-agent.war` contains Vue production assets and backend classes.
- Direct SPA routes forward to `index.html` without capturing `/api/**`.
- IDEA Tomcat setup is documented, with external Tomcat verification status stated honestly.
- No secret was added to Git.
