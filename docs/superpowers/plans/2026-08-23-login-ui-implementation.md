# Login UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tested Vue 3 authentication frontend that registers users, logs in, restores a saved session, refreshes once when necessary, shows the current user, and logs out against the existing Spring Boot API.

**Architecture:** Add one Vite SPA under `frontend/`. Authentication code is isolated in `features/auth`: an API adapter talks to `/api`, a storage adapter owns browser persistence, and one Pinia store coordinates session state. Views depend on the store rather than Axios or localStorage directly.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, Pinia, Axios, Vitest, Vue Test Utils, jsdom, plain CSS

**Spec:** `docs/superpowers/specs/2026-08-23-personal-agent-design.md`

## Global Constraints

- Keep the modular-monolith backend unchanged unless real integration proves a contract defect.
- Do not add a UI framework, form library, CSS framework, third-party login, password recovery, CAPTCHA, or roles.
- Use `/api` through the Vite development proxy; do not add local CORS configuration.
- Persist tokens only through `tokenStorage.ts`; components must never access browser storage directly.
- Attempt refresh once during application bootstrap; do not build a generic retry framework.
- Use visible labels, keyboard focus, inline errors, 44px minimum controls, reduced-motion support, and responsive layouts from 375px upward.
- Never commit real passwords, JWTs, refresh tokens, or local database configuration.

---

### Task 1: Scaffold the Frontend and Test Runtime

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig*.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/styles/main.css`
- Create: `frontend/src/test/setup.ts`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: Spring Boot at `http://127.0.0.1:8080` during local development.
- Produces: `npm run dev`, `npm run test`, and `npm run build`; `/api` proxy; Vue app with Pinia and Router installed.

- [ ] Generate a Vue TypeScript Vite project under `frontend/`.
- [ ] Install runtime packages `vue-router`, `pinia`, and `axios`.
- [ ] Install test packages `vitest`, `jsdom`, `@vue/test-utils`, and `axios-mock-adapter`.
- [ ] Configure Vite test environment as `jsdom`, setup file `src/test/setup.ts`, and `/api` proxy target `http://127.0.0.1:8080`.
- [ ] Replace generated demo UI with an empty router outlet and shared CSS tokens.
- [ ] Run `npm run test -- --run` and `npm run build`; both must exit 0.
- [ ] Commit as `build: scaffold Vue authentication frontend`.

### Task 2: Implement Authentication Boundaries with Tests

**Files:**
- Create: `frontend/src/features/auth/types.ts`
- Create: `frontend/src/features/auth/tokenStorage.ts`
- Create: `frontend/src/features/auth/tokenStorage.test.ts`
- Create: `frontend/src/features/auth/authApi.ts`
- Create: `frontend/src/features/auth/authApi.test.ts`

**Interfaces:**
- Produces `AuthTokens`, `User`, `LoginInput`, and `RegisterInput` types.
- Produces `tokenStorage.read()`, `tokenStorage.write(tokens)`, and `tokenStorage.clear()`.
- Produces `authApi.register`, `login`, `currentUser`, `refresh`, and `logout`.

- [ ] Write a failing storage test proving both tokens round-trip under one versioned key and malformed JSON is cleared.
- [ ] Run the test and confirm failure because `tokenStorage.ts` does not exist.
- [ ] Implement the smallest storage adapter using `localStorage` and one key `personal-agent.auth.v1`.
- [ ] Run the storage tests and confirm they pass.
- [ ] Write failing API-adapter tests with an Axios mock adapter proving exact methods, paths, bodies, and Bearer header for `/users/me`.
- [ ] Implement one Axios instance with base URL `/api` and the five typed methods.
- [ ] Run all frontend tests and commit as `feat: add frontend authentication adapters`.

### Task 3: Implement Session State and Route Protection

**Files:**
- Create: `frontend/src/features/auth/authStore.ts`
- Create: `frontend/src/features/auth/authStore.test.ts`
- Modify: `frontend/src/router/index.ts`
- Create: `frontend/src/router/router.test.ts`

**Interfaces:**
- Produces Pinia store state `status`, `user`, `tokens`, `error`.
- Produces actions `login`, `register`, `restoreSession`, and `logout`.
- Route metadata `requiresAuth` protects `/` and redirects anonymous users to `/login`.

- [ ] Write failing store tests for successful login, failed login, saved-session restore, one refresh followed by `/me`, failed refresh cleanup, and logout cleanup.
- [ ] Run the tests and verify the store is missing.
- [ ] Implement the store without a generic Axios retry interceptor.
- [ ] Run store tests and confirm pass.
- [ ] Write failing router tests for anonymous redirect and authenticated access.
- [ ] Implement the minimal navigation guard using store status.
- [ ] Run the complete frontend suite and commit as `feat: add frontend authentication state`.

### Task 4: Build Accessible Login, Registration, and Home Views

**Files:**
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/LoginView.test.ts`
- Create: `frontend/src/views/RegisterView.vue`
- Create: `frontend/src/views/RegisterView.test.ts`
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/views/HomeView.test.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/styles/main.css`

**Interfaces:**
- `/login` accepts username or email and password.
- `/register` accepts username, email, password, and display name.
- `/` displays the authenticated user and a logout action.

- [ ] Write failing component tests for visible labels, disabled submit while pending, inline backend error, navigation after success, and logout.
- [ ] Run tests and verify the views are missing.
- [ ] Implement semantic forms using `label`, `autocomplete`, `aria-live`, and native required/minlength constraints.
- [ ] Implement a flat responsive layout with teal design tokens, no gradients, visible focus rings, and `prefers-reduced-motion` support.
- [ ] Run component tests at jsdom size and verify success.
- [ ] Run `npm run test -- --run` and `npm run build`.
- [ ] Commit as `feat: add login and registration pages`.

### Task 5: Integrate, Self-Test, and Report

**Files:**
- Modify: `README.md`
- Modify: `docs/progress/2026-08-23.md`
- Create: `docs/testing/login-ui-test-cases.md`

**Interfaces:**
- Produces reproducible startup commands and manual acceptance cases.

- [ ] Start the existing Spring Boot backend with the ignored local profile from the authentication worktree.
- [ ] Start Vite from the login worktree and verify the proxy reaches the backend.
- [ ] Exercise registration, login, reload restore, logout, invalid credentials, duplicate registration, and narrow-screen layout.
- [ ] Run backend Maven tests, frontend Vitest tests, TypeScript build, `git diff --check`, and tracked-secret scan.
- [ ] Document test cases with expected results and update the daily progress report with evidence only.
- [ ] Commit as `docs: report login UI verification` and push `feature/login-ui`.
