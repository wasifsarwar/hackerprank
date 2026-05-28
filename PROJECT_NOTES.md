# HackerPrank Project Notes

Last updated: 2026-05-28

This is the living handoff file for HackerPrank. Future chats should read this first, then run `git status --short --branch`, then check `README.md` for setup commands.

Agentic development docs:

- `docs/agentic/AGENTS.md` - operating instructions for coding agents.
- `docs/agentic/SKILLS.md` - repeatable project workflows for agents.

Agent memory rule:

- Agents must update this file after meaningful implementation or workflow changes so a future chat can reconstruct where the project left off from the repo alone.
- Agents should start new sessions by reading `PROJECT_NOTES.md`, `docs/agentic/AGENTS.md`, `docs/agentic/SKILLS.md`, and `README.md`, then checking `git status --short --branch` and `git log -5 --oneline`.

## Project Goal

HackerPrank is a local LeetCode/HackerRank-style coding practice platform.

The long-term goal is an agentic tutor that can generate original interview-style coding problems on demand, including statements, starter code, reference solutions, and test cases. The user wants this to be a learning project, especially for staying sharp with Spring Boot while still building a useful frontend.

## Current Stack

- Frontend: React, TypeScript, Vite, Monaco editor
- Backend: Spring Boot 3.4.6, Java 21 target
- Persistence: PostgreSQL 16, Flyway migrations, Spring JDBC, HikariCP
- Backend tests: H2 in PostgreSQL compatibility mode
- Runner: Python and Java submissions executed through a sandbox abstraction
- Default sandbox: Docker CLI, currently backed locally by Colima
- Repository: Git repo at `/Users/wasifsiddique/Desktop/hackerprank`
- Main branch: `main`, used as the PR review target
- Feature workflow: create `codex/<short-feature-name>` branches for feature work; do not commit features directly to `main`

## Commit Map

- `73a0e33` - Build HackerPrank proof of concept
- `438df5f` - Replace code textarea with Monaco editor
- `8a5d158` - Run submissions in Docker sandboxes
- `1b06269` - Add generated problem endpoint
- `0e44f06` - Add project handoff notes
- `956d2c6` - Add agent workflow docs
- `c5dc1bb` - Strengthen agent handoff rules
- Current session - Add database-backed persistence and queryable submissions
- Current session - Document feature-branch workflow for future work
- Current session - Add frontend submission history and result detail views
- Current session - Add container images, full-stack Compose, and GitHub Actions CI/CD
- Current session - Guard frontend problem, run, and submission-history requests against stale responses after problem changes
- Current session - Move agent workflow docs into `docs/agentic/` and begin componentizing the frontend

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
- Generator panel with topic input and difficulty selector
- Generated draft preview with publish and discard actions
- Submission history tab for the selected published problem
- Persisted submission detail view with saved code, compile output, and per-test results
- Load-code action for restoring a saved submission into Monaco

Important files:

- `frontend/src/App.tsx` - app-level state orchestration and API-driven workflows
- `frontend/src/components/ProblemRail.tsx` - sidebar, generator controls, draft actions, and problem list
- `frontend/src/components/ProblemStatement.tsx` - problem statement, formats, constraints, and examples
- `frontend/src/components/CodingPanel.tsx` - language toolbar, Monaco editor, and results layout
- `frontend/src/components/ResultsPanel.tsx` - current run results and persisted submission history
- `frontend/src/components/TestResults.tsx` - per-test result rendering
- `frontend/src/api.ts` - API client functions
- `frontend/src/types.ts` - shared TypeScript API shapes
- `frontend/src/ui.ts` - shared UI constants
- `frontend/src/format.ts` - display formatting helpers
- `frontend/src/styles.css` - current app styling
- `frontend/vite.config.ts` - Vite dev server and `/api` proxy to backend

Current frontend limitation:

- The generator UI supports topic and difficulty, but not richer constraints such as target concepts, company style, time limits, or prompt notes.
- Submission history is global per problem because there are no user accounts yet.
- Async problem, run, and history requests now use request guards so stale responses cannot overwrite the currently selected problem's result or history state.

### Backend

Path: `backend/`

The backend is a Spring Boot API that serves problems, generates problems, and runs submissions.

Important packages:

- `com.hackerprank.problems`
- `com.hackerprank.submissions`
- `com.hackerprank.config`

Important files:

- `ProblemController.java` - problem list/detail/generate endpoints
- `ProblemRepository.java` - JDBC problem repository with optimized summary projections
- `ProblemDraft.java` - private generated draft model with reference solution
- `ProblemDraftRepository.java` - JDBC generated draft store
- `PublicProblemDraft.java` - public draft response that hides reference solutions and hidden tests
- `ProblemGeneratorService.java` - deterministic generated-problem templates and validation
- `SubmissionController.java` - submission endpoint
- `SubmissionRepository.java` - JDBC submission and test-result persistence
- `SubmissionService.java` - prepares code, runs test cases, computes status
- `DockerSandboxRunner.java` - Docker-based sandbox implementation
- `LocalSandboxRunner.java` - local process runner for tests/dev
- `application.properties` - default database, Flyway, pool, and runner config
- `db/migration/V1__initial_persistence.sql` - normalized persistence schema and indexes

## API Surface

### Problems

`GET /api/problems`

Returns problem summaries.

`GET /api/problems/{id}`

Returns a public problem without hidden test cases.

`POST /api/problems/drafts`

Request body:

```json
{
  "topic": "stacks",
  "difficulty": "Medium"
}
```

Returns a validated `PublicProblemDraft`.

Current behavior:

- Creates a database-backed draft, not a public problem.
- Validates the generated draft by running its private Python reference solution through `SubmissionService`.
- Public draft responses include `id`, `topic`, `difficulty`, `validationStatus`, `createdAt`, and `problem`.
- Public draft responses do not expose hidden test cases or reference solutions.

`GET /api/problems/drafts/{id}`

Returns a public draft preview if the draft still exists.

`POST /api/problems/drafts/{id}/publish`

Marks the draft problem as `PUBLISHED` and removes only the draft metadata.

`DELETE /api/problems/drafts/{id}`

Discards a draft.

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

- Kept as a compatibility shortcut.
- Internally creates a validated draft, publishes it immediately, then removes draft metadata.
- Defaults to topic `arrays` and difficulty `Easy` if omitted.
- Accepts difficulty `Easy`, `Medium`, or `Hard`.
- Routes topic text to deterministic templates:
  - arrays/default -> `Signal Peaks`
  - string/map/hash/count -> `First Solo Word`
  - stack/bracket/parentheses -> `Bracket Balance`

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

Run responses now also include:

- `submissionId`
- `createdAt`

`GET /api/submissions`

Optional query params:

- `problemId`
- `limit` (bounded to `1..100`; default `25`)

Returns recent `SubmissionSummary` rows without loading source code or per-test output.

`GET /api/submissions/{id}`

Returns a persisted `SubmissionDetail` with code, compile output, and per-test results.

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

`ProblemDraft` includes private validation data:

- `id`
- `topic`
- `difficulty`
- `problem`
- `referenceSolution`
- `validationStatus`
- `createdAt`

`PublicProblemDraft` intentionally omits the reference solution and hidden test cases.

Seed problems:

- `add-a-pair`
- `most-frequent-word`

Problems, generated drafts, private reference solutions, validation metadata, submissions, and submission test results are stored in PostgreSQL. Published generated problems and submitted runs survive backend restarts.

### Containerization And CI/CD

Current container files:

- `backend/Dockerfile` builds the Spring Boot app with Maven on Java 21, then runs it on Eclipse Temurin 21 Alpine with Python 3 installed for local runner support.
- `frontend/Dockerfile` builds the Vite app with Node.js 22 and serves it through unprivileged Nginx on port `8080`.
- `frontend/nginx.conf` serves the frontend SPA and proxies `/api/` to the backend container.
- `.dockerignore` keeps build output, dependencies, logs, and local submission workspaces out of Docker build contexts.
- `docker-compose.yml` can run PostgreSQL alone or the full PostgreSQL/backend/frontend stack.
- Dockerfiles intentionally avoid BuildKit-only features so they can build with a plain Docker CLI even when the local `buildx` component is missing.

Important tradeoff:

- Host-based backend development still defaults to the Docker sandbox runner.
- The backend container image sets `HACKERPRANK_RUNNER_MODE=local` so published images, CI, and local smoke tests do not need to mount the host Docker socket or solve nested workspace mounts.
- The backend container still supports both submission languages in local-runner mode: Java uses the Temurin 21 JDK in the image, and Python uses the installed `python3` runtime.
- This is acceptable for proof-of-concept smoke testing, but production-grade execution should move to a dedicated worker/sandbox service.

GitHub Actions workflow:

- `.github/workflows/ci-cd.yml` runs on pull requests and pushes to `main`.
- `backend-test` runs `mvn -B test` with Java 21.
- `frontend-build` runs `npm ci` and `npm run build` with Node.js 22.
- `compose-smoke` builds and starts the Compose stack, then curls backend and frontend endpoints.
- `publish-images` runs only on pushes to `main` and publishes backend/frontend images to GHCR.

## Persistence And Query Notes

The first persistence implementation intentionally uses Spring JDBC rather than JPA so the SQL shape stays visible while the data model is still small and educational.

Tables are normalized around the main read/write paths:

- `problems`
- `problem_tags`
- `problem_constraints`
- `problem_examples`
- `problem_test_cases`
- `problem_starter_code`
- `problem_private_artifacts`
- `problem_drafts`
- `submissions`
- `submission_test_results`

Performance choices made so far:

- Problem list uses `ProblemRepository.findAllSummaries()` and does not hydrate descriptions, examples, test cases, or starter code.
- Problem list tags are loaded with one batched `IN` query, not one query per problem.
- Submission list endpoints return summaries and avoid loading code or per-test output.
- Submission detail loads per-test results only for one submission.
- Indexes cover problem listing/filtering, tags, hidden-test lookup, draft lookup, recent submissions, per-problem submissions, status/language submission filters, and submission test-result lookup.
- HikariCP pool defaults are configurable through `HACKERPRANK_DATABASE_*` environment variables.

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

Database startup:

```sh
docker compose up -d postgres
```

If the local Docker CLI does not have Compose v2, use the fallback command in `README.md`. On 2026-05-27, this machine's `docker` binary did not expose `docker compose`; a real PostgreSQL smoke test was run with a temporary `postgres:16-alpine` container on port `55432`.

The repo has `.java-version` set to `21`, and Maven targets Java 21 through `backend/pom.xml`.

Common local commands:

```sh
docker compose up -d postgres
```

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

- 10 tests passed

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

Draft flow smoke test:

```sh
node - <<'NODE'
const base = 'http://127.0.0.1:5173';
const draftResponse = await fetch(`${base}/api/problems/drafts`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ topic: 'stacks', difficulty: 'Medium' })
});
const draft = await draftResponse.json();
console.log(draftResponse.status, draft.id, draft.problem.id, draft.validationStatus);
const listBefore = await (await fetch(`${base}/api/problems`)).json();
console.log(listBefore.some((item) => item.id === draft.problem.id));
const publishResponse = await fetch(`${base}/api/problems/drafts/${draft.id}/publish`, { method: 'POST' });
const published = await publishResponse.json();
console.log(publishResponse.status, published.id);
const listAfter = await (await fetch(`${base}/api/problems`)).json();
console.log(listAfter.some((item) => item.id === published.id));
NODE
```

Expected:

- Draft creation returns `200`
- Draft has `VALIDATED` status
- Problem is not listed before publish
- Publish returns `200`
- Problem is listed after publish

Check no leftover sandbox containers:

```sh
docker ps -a --filter name=hackerprank --format '{{.Names}}'
```

Expected:

- No output

Real PostgreSQL smoke result from 2026-05-27:

- Flyway migration applied successfully to PostgreSQL 16.
- Seed problem summaries loaded from PostgreSQL.
- A generated draft stayed hidden until publish.
- Published generated problem survived a backend restart.
- A submission returned `submissionId` and `createdAt`.
- The same submission detail was fetchable after backend restart with all 4 test results.

## Browser Automation Note

The in-app browser automation connector is currently available.

Healthy state from automation:

```json
[
  {
    "name": "Codex In-app Browser",
    "type": "iab"
  }
]
```

Latest UI verification used the browser connector to confirm the generator controls render, a `stacks`/`Medium` draft previews as `Bracket Balance`, and publishing adds the problem to the problem list.

Latest UI verification on 2026-05-28 confirmed the submission history flow in the in-app browser:

- Submit creates a persisted result.
- The History tab refreshes and shows the saved run.
- Clicking the saved run loads persisted code and per-test details.
- Load Code switches back to the current editor view with the saved code.

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

PostgreSQL + Flyway + JDBC persistence:

- PostgreSQL matches the likely production database direction better than an embedded-only store.
- Flyway gives explicit, reviewable schema history.
- Spring JDBC keeps query shape transparent while the user learns Spring persistence and while performance matters are still easy to reason about.
- Read projections are preferred for list endpoints so large text/code/test-output fields stay off hot paths.

## Known Limitations

- Generated-problem templates are deterministic and limited.
- Generator controls only cover topic and difficulty.
- No user accounts or sessions.
- Submission history is not user-scoped yet.
- No worker queue.
- No full generated-prompt/model audit trail yet beyond topic, validation status, and reference solution storage.
- Output matching only trims trailing whitespace.
- Docker sandbox is local-dev grade, not production hardened.
- No rate limiting or abuse controls.
- No OpenAI integration yet.

## Recommended Next Milestones

1. Introduce an OpenAI-backed generator behind `ProblemGeneratorService`.
2. Define a strict JSON schema for generated problem drafts.
3. Store prompt text, model id, generation parameters, and validation diagnostics.
4. Add Java reference-solution validation in addition to Python.
5. Add richer generator controls for concepts, constraints, and interview style.
6. Add user accounts and user-scoped submission history.
7. Move execution to a worker queue.
8. Harden sandboxing before any remote or multi-user deployment.

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
- How the change was verified
- New commands or setup steps
- New endpoints or data shapes
- Any broken assumptions
- Suggested next step

Do this before the final response for non-trivial implementation work. The goal is that opening a fresh chat never loses the thread.

Keep the README focused on setup and usage. Keep this file focused on project memory and handoff context.
