# HackerPrank

A local proof of concept for a LeetCode/HackerRank-style coding practice platform.

For project memory, design decisions, and future-chat handoff context, see `PROJECT_NOTES.md`.

## Shape of the App

- `frontend/`: React + TypeScript + Vite
- `backend/`: Spring Boot API that serves problems and runs submissions

The first version intentionally uses stdin/stdout problems. That keeps the runner simple while still teaching the important pieces: APIs, DTOs, process execution, timeouts, test results, and frontend state.

## Local Commands

Prerequisites:

- Java 21
- Maven
- Node.js 20+
- Docker Desktop, or another Docker-compatible runtime with the `docker` CLI

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

Backend:

```sh
cd backend
mvn spring-boot:run
```

The backend is configured for Spring Boot 3 and Java 21.

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
- Problems are seeded in memory.
- There is no database or auth yet.

## Next Milestones

1. Add persistence for problems and submissions.
2. Add a generated-problem draft flow.
3. Add richer result inspection.
4. Move execution to a worker queue.
