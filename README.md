# HackerPrank

A local proof of concept for a LeetCode/HackerRank-style coding practice platform.

For project memory, design decisions, and future-chat handoff context, see `PROJECT_NOTES.md`.
For coding-agent operating instructions and repeatable workflows, start with the repo-scoped `AGENTS.md`, then see `docs/agentic/AGENTS.md` and `docs/agentic/SKILLS.md`.

## Shape of the App

- `frontend/`: React + TypeScript + Vite
- `backend/`: Spring Boot API that serves problems, creates generated drafts, publishes problems, runs submissions, and persists platform data
- `docker-compose.yml`: local PostgreSQL database plus an optional full-stack container setup
- `.github/workflows/ci-cd.yml`: GitHub Actions checks and image publishing

The first version intentionally uses stdin/stdout problems. That keeps the runner simple while still teaching the important pieces: APIs, DTOs, process execution, timeouts, test results, and frontend state.

The generator panel captures topic, difficulty, target concepts, constraints/notes, and interview style. Until the OpenAI-backed generator lands, the deterministic templates keep using topic and difficulty for selection while preserving the richer controls in draft generation metadata.

Generated draft previews show safe metadata such as provider, model id, prompt version, validation summary, and intended technique. Private generation details such as prompt text, reference solutions, hidden tests, raw validation errors, and raw parameters stay server-side.

## Local Commands

Prerequisites:

- Java 21
- Maven
- Node.js 20+
- Docker Desktop, or another Docker-compatible runtime with the `docker` CLI and Compose v2
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

Full stack in containers:

```sh
docker compose up -d --build postgres backend frontend
```

The containerized frontend is available at `http://127.0.0.1:5173`, and it proxies `/api` requests to the backend service. The backend is also exposed directly at `http://127.0.0.1:8080`.

The backend container image defaults to `hackerprank.runner.mode=local`. That keeps the published image, local smoke tests, and CI self-contained without mounting the host Docker socket. Host-based backend development still uses the Docker submission runner by default through `backend/src/main/resources/application.properties`.

The backend container still supports both submission languages: Java submissions use the Temurin 21 JDK in the image, and Python submissions use the installed `python3` runtime.

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

## CI/CD

Pull requests run GitHub Actions checks for:

- Backend tests with Java 21 and Maven
- Frontend TypeScript/Vite build with Node.js 22
- A Docker Compose smoke test that builds and starts PostgreSQL, backend, and frontend containers

Pushes to `main` run the same checks and then publish backend and frontend images to GitHub Container Registry:

- `ghcr.io/wasifsarwar/hackerprank/backend`
- `ghcr.io/wasifsarwar/hackerprank/frontend`

## Current Limitations

- Docker isolation is local-dev grade, not production hardened.
- Backend containers use the local runner by default; production-grade execution should move to a dedicated sandbox worker.
- Output matching is exact after trimming trailing whitespace.
- There is no auth or user account model yet.
- Submission history is global per problem, not user-scoped.
- Execution is synchronous; there is no worker queue yet.
- Deterministic problem templates record richer generator controls but do not yet vary problem content from them.

## Next Milestones

1. Add an OpenAI-backed generator behind the current draft flow.
2. Add user accounts so submission history can be user-scoped.
3. Move execution to a worker queue.
