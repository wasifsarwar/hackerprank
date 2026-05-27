# HackerPrank Project Notes

Last updated: 2026-05-27

This is the living handoff file for HackerPrank. Future chats should read this first, then run `git status --short --branch`, then check `README.md` for setup commands.

## Project Goal

HackerPrank is a local LeetCode/HackerRank-style coding practice platform.

The long-term goal is an agentic tutor that can generate original interview-style coding problems on demand, including statements, starter code, reference solutions, and test cases. The user wants this to be a learning project, especially for staying sharp with Spring Boot while still building a useful frontend.

## Current Stack

- Frontend: React, TypeScript, Vite, Monaco editor
- Backend: Spring Boot 3.4.6, Java 21 target
- Runner: Python and Java submissions executed through a sandbox abstraction
- Default sandbox: Docker CLI, currently backed locally by Colima
- Repository: Git repo at `/Users/wasifsiddique/Desktop/hackerprank`
- Main branch: `main`

## Commit Map

- `73a0e33` - Build HackerPrank proof of concept
- `438df5f` - Replace code textarea with Monaco editor
- `8a5d158` - Run submissions in Docker sandboxes
- `1b06269` - Add generated problem endpoint

## Current Application Shape

### Frontend

Path: `frontend/`

The frontend is a Vite React app. It currently provides:

- Problem rail/sidebar
- Problem statement view
- Language tabs for Python and Java
- Monaco code editor
- Run samples and submit buttons
- Results panel with per-test output
- Generate Problem button wired to `POST /api/problems/generate`

Important files:

- `frontend/src/App.tsx` - main UI and state orchestration
- `frontend/src/api.ts` - API client functions
- `frontend/src/types.ts` - shared TypeScript API shapes
- `frontend/src/styles.css` - current app styling
- `frontend/vite.config.ts` - Vite dev server and `/api` proxy to backend

Current frontend limitation:

- The Generate Problem button sends a fixed payload: `{ topic: "arrays", difficulty: "Easy" }`.
- There are not yet UI controls for topic, difficulty, concepts, or style.

### Backend

Path: `backend/`

The backend is a Spring Boot API that serves problems, generates problems, and runs submissions.

Important packages:

- `com.hackerprank.problems`
- `com.hackerprank.submissions`
- `com.hackerprank.config`

Important files:

- `ProblemController.java` - problem list/detail/generate endpoints
- `ProblemRepository.java` - in-memory problem store
- `ProblemGeneratorService.java` - deterministic generated-problem templates and validation
- `SubmissionController.java` - submission endpoint
- `SubmissionService.java` - prepares code, runs test cases, computes status
- `DockerSandboxRunner.java` - Docker-based sandbox implementation
- `LocalSandboxRunner.java` - local process runner for tests/dev
- `application.properties` - default runner config

## API Surface

### Problems

`GET /api/problems`

Returns problem summaries.

`GET /api/problems/{id}`

Returns a public problem without hidden test cases.

`POST /api/problems/generate`

Request body:

```json
{
  "topic": "arrays",
  "difficulty": "Easy"
}
```

Returns a generated `PublicProblem`.

Current behavior:

- Defaults to topic `arrays` and difficulty `Easy` if omitted.
- Accepts difficulty `Easy`, `Medium`, or `Hard`.
- Routes topic text to deterministic templates:
  - arrays/default -> `Signal Peaks`
  - string/map/hash/count -> `First Solo Word`
  - stack/bracket/parentheses -> `Bracket Balance`
- Saves the generated problem into the in-memory repository.
- Runs an internal Python reference solution through `SubmissionService`.
- Deletes the generated problem if validation does not return `ACCEPTED`.

This endpoint is intentionally deterministic for now. The API shape is ready to become OpenAI-backed later.

### Submissions

`POST /api/submissions/run`

Request body:

```json
{
  "problemId": "add-a-pair",
  "language": "python",
  "code": "print(1)",
  "runHiddenTests": false
}
```

Supported languages:

- `python`
- `java`

Statuses:

- `ACCEPTED`
- `WRONG_ANSWER`
- `RUNTIME_ERROR`
- `COMPILE_ERROR`
- `TIME_LIMIT_EXCEEDED`

## Problem Model

`Problem` includes:

- `id`
- `title`
- `difficulty`
- `tags`
- `description`
- `inputFormat`
- `outputFormat`
- `constraints`
- `examples`
- `testCases`
- `starterCode`

`PublicProblem` intentionally omits hidden test cases.

Seed problems:

- `add-a-pair`
- `most-frequent-word`

Generated problems are currently stored in memory only. They disappear when the backend restarts.

## Runner And Sandbox Notes

The app started with local execution, then moved to Docker isolation.

Current default config in `backend/src/main/resources/application.properties`:

```properties
hackerprank.runner.mode=docker
hackerprank.runner.workspace-root=.hackerprank-submissions
hackerprank.runner.docker.python-image=python:3.12-alpine
hackerprank.runner.docker.java-image=eclipse-temurin:21-jdk-alpine
hackerprank.runner.docker.cpus=1
hackerprank.runner.docker.memory=256m
hackerprank.runner.docker.pids-limit=128
```

Docker runner properties:

- Uses `docker run --rm --interactive`
- Pulls missing images with `--pull missing`
- Disables network with `--network none`
- Applies CPU, memory, memory-swap, and pid limits
- Drops Linux capabilities with `--cap-drop ALL`
- Uses `--security-opt no-new-privileges`
- Uses read-only root filesystem
- Mounts only a temporary per-submission workspace at `/workspace`
- Adds `/tmp` as a small tmpfs
- Force-removes timed-out containers

This is good for local development, but it is not production-grade isolation.

## Local Environment Notes

The user asked whether Docker Desktop was required. We chose Colima instead.

Installed/used locally:

```sh
brew install docker colima
colima start --cpu 2 --memory 2 --disk 20
docker pull python:3.12-alpine
docker pull eclipse-temurin:21-jdk-alpine
```

The repo has `.java-version` set to `21`, and Maven targets Java 21 through `backend/pom.xml`.

Common local commands:

```sh
cd backend
mvn spring-boot:run
```

```sh
cd frontend
npm run dev
```

Frontend dev URL:

```text
http://localhost:5173/
```

Backend URL:

```text
http://localhost:8080/
```

The Vite dev server proxies `/api` to `http://127.0.0.1:8080`.

## Verification Commands

Backend tests:

```sh
cd backend
mvn test
```

Last known result:

- 8 tests passed

Frontend build:

```sh
cd frontend
npm run build
```

Last known result:

- TypeScript compile passed
- Vite production build passed

Live endpoint smoke test:

```sh
curl -sS -w '\nHTTP %{http_code}\n' \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{"topic":"arrays","difficulty":"Easy"}' \
  http://127.0.0.1:5173/api/problems/generate
```

Expected:

- `HTTP 200`
- A generated problem such as `signal-peaks-<id>`

Check no leftover sandbox containers:

```sh
docker ps -a --filter name=hackerprank --format '{{.Names}}'
```

Expected:

- No output

## Browser Automation Note

The user had the Codex in-app browser open at `http://localhost:5173/`, but the automation connector did not attach.

Observed from automation:

```json
[]
```

That means the Browser plugin registry did not expose an `iab` target. The app itself was still verified through curl and builds.

Troubleshooting steps suggested:

1. Close and reopen the in-app browser panel.
2. Reload the Codex app/window.
3. Confirm the Browser plugin/tool is enabled.
4. Accept any permission prompt like "allow automation" or "connect browser".
5. Open a fresh in-app browser tab to `http://localhost:5173/`.
6. Ask Codex to retry the connector check.

Healthy state:

- `agent.browsers.list()` should show an `iab` browser target.

## Design Decisions So Far

Spring Boot backend:

- Chosen because the user wants to stay sharp with Spring for work.
- Good fit for typed DTOs, controllers, configuration, and testability.

React + TypeScript frontend:

- Good fit for a rich interactive coding environment.
- Monaco gives a real editor feel without building editor behavior ourselves.

stdin/stdout problem format:

- Keeps the runner simple.
- Matches HackerRank-style exercises.
- Avoids early complexity around function signatures, language-specific harnesses, and custom test adapters.

Docker/Colima execution:

- Avoids requiring Docker Desktop.
- Gives a reasonable local sandbox story.
- Keeps the same `docker` CLI contract if the runtime changes later.

Deterministic generator first:

- Gives us a stable endpoint and validation flow.
- Lets us test problem creation without spending tokens or debugging LLM variability.
- Provides a clear service boundary for adding OpenAI generation later.

## Known Limitations

- Problem storage is in memory only.
- Generated problems disappear on backend restart.
- Generated-problem templates are deterministic and limited.
- The frontend generate button does not yet let the user choose topic or difficulty.
- No user accounts or sessions.
- No submission history.
- No database.
- No worker queue.
- No persisted audit trail of generated problem prompts, reference solutions, or validation results.
- Output matching only trims trailing whitespace.
- Docker sandbox is local-dev grade, not production hardened.
- No rate limiting or abuse controls.
- No OpenAI integration yet.

## Recommended Next Milestones

1. Add frontend controls for topic and difficulty.
2. Add a generated-problem draft/review flow before publishing problems.
3. Add persistence for problems and submissions.
4. Introduce an OpenAI-backed generator behind `ProblemGeneratorService`.
5. Define a strict JSON schema for generated problem drafts.
6. Store reference solutions separately from public problem data.
7. Add Java reference-solution validation in addition to Python.
8. Add submission history and result detail pages.
9. Move execution to a worker queue.
10. Harden sandboxing before any remote or multi-user deployment.

## Future OpenAI Generator Notes

The generator should eventually produce structured data, not free-form markdown.

Likely generated draft shape:

- title
- difficulty
- tags
- description
- input format
- output format
- constraints
- examples
- visible test cases
- hidden test cases
- Python starter code
- Java starter code
- Python reference solution
- Java reference solution
- explanation of intended technique
- topic/concept metadata

The backend should validate generated drafts before publishing:

- Schema validation
- Reference solution passes all generated tests
- Hidden tests include edge cases
- Starter code compiles/runs enough to fail predictably
- Problem statement does not leak hidden test answers
- No web-derived or copied problem wording

## How To Keep These Notes Useful

When making a meaningful project change, update this file with:

- What changed
- Why the decision was made
- New commands or setup steps
- New endpoints or data shapes
- Any broken assumptions
- Suggested next step

Keep the README focused on setup and usage. Keep this file focused on project memory and handoff context.

