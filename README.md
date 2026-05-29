# HackerPrank

A local proof of concept for a LeetCode/HackerRank-style coding practice platform.

For project memory, design decisions, and future-chat handoff context, see `PROJECT_NOTES.md`.
For the product north star and iteration roadmap, see `docs/PRODUCT_PLAN.md`.
For coding-agent operating instructions and repeatable workflows, start with the repo-scoped `AGENTS.md`, then see `docs/agentic/AGENTS.md` and `docs/agentic/SKILLS.md`.

## Shape of the App

- `frontend/`: React + TypeScript + Vite
- `backend/`: Spring Boot API that serves problems, creates generated drafts, publishes problems, runs submissions, and persists platform data
- `docker-compose.yml`: local PostgreSQL database plus an optional full-stack container setup
- `.github/workflows/ci-cd.yml`: GitHub Actions checks and image publishing

The first version intentionally uses stdin/stdout problems. That keeps the runner simple while still teaching the important pieces: APIs, DTOs, process execution, timeouts, test results, and frontend state.

Failed submissions can request a focused tutor hint from the saved run and ask short submission-scoped follow-up questions. The default tutor implementation is deterministic and intentionally nudge-oriented: visible sample failures can mention visible expected vs actual output, while hidden-only failures stay generic. OpenAI-backed or Anthropic-backed tutor hints and follow-ups can be enabled behind the same API with a strict safe-context boundary.

The generator panel captures topic, difficulty, target concepts, constraints/notes, and interview style. AI-backed generators use those controls in the prompt, while deterministic fallback templates keep using topic and difficulty for selection and preserve the richer controls in draft generation metadata.

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

Local secrets can live in a root `.env` file, which is ignored by git. Start from the checked-in example:

```sh
cp .env.example .env
```

Paste your real API key into `.env`, then load it before starting the backend:

```sh
set -a
source ../.env
set +a
mvn spring-boot:run
```

OpenAI-backed generation is opt-in. The backend keeps deterministic templates as the default and fallback so local development and tests never require network access:

```sh
export HACKERPRANK_GENERATOR_PROVIDER=openai
export OPENAI_API_KEY=...
```

Optional OpenAI generator settings:

```sh
export HACKERPRANK_OPENAI_MODEL=gpt-5-mini
export HACKERPRANK_OPENAI_RESPONSES_URL=https://api.openai.com/v1/responses
export HACKERPRANK_OPENAI_TIMEOUT_SECONDS=45
export HACKERPRANK_OPENAI_MAX_OUTPUT_TOKENS=6000
```

The OpenAI path uses the Responses API with structured JSON output, then passes the generated draft through the same Python/Java reference-solution validator before persistence. If a schema-valid draft fails validation, the backend attempts one repair call with the validation error before falling back. If OpenAI is disabled, missing `OPENAI_API_KEY`, generation fails, or repair fails, the backend falls back to deterministic templates.

Anthropic-backed Claude generation is also opt-in and uses the same generated-problem contract, validator, one-shot repair loop, and deterministic fallback:

```sh
export HACKERPRANK_GENERATOR_PROVIDER=anthropic
export ANTHROPIC_API_KEY=...
```

Optional Anthropic generator settings:

```sh
export HACKERPRANK_ANTHROPIC_MODEL=claude-sonnet-4-20250514
export HACKERPRANK_ANTHROPIC_MESSAGES_URL=https://api.anthropic.com/v1/messages
export HACKERPRANK_ANTHROPIC_VERSION=2023-06-01
export HACKERPRANK_ANTHROPIC_TIMEOUT_SECONDS=45
export HACKERPRANK_ANTHROPIC_MAX_OUTPUT_TOKENS=6000
```

OpenAI-backed tutor hints are also opt-in. They reuse `OPENAI_API_KEY`, but require the tutor provider flag so local development stays deterministic by default:

```sh
export HACKERPRANK_TUTOR_PROVIDER=openai
export OPENAI_API_KEY=...
```

Optional tutor settings:

```sh
export HACKERPRANK_TUTOR_OPENAI_MODEL=gpt-5-mini
export HACKERPRANK_TUTOR_OPENAI_RESPONSES_URL=https://api.openai.com/v1/responses
export HACKERPRANK_TUTOR_OPENAI_TIMEOUT_SECONDS=30
export HACKERPRANK_TUTOR_OPENAI_MAX_OUTPUT_TOKENS=900
export HACKERPRANK_TUTOR_OPENAI_PROMPT_VERSION=openai-tutor-v1
```

Anthropic-backed Claude tutor hints and follow-up chat use the same safe context and fallback behavior:

```sh
export HACKERPRANK_TUTOR_PROVIDER=anthropic
export ANTHROPIC_API_KEY=...
```

Optional Anthropic tutor settings:

```sh
export HACKERPRANK_TUTOR_ANTHROPIC_MODEL=claude-sonnet-4-20250514
export HACKERPRANK_TUTOR_ANTHROPIC_MESSAGES_URL=https://api.anthropic.com/v1/messages
export HACKERPRANK_TUTOR_ANTHROPIC_VERSION=2023-06-01
export HACKERPRANK_TUTOR_ANTHROPIC_TIMEOUT_SECONDS=30
export HACKERPRANK_TUTOR_ANTHROPIC_MAX_OUTPUT_TOKENS=900
export HACKERPRANK_TUTOR_ANTHROPIC_PROMPT_VERSION=anthropic-tutor-v1
```

The tutor sends only public problem data, user code, compile output, visible failure details, recent tutor messages, and hidden-test counts. Hidden test names, inputs, expected outputs, actual outputs, and stderr stay server-side and are never placed in OpenAI or Anthropic tutor requests. If the model call fails or returns an unusable hint or reply, the deterministic tutor response is used instead.

Generated-problem eval fixtures live in `backend/src/test/resources/generated-problems/`. Add `valid-*.json` and `invalid-*.json` fixtures there when changing prompt versions, mapping, or validation rules so the backend test suite automatically captures generation quality regressions.

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
- Tutor chat is submission-scoped, but does not yet support explicit hint-level controls or solution unlocks.
- There is no auth or user account model yet.
- Submission history is global per problem, not user-scoped.
- Execution is synchronous; there is no worker queue yet.
- Deterministic problem templates record richer generator controls but do not yet vary problem content from them.

## Next Milestones

Use `docs/PRODUCT_PLAN.md` as the canonical roadmap.

1. Add a richer generated-draft quality panel.
2. Add generation variant controls and deeper prompt behavior.
3. Add user accounts so submission history can be user-scoped.
4. Move execution to a worker queue.
