# Personal Agent Foundation and Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce the first runnable increment of Personal Agent: a Java 21 Spring Boot backend connected to MySQL `work`, with database migrations, registration, login, access-token authentication, refresh, logout, and current-user lookup.

**Architecture:** Build one Spring Boot 3 modular-monolith application under `backend/`. Authentication owns its domain model, application services, HTTP API, MyBatis-Plus persistence adapters, and Spring Security adapter; other future modules consume only the authenticated user ID exposed by the security layer.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring Security, Spring OAuth2 Resource Server, MyBatis-Plus 3.5.17, MySQL 8, Flyway, Maven Wrapper, JUnit 5, Mockito, MockMvc

**Spec:** `docs/superpowers/specs/2026-08-23-personal-agent-design.md`

## Global Constraints

- Use Java 21 and package root `com.niniyumi.personalagent`.
- Connect to the existing MySQL 8 database `work`; never embed its password in source control.
- Use a modular monolith; do not add Redis, RabbitMQ, Docker, MinIO, microservices, or AI dependencies in this increment.
- Use `api`, `application`, `domain`, and `infrastructure` packages only where they contain real responsibilities.
- Every business behavior follows red-green-refactor: failing test, minimal implementation, passing test.
- Access tokens expire after 15 minutes; refresh sessions expire after 7 days.
- Store BCrypt password hashes and SHA-256 refresh-token hashes; never store plaintext passwords or refresh tokens.
- Every commit must leave `backend\mvnw.cmd test` passing.

---

## Delivery Roadmap

The approved spec covers four independently testable increments. Only Increment 1 is detailed in this plan so later plans can use the real code state instead of speculative files.

1. **Foundation and authentication — this plan:** Spring Boot, MySQL migrations, registration, login, refresh, logout, and current user.
2. **Web shell and weekly reports:** Vue application, MinIO file adapter, task executor, Chat Provider, conversational weekly-report workflow, editing, and DOCX export.
3. **Course recording and notes:** browser recording, idempotent audio parts, composed audio, Speech Provider, transcript, note generation, and export.
4. **Agent integration and release hardening:** Agent tool registry, home dashboard, SSE task panel, cross-module acceptance tests, deployment documentation, and performance checks.

Each later increment receives its own implementation plan immediately before execution.

## File Map for Increment 1

```text
backend/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
└── src/
    ├── main/java/com/niniyumi/personalagent/
    │   ├── PersonalAgentApplication.java
    │   ├── auth/
    │   │   ├── api/AuthController.java
    │   │   ├── api/CurrentUserController.java
    │   │   ├── api/dto/AuthTokensResponse.java
    │   │   ├── api/dto/LoginRequest.java
    │   │   ├── api/dto/RefreshRequest.java
    │   │   ├── api/dto/RegisterRequest.java
    │   │   ├── api/dto/UserResponse.java
    │   │   ├── application/EmailAlreadyExistsException.java
    │   │   ├── application/InvalidCredentialsException.java
    │   │   ├── application/InvalidRefreshTokenException.java
    │   │   ├── application/IssuedRefreshToken.java
    │   │   ├── application/LoginCommand.java
    │   │   ├── application/LoginResult.java
    │   │   ├── application/RegisterCommand.java
    │   │   ├── application/AuthService.java
    │   │   ├── application/RefreshTokenService.java
    │   │   ├── domain/RefreshSession.java
    │   │   ├── domain/RefreshSessionRepository.java
    │   │   ├── domain/User.java
    │   │   ├── domain/UserRepository.java
    │   │   ├── domain/UserStatus.java
    │   │   ├── infrastructure/persistence/MybatisRefreshSessionRepository.java
    │   │   ├── infrastructure/persistence/MybatisUserRepository.java
    │   │   ├── infrastructure/persistence/RefreshSessionRow.java
    │   │   ├── infrastructure/persistence/RefreshSessionMapper.java
    │   │   ├── infrastructure/persistence/UserRow.java
    │   │   ├── infrastructure/persistence/UserMapper.java
    │   │   ├── infrastructure/security/AuthenticatedUser.java
    │   │   ├── infrastructure/security/JwtProperties.java
    │   │   ├── infrastructure/security/JwtTokenService.java
    │   │   └── infrastructure/security/SecurityConfig.java
    │   └── common/api/
    │       ├── ApiErrorResponse.java
    │       └── GlobalExceptionHandler.java
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/V1__create_auth_tables.sql
    └── test/java/com/niniyumi/personalagent/auth/
        ├── api/AuthControllerTest.java
        ├── api/CurrentUserControllerTest.java
        ├── application/AuthServiceTest.java
        ├── application/RefreshTokenServiceTest.java
        └── infrastructure/security/JwtTokenServiceTest.java
```

---

### Task 1: Bootstrap the Reproducible Spring Boot Build

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/mvnw`
- Create: `backend/mvnw.cmd`
- Create: `backend/.mvn/wrapper/maven-wrapper.properties`
- Create: `backend/src/main/java/com/niniyumi/personalagent/PersonalAgentApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/java/com/niniyumi/personalagent/PersonalAgentApplicationTests.java`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: JDK 21 from the verified `JAVA_HOME`.
- Produces: `PersonalAgentApplication`, Maven Wrapper commands, and environment-driven datasource configuration used by every later task.

- [ ] **Step 1: Download the official Spring Initializr skeleton**

Run from the repository root in PowerShell:

```powershell
$starterUrl = 'https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=3.5.16&groupId=com.niniyumi&artifactId=personal-agent-backend&name=PersonalAgent&description=Personal%20AI%20Agent&packageName=com.niniyumi.personalagent&packaging=jar&javaVersion=21&dependencies=web,validation,security,oauth2-resource-server,flyway,mysql'
Invoke-WebRequest -Uri $starterUrl -OutFile "$env:TEMP\personal-agent-backend.zip"
Expand-Archive -LiteralPath "$env:TEMP\personal-agent-backend.zip" -DestinationPath backend
```

Expected: `backend\mvnw.cmd`, `backend\pom.xml`, and `PersonalAgentApplication.java` exist.

- [ ] **Step 2: Add MyBatis-Plus and test dependencies**

Add the property and dependencies to `backend/pom.xml`:

```xml
<properties>
    <java.version>21</java.version>
    <mybatis-plus.version>3.5.17</mybatis-plus.version>
</properties>

<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Keep the dependencies generated by Initializr for web, validation, security, OAuth2 resource server, Flyway, and MySQL.

Replace the generated `@SpringBootTest` context test with a build-only smoke test so unit tests do not require a real database password:

```java
class PersonalAgentApplicationTests {
    @Test
    void applicationTypeExists() {
        assertThat(PersonalAgentApplication.class).isNotNull();
    }
}
```

- [ ] **Step 3: Configure environment-only secrets**

Replace `backend/src/main/resources/application.yml` with:

```yaml
spring:
  application:
    name: personal-agent
  datasource:
    url: ${DB_URL:jdbc:mysql://127.0.0.1:3306/work?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true

app:
  security:
    jwt-secret: ${JWT_SECRET}
    access-token-minutes: 15
    refresh-token-days: 7

server:
  error:
    include-message: never
```

- [ ] **Step 4: Extend repository ignore rules for backend-local overrides**

Confirm `.gitignore` includes:

```gitignore
backend/target/
backend/src/main/resources/application-local.yml
```

- [ ] **Step 5: Verify the wrapper build resolves dependencies and runs the smoke test**

Run:

```powershell
Set-Location backend
.\mvnw.cmd -q test
.\mvnw.cmd -q -DskipTests package
```

Expected: both commands exit `0` and `backend\target\personal-agent-backend-*.jar` exists. The smoke test does not start Spring, so it does not require a datasource.

- [ ] **Step 6: Commit the reproducible skeleton**

```powershell
git add backend .gitignore
git commit -m "build: bootstrap Spring Boot backend"
```

---

### Task 2: Create Auth Tables and Persistence Ports

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_auth_tables.sql`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/domain/User.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/domain/UserStatus.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/domain/UserRepository.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/domain/RefreshSession.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/domain/RefreshSessionRepository.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/persistence/UserMapper.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/persistence/RefreshSessionMapper.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/persistence/UserRow.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/persistence/RefreshSessionRow.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/persistence/MybatisUserRepository.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/persistence/MybatisRefreshSessionRepository.java`

**Interfaces:**
- Consumes: MySQL datasource and Flyway configuration from Task 1.
- Produces: `UserRepository` and `RefreshSessionRepository`, the only persistence interfaces used by authentication application services.

- [ ] **Step 1: Write the migration contract test**

Create `backend/src/test/java/com/niniyumi/personalagent/auth/infrastructure/persistence/AuthMigrationContractTest.java`:

```java
class AuthMigrationContractTest {
    @Test
    void migrationContainsRequiredTablesAndUniqueKeys() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V1__create_auth_tables.sql"));
        assertThat(sql).contains("CREATE TABLE users", "CREATE TABLE refresh_sessions");
        assertThat(sql).contains("UNIQUE KEY uk_users_username", "UNIQUE KEY uk_users_email");
        assertThat(sql).doesNotContain("123456");
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

```powershell
Set-Location backend
.\mvnw.cmd -q -Dtest=AuthMigrationContractTest test
```

Expected: FAIL because `V1__create_auth_tables.sql` does not exist.

- [ ] **Step 3: Create the exact Flyway migration**

Create `V1__create_auth_tables.sql`:

```sql
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_sessions_token_hash (token_hash),
    KEY idx_refresh_sessions_user_id (user_id),
    CONSTRAINT fk_refresh_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

- [ ] **Step 4: Define domain models and repository ports**

Use immutable records and explicit repository methods:

```java
public record User(Long id, String username, String email, String passwordHash,
                   String displayName, UserStatus status, Instant createdAt, Instant updatedAt) {}

public interface UserRepository {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User save(User user);
    Optional<User> findByUsernameOrEmail(String login);
    Optional<User> findById(long id);
}

public record RefreshSession(Long id, long userId, String tokenHash,
                             Instant expiresAt, Instant revokedAt, Instant createdAt) {
    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}

public interface RefreshSessionRepository {
    RefreshSession save(RefreshSession session);
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    void revoke(long id, Instant revokedAt);
}
```

`UserStatus` contains only `ACTIVE` and `DISABLED`.

- [ ] **Step 5: Implement thin MyBatis-Plus adapters**

`UserRow` and `RefreshSessionRow` are mutable persistence-only classes with `@TableName`, `@TableId(type = IdType.AUTO)`, and fields matching the migration. `UserMapper` and `RefreshSessionMapper` extend `BaseMapper` for those rows. Repository adapters translate between persistence rows and domain records. Keep query construction inside the adapters:

```java
public Optional<User> findByUsernameOrEmail(String login) {
    return Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<UserRow>()
        .eq(UserRow::getUsername, login)
        .or()
        .eq(UserRow::getEmail, login)))
        .map(UserRow::toDomain);
}
```

- [ ] **Step 6: Run the migration contract and all unit tests**

```powershell
.\mvnw.cmd -q test
```

Expected: PASS.

- [ ] **Step 7: Apply Flyway to local MySQL and inspect tables**

Set secrets only in the current PowerShell process:

```powershell
$env:DB_PASSWORD = Read-Host 'MySQL password'
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
.\mvnw.cmd -q spring-boot:run
```

After startup reports Flyway success, stop the process and run the installed MySQL client:

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --user=root --password --database=work --execute='SHOW TABLES;'
```

Expected: `users`, `refresh_sessions`, and `flyway_schema_history` are present.

- [ ] **Step 8: Commit the persistence boundary**

```powershell
git add backend/src/main
git add backend/src/test/java/com/niniyumi/personalagent/auth/infrastructure
git commit -m "feat: add authentication persistence"
```

---

### Task 3: Implement User Registration

**Files:**
- Create: `backend/src/test/java/com/niniyumi/personalagent/auth/application/AuthServiceTest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/AuthService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/RegisterCommand.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/UsernameAlreadyExistsException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/EmailAlreadyExistsException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/api/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/api/dto/UserResponse.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/api/AuthController.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/common/api/ApiErrorResponse.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/auth/api/AuthControllerTest.java`

**Interfaces:**
- Consumes: `UserRepository` from Task 2 and Spring `PasswordEncoder`.
- Produces: `AuthService.register(RegisterCommand)` and `POST /api/auth/register`.

- [ ] **Step 1: Write failing registration service tests**

Cover one success and the two common conflicts:

```java
@Test
void registerHashesPasswordAndSavesActiveUser() {
    when(userRepository.existsByUsername("nini")).thenReturn(false);
    when(userRepository.existsByEmail("nini@example.com")).thenReturn(false);
    when(passwordEncoder.encode("Password123")).thenReturn("bcrypt-hash");
    when(userRepository.save(any())).thenAnswer(invocation ->
        withId(invocation.getArgument(0), 42L));

    User user = service.register(new RegisterCommand(
        "nini", "nini@example.com", "Password123", "Nini"));

    assertThat(user.id()).isEqualTo(42L);
    assertThat(user.passwordHash()).isEqualTo("bcrypt-hash");
    assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
}

@Test
void registerRejectsDuplicateUsername() {
    when(userRepository.existsByUsername("nini")).thenReturn(true);
    assertThatThrownBy(() -> service.register(command))
        .isInstanceOf(UsernameAlreadyExistsException.class);
}
```

Add the equivalent duplicate-email test.

- [ ] **Step 2: Run tests and verify red state**

```powershell
.\mvnw.cmd -q -Dtest=AuthServiceTest test
```

Expected: compilation FAIL because `AuthService` and `RegisterCommand` do not exist.

- [ ] **Step 3: Implement minimal registration service**

Use the exact contract:

```java
public record RegisterCommand(String username, String email, String password, String displayName) {}

@Transactional
public User register(RegisterCommand command) {
    String username = command.username().trim().toLowerCase(Locale.ROOT);
    String email = command.email().trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByUsername(username)) throw new UsernameAlreadyExistsException();
    if (userRepository.existsByEmail(email)) throw new EmailAlreadyExistsException();
    return userRepository.save(new User(null, username, email,
        passwordEncoder.encode(command.password()), command.displayName().trim(),
        UserStatus.ACTIVE, null, null));
}
```

- [ ] **Step 4: Run service tests and verify green state**

```powershell
.\mvnw.cmd -q -Dtest=AuthServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Write failing registration API test**

Use `@WebMvcTest(AuthController.class)`, `@AutoConfigureMockMvc(addFilters = false)`, and a mocked `AuthService`. Verify:

```java
mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"username":"nini","email":"nini@example.com",
             "password":"Password123","displayName":"Nini"}
            """))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.id").value(42))
    .andExpect(jsonPath("$.username").value("nini"));
```

Also verify blank fields return `400` and a duplicate username maps to `409` with code `USERNAME_EXISTS`.

- [ ] **Step 6: Implement DTO validation, controller, and error response**

`RegisterRequest` rules:

```java
public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 80) String displayName) {}
```

Return `201 Created` with `UserResponse(id, username, email, displayName)`. `ApiErrorResponse` contains `code`, `message`, and `traceId`; it never returns a stack trace.

- [ ] **Step 7: Run registration tests and the full suite**

```powershell
.\mvnw.cmd -q test
```

Expected: PASS.

- [ ] **Step 8: Commit registration**

```powershell
git add backend/src
git commit -m "feat: add user registration"
```

---

### Task 4: Implement Access-Token Security and Current User

**Files:**
- Create: `backend/src/test/java/com/niniyumi/personalagent/auth/infrastructure/security/JwtTokenServiceTest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/security/JwtProperties.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/security/JwtTokenService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/security/AuthenticatedUser.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/infrastructure/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/api/CurrentUserController.java`
- Test: `backend/src/test/java/com/niniyumi/personalagent/auth/api/CurrentUserControllerTest.java`

**Interfaces:**
- Consumes: active users from `UserRepository`.
- Produces: `JwtTokenService.issueAccessToken(User)` and authenticated `GET /api/users/me`. Login is added with refresh-token rotation in Task 5 so the public login response is introduced only once.

- [ ] **Step 1: Write failing JWT tests with a fixed clock**

```java
@Test
void issuedTokenContainsUserIdentityAndFifteenMinuteExpiry() {
    String token = tokenService.issueAccessToken(activeUser(42L));
    Jwt jwt = decoder.decode(token);
    assertThat(jwt.getSubject()).isEqualTo("42");
    assertThat(jwt.getClaimAsString("username")).isEqualTo("nini");
    assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-08-23T08:15:00Z"));
}
```

Use a test secret produced from exactly 32 bytes and a fixed `Clock` at `2026-08-23T08:00:00Z`.

- [ ] **Step 2: Run JWT test and verify it fails**

```powershell
.\mvnw.cmd -q -Dtest=JwtTokenServiceTest test
```

Expected: compilation FAIL because the token service does not exist.

- [ ] **Step 3: Implement JWT encoder, decoder, and security chain**

Use Spring Security Nimbus with HS256. Required claims are `sub`, `username`, `iat`, and `exp`. Configure routes:

```java
http.csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
        .anyRequest().authenticated())
    .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
```

`AuthenticatedUser` contains `long userId` and `String username`. The converter builds it from JWT claims and grants no role-based authorities in this increment. `SecurityConfig` also provides `PasswordEncoder` and `Clock`:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
Clock clock() {
    return Clock.systemUTC();
}
```

- [ ] **Step 4: Run JWT tests and verify they pass**

```powershell
.\mvnw.cmd -q -Dtest=JwtTokenServiceTest test
```

- [ ] **Step 5: Write failing current-user API tests**

Verify anonymous access is rejected and an authenticated user can retrieve their profile:

```java
mockMvc.perform(get("/api/users/me"))
    .andExpect(status().isUnauthorized());

mockMvc.perform(get("/api/users/me")
        .with(authentication(new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(42L, "nini"), "token", List.of()))))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value(42));
```

- [ ] **Step 6: Implement current-user endpoint**

`GET /api/users/me` reads `@AuthenticationPrincipal AuthenticatedUser`, loads the user through `UserRepository.findById`, and returns `UserResponse`. The controller never accepts a user ID from request parameters.

- [ ] **Step 7: Run security and current-user tests**

```powershell
.\mvnw.cmd -q -Dtest=JwtTokenServiceTest,CurrentUserControllerTest test
```

Expected: PASS.

- [ ] **Step 8: Run the full suite and commit**

```powershell
.\mvnw.cmd -q test
git add backend/src
git commit -m "feat: add access token security"
```

---

### Task 5: Implement Login, Refresh, and Logout

**Files:**
- Create: `backend/src/test/java/com/niniyumi/personalagent/auth/application/RefreshTokenServiceTest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/RefreshTokenService.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/IssuedRefreshToken.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/LoginCommand.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/LoginResult.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/InvalidCredentialsException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/application/InvalidRefreshTokenException.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/api/dto/RefreshRequest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/api/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/niniyumi/personalagent/auth/api/dto/AuthTokensResponse.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/auth/application/AuthService.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/auth/api/AuthController.java`
- Modify: `backend/src/main/java/com/niniyumi/personalagent/common/api/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: `RefreshSessionRepository`, `UserRepository`, `Clock`, and `JwtTokenService`.
- Produces: opaque refresh tokens, `POST /api/auth/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout`.

- [ ] **Step 1: Write failing refresh-token tests**

Cover issuing, refreshing, expiry, and revocation:

```java
@Test
void issueStoresOnlyHashAndReturnsOpaqueToken() {
    IssuedRefreshToken issued = service.issue(42L);
    verify(repository).save(argThat(session ->
        !session.tokenHash().equals(issued.value())
            && session.expiresAt().equals(now.plus(7, ChronoUnit.DAYS))));
}

@Test
void refreshRejectsRevokedSession() {
    when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedSession));
    assertThatThrownBy(() -> service.validate("opaque-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
}
```

- [ ] **Step 2: Run tests and verify they fail**

```powershell
.\mvnw.cmd -q -Dtest=RefreshTokenServiceTest test
```

- [ ] **Step 3: Implement opaque token generation and hashing**

Generate 32 random bytes with `SecureRandom`, encode using URL-safe Base64 without padding, and hash the presented token with SHA-256 lowercase hex before repository lookup.

```java
byte[] bytes = new byte[32];
secureRandom.nextBytes(bytes);
String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
String tokenHash = HexFormat.of().formatHex(
    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
```

Return the plaintext value only from the issue call:

```java
public record IssuedRefreshToken(String value, RefreshSession session) {}
```

- [ ] **Step 4: Run refresh-token tests and verify they pass**

```powershell
.\mvnw.cmd -q -Dtest=RefreshTokenServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Write failing login and rotation tests**

Verify a valid username or email and password returns both tokens. Missing users, wrong passwords, and `DISABLED` users must all throw `InvalidCredentialsException`, preventing account enumeration. Refresh must revoke the used session before issuing a replacement.

```java
when(userRepository.findByUsernameOrEmail("nini")).thenReturn(Optional.of(activeUser));
when(passwordEncoder.matches("Password123", activeUser.passwordHash())).thenReturn(true);
when(jwtTokenService.issueAccessToken(activeUser)).thenReturn("access-token");
when(refreshTokenService.issue(activeUser.id())).thenReturn(new IssuedRefreshToken("refresh-token", session));

LoginResult result = authService.login(new LoginCommand("nini", "Password123"));

assertThat(result.accessToken()).isEqualTo("access-token");
assertThat(result.refreshToken()).isEqualTo("refresh-token");
```

- [ ] **Step 6: Implement login response and authentication endpoints**

`AuthTokensResponse` contains:

```java
public record AuthTokensResponse(String accessToken, String refreshToken,
                                 String tokenType, long expiresInSeconds) {}
```

Use `tokenType="Bearer"` and `expiresInSeconds=900`.

Define application results explicitly:

```java
public record LoginCommand(String login, String password) {}
public record LoginResult(String accessToken, String refreshToken) {}
```

`POST /api/auth/login` accepts `LoginRequest(@NotBlank String login, @NotBlank String password)`. `AuthService.login` verifies an active user and password, then issues both tokens.

Implement refresh and logout endpoints:

Both accept `RefreshRequest(@NotBlank String refreshToken)`.

- `POST /api/auth/refresh`: validate active refresh session, load active user, revoke the used session, issue a new refresh session, and return a new token pair.
- `POST /api/auth/logout`: revoke the matching active session and return `204 No Content`.
- Invalid, expired, or revoked refresh tokens return `401` with code `INVALID_REFRESH_TOKEN`.

- [ ] **Step 7: Add API tests and run the full suite**

Add MockMvc tests for successful login, generic invalid credentials, refresh rotation, invalid refresh, and logout. Then run:

```powershell
.\mvnw.cmd -q test
```

Expected: PASS.

- [ ] **Step 8: Commit authentication sessions**

```powershell
git add backend/src
git commit -m "feat: add login and refresh sessions"
```

---

### Task 6: Run the Real MySQL Authentication Smoke Test

**Files:**
- Create: `.env.example`
- Modify: `README.md`
- Create: `docs/development/local-setup.md`

**Interfaces:**
- Consumes: all authentication endpoints from Tasks 3–5 and local MySQL `work`.
- Produces: reproducible local setup and evidence that the first increment works against the real database.

- [ ] **Step 1: Add safe environment documentation**

Create `.env.example` without real secrets:

```dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/work?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=replace-with-local-mysql-password
JWT_SECRET=replace-with-base64-encoded-32-byte-secret
```

Document that `.env` is ignored and Spring Boot does not automatically load `.env`; PowerShell users set variables in the current process.

- [ ] **Step 2: Document exact local start commands**

Add to `docs/development/local-setup.md`:

```powershell
$env:DB_PASSWORD = Read-Host 'MySQL password'
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
Set-Location backend
.\mvnw.cmd spring-boot:run
```

- [ ] **Step 3: Run all tests before the smoke test**

```powershell
Set-Location backend
.\mvnw.cmd test
```

Expected: zero failures and exit code `0`.

- [ ] **Step 4: Start the backend against MySQL `work`**

Run the commands from Step 2 and wait for `Started PersonalAgentApplication`.

Expected: Flyway reports schema version `1`; the server listens on port `8080`.

- [ ] **Step 5: Exercise the full authentication flow**

In a second PowerShell terminal:

```powershell
$register = @{ username='nini_demo'; email='nini_demo@example.com'; password='Password123'; displayName='Nini Demo' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/auth/register' -ContentType 'application/json' -Body $register

$login = @{ login='nini_demo'; password='Password123' } | ConvertTo-Json
$tokens = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/auth/login' -ContentType 'application/json' -Body $login

Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:8080/api/users/me' -Headers @{ Authorization = "Bearer $($tokens.accessToken)" }

$refresh = @{ refreshToken=$tokens.refreshToken } | ConvertTo-Json
$newTokens = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/auth/refresh' -ContentType 'application/json' -Body $refresh

Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/auth/logout' -ContentType 'application/json' -Body (@{ refreshToken=$newTokens.refreshToken } | ConvertTo-Json)
```

Expected: registration returns user ID, login returns two tokens, `/me` returns `nini_demo`, refresh returns a new token pair, and logout returns `204`.

- [ ] **Step 6: Verify plaintext credentials are absent**

```powershell
git grep -n -E '123456|Password123|nini_demo@example.com' -- ':!docs/superpowers/plans/*'
```

Expected: no tracked source or configuration matches. The smoke-test credentials appear only in this plan document.

- [ ] **Step 7: Update README status and documentation links**

Change project status from “implementation has not started” to “authentication increment complete” only after Steps 3–6 pass. Link `docs/development/local-setup.md` from README.

- [ ] **Step 8: Final verification and commit**

```powershell
Set-Location backend
.\mvnw.cmd test
Set-Location ..
git status --short
git add .env.example README.md docs/development/local-setup.md
git commit -m "docs: add local authentication setup"
```

Expected: tests pass, only intended documentation is staged, and the commit succeeds.

---

## Increment 1 Completion Gate

Before starting the weekly-report plan, verify all statements are true:

- `backend\mvnw.cmd test` exits `0`.
- Flyway applies `V1__create_auth_tables.sql` to MySQL `work`.
- Registration, login, `/me`, refresh, and logout succeed against the running application.
- Invalid login and invalid refresh tokens both return `401` without revealing account existence.
- Duplicate username and email return `409` with stable error codes.
- `git status --short` is empty.
- No plaintext database password, JWT secret, user password, or refresh token is tracked.
- README links the system design and local setup documents.

After this gate passes, write `docs/superpowers/plans/2026-08-23-weekly-report-implementation.md` from the actual repository state.
