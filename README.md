# Personal Workspace

A personal AI agent for course transcription, structured notes, and conversational weekly reports.

## Project Status

The authentication backend and Vue web interface are implemented. The current increment
provides registration, login, session restoration, refresh-token rotation, logout,
route protection, and a responsive personal workspace.

## First-Phase Scope

- Multi-user registration and login
- Agent-centered web interface
- Up to two hours of browser course recording
- Asynchronous speech transcription and structured course notes
- Conversational weekly-report material collection with optional DOCX files
- On-demand weekly-report generation, editing, storage, and DOCX export
- Configurable chat and speech-to-text providers

## Architecture

- Java 21 and Spring Boot 3 modular monolith
- Vue 3, TypeScript, and Vite
- MySQL 8 with Flyway
- MyBatis-Plus
- MinIO object storage
- Server-Sent Events for task progress

Redis, RabbitMQ, microservices, vector databases, Feishu integration, and the literature assistant are intentionally excluded from the first phase.

## Documentation

- [System design](docs/superpowers/specs/2026-08-23-personal-agent-design.md)
- [Foundation and authentication implementation plan](docs/superpowers/plans/2026-08-23-foundation-auth-implementation.md)
- [Local development setup](docs/development/local-setup.md)
- [Frontend setup](frontend/README.md)
- [Login test cases](docs/testing/login-ui-test-cases.md)
- [Daily progress](docs/progress/README.md)

## License

This project is licensed under the MIT License.
