# Personal Workspace

A lightweight personal AI agent for structured weekly reports and work summaries.

## Project Status

Registration/login, the Vue workspace, structured weekly reports, DOCX text extraction,
and quarterly/yearly work summaries are implemented.

## First-Phase Scope

- Multi-user registration and login
- Agent-centered web interface
- Three-field weekly-report creation and editing
- Year/month report history and detail view
- DOCX extraction with configurable AI classification
- Quarterly/yearly summaries with editable 0–100 score
- One configurable OpenAI-compatible chat provider

## Architecture

- Java 21 and Spring Boot 3 modular monolith
- Vue 3, TypeScript, and Vite
- MySQL 8 with Flyway
- MyBatis-Plus
Course recording, Redis, queues, object storage, Feishu integration, and the literature assistant are later phases.

## Documentation

- [System design](docs/superpowers/specs/2026-08-23-personal-agent-design.md)
- [Foundation and authentication implementation plan](docs/superpowers/plans/2026-08-23-foundation-auth-implementation.md)
- [Local development setup](docs/development/local-setup.md)
- [Frontend setup](frontend/README.md)
- [Login test cases](docs/testing/login-ui-test-cases.md)
- [Weekly report test cases](docs/testing/weekly-report-test-cases.md)
- [Daily progress](docs/progress/README.md)

## License

This project is licensed under the MIT License.
