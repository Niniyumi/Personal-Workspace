# Local development setup

## Prerequisites

- Java 17
- MySQL 8 running locally, with a database named `work`

## Protect the existing database

Before starting the application, inspect the existing `work` database for important data and
tables with names used by this application. Back up any data you need to preserve before Flyway
runs. The application applies migrations directly to `work`; do not continue until you are
confident the database is safe to migrate.

The application reads configuration from process environment variables. The checked-in
`.env.example` is a reference only: `.env` is ignored by Git, and Spring Boot does not
automatically load `.env` files. Do not commit a database password, JWT secret, model API
key, user password, or issued refresh token.

## Start the backend

In PowerShell, set the local MySQL password in the current process and generate a fresh
JWT secret containing at least 16 characters, then start the application. The command below generates a stronger value:

```powershell
$env:DB_PASSWORD = Read-Host 'MySQL password'
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
Set-Location backend
.\mvnw.cmd spring-boot:run
```

If you already use the ignored `application-local.yml` in the project root, start it explicitly:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.config.additional-location=file:../application-local.yml"
```

`app.security.jwt-secret` must contain at least 32 ASCII characters. A shorter value allows
registration but makes JWT issuance fail during login.

The file is intentionally excluded from Maven resources, so it cannot enter the JAR. The variables apply only to the current PowerShell process. Keep the terminal open while the
application runs. On a successful start, Flyway reports schema version `7` and Spring
Boot listens on port `8080`.

## Optional model provider

DOCX extraction works without a model key. To classify extracted text into the three weekly-report fields, set one OpenAI-compatible provider:

```powershell
$env:AI_BASE_URL = 'https://api.openai.com/v1'
$env:DASHSCOPE_API_KEY = Read-Host 'Alibaba Cloud Model Studio API key'
$env:AI_MODEL = 'your-model-name'
$env:AI_TIMEOUT_SECONDS = '120'
```

The active provider is selected only by these values. There is no automatic provider chain or retry. If the provider is missing, unavailable, or returns invalid JSON, the extracted text is placed in “本周核心工作” for manual editing.

## Verify the build

Run the test suite before starting the application:

```powershell
Set-Location backend
.\mvnw.cmd test
```

The command must finish with zero failures and exit code `0`.

## Build the deployable website

The executable JAR contains both the Vue page, Spring Boot backend, and embedded Tomcat:

```powershell
Set-Location frontend
npm run build
Set-Location ../backend
.\mvnw.cmd clean package
```

The result is `backend/target/personal-agent.jar`. Local and secret configuration files are excluded from this artifact. From `backend`, run it with `java -jar target/personal-agent.jar --spring.config.additional-location=file:../application-local.yml`. For IDEA configuration, see [IDEA + executable JAR](idea-jar.md).

## Authentication smoke-test flow

With the backend running, use a second PowerShell terminal. Choose a unique username and
email, and keep the chosen password only in that terminal session. Exercise these
endpoints in order:

1. `POST /api/auth/registration-code/request`
2. Read the registration code from the test mailbox, then `POST /api/auth/register`
3. `POST /api/auth/login`
4. `GET /api/users/me` with the returned access token
5. `POST /api/auth/password-reset/request`
6. Read the reset code from the test mailbox, then `POST /api/auth/password-reset/confirm`
7. Log in with the new password and confirm that the old refresh token is rejected
8. `POST /api/auth/logout` with the current refresh token

Expected results are a created user, token pairs from login and refresh, the current user
from `/api/users/me`, and HTTP `204 No Content` from logout. Reusing the logged-out
refresh token must return HTTP `401 Unauthorized`.
