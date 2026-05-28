# HackerPrank

A local proof of concept for a LeetCode/HackerRank-style coding practice platform.

For project memory, design decisions, and future-chat handoff context, see `PROJECT_NOTES.md`.
For coding-agent operating instructions and repeatable workflows, see `AGENTS.md` and `SKILLS.md`.

## Shape of the App

- `frontend/`: React + TypeScript + Vite
- `backend/`: Spring Boot API that serves problems, creates generated drafts, publishes problems, runs submissions, and persists platform data
- `docker-compose.yml`: local PostgreSQL database for persisted problems, drafts, and submissions

The first version intentionally uses stdin/stdout problems. That keeps the runner simple while still teaching the important pieces: APIs, DTOs, process execution, timeouts, test results, and frontend state.

## Local Commands

Prerequisites:

- Java 21
- Maven
- Node.js 20+
- Docker Desktop, or another Docker-compatible runtime with the `docker` CLI
- PostgreSQL through the included Docker setup

This machine is set up with Colima:

```sh
brew install docker colima
colima start --cpu 2 --memory 2 --disk 20
```

Frontend:

```sh
cd frontend
npm install
npm run dev
```

Database:

```sh
docker compose up -d postgres
```

If your Docker CLI does not include Compose v2 yet, either install the Compose plugin or use this equivalent local command:

```sh
docker run -d \
  --name hackerprank-postgres \
  -e POSTGRES_DB=hackerprank \
  -e POSTGRES_USER=hackerprank \
  -e POSTGRES_PASSWORD=hackerprank \
  -p 5432:5432 \
  -v hackerprank-postgres-data:/var/lib/postgresql/data \
  postgres:16-alpine
```

Backend:

```sh
cd backend
mvn spring-boot:run
```

The backend is configured for Spring Boot 3 and Java 21. It uses PostgreSQL by default with Flyway migrations and HikariCP pooling. Tests use H2 in PostgreSQL compatibility mode.

The submission runner uses Docker by default. Pull the runtime images once before running submissions:

```sh
docker pull python:3.12-alpine
docker pull eclipse-temurin:21-jdk-alpine
```

For local development without Docker, you can temporarily run the backend with:

```sh
mvn spring-boot:run -Dspring-boot.run.arguments=--hackerprank.runner.mode=local
```

Docker submissions run with no container network, memory/CPU/pid limits, dropped Linux capabilities, `no-new-privileges`, a read-only root filesystem, and a per-submission temporary workspace mounted at `/workspace`.

## Current Limitations

- Docker isolation is local-dev grade, not production hardened.
- Output matching is exact after trimming trailing whitespace.
- There is no auth or user account model yet.
- Submission history has backend APIs, but no frontend history screen yet.
- Execution is synchronous; there is no worker queue yet.

## Next Milestones

1. Add an OpenAI-backed generator behind the current draft flow.
2. Add frontend submission history and result detail views.
3. Add richer generator controls and stored generation metadata.
4. Move execution to a worker queue.
