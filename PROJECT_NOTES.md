# HackerPrank Project Notes

Last updated: 2026-05-28

This is the living handoff file for HackerPrank. Future chats should read this first, then run `git status --short --branch`, then check `README.md` for setup commands.

Agentic development docs:

- `docs/PRODUCT_PLAN.md` - product north star, end-state feature plan, iteration roadmap, and near-term PR backlog.
- `AGENTS.md` - repo-scoped entry point so agent instructions apply to the full repository.
- `docs/agentic/AGENTS.md` - operating instructions for coding agents.
- `docs/agentic/SKILLS.md` - repeatable project workflows for agents.

Agent memory rule:

- Agents must update this file after meaningful implementation or workflow changes so a future chat can reconstruct where the project left off from the repo alone.
- Agents should start new sessions by reading `PROJECT_NOTES.md`, `AGENTS.md`, `docs/agentic/AGENTS.md`, `docs/agentic/SKILLS.md`, and `README.md`, then checking `git status --short --branch` and `git log -5 --oneline`.

## Project Goal

HackerPrank is a local LeetCode/HackerRank-style coding practice platform.

The long-term goal is an agentic tutor that can generate original interview-style coding problems on demand, including statements, starter code, reference solutions, and test cases. The user wants this to be a learning project, especially for staying sharp with Spring Boot while still building a useful frontend.

The product roadmap now lives in `docs/PRODUCT_PLAN.md`. The current recommended next implementation branch is `codex/generation-variant-controls`.

## Product Discovery Snapshot

- First user: Wasif, and people like him preparing for technical interviews.
- Primary job: generate unique HackerRank-style practice problems that feel realistic for the target company or interview style; pattern teaching is secondary.
- Topic flow: a topic such as "sliding window" should produce a 3 to 5 problem progression from easy to hard, with adaptive session behavior later.
- Tutor tone: gentle and Socratic, like an interviewer judging technical problem-solving skill. Correctness matters, but the reasoning process matters more.
- Quality bar: originality, realistic feel, clean examples, hidden edge cases, and great explanations.
- Progress model: keep it lightweight and session-based for now.
- Near-term demo: generate a good problem live, solve it in an autocomplete-enabled editor, and keep the UI polished.
- Collaboration preference: explain Spring and React design decisions whenever practical so the project remains a learning experience.

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
- Current session - Restore a root `AGENTS.md` entry point so agent instructions remain repo-scoped while detailed docs live under `docs/agentic/`
- Current session - Guard generated draft actions so stale generate, publish, or discard responses cannot overwrite the active problem state
- Current session - Add a generated-problem contract, generation metadata storage, and Python/Java reference-solution validation
- Current session - Add an opt-in OpenAI-backed backend generator behind the generated-problem contract, with deterministic fallback
- Current session - Keep OpenAI validation inside the fallback boundary so schema-valid but incorrect model drafts fall back instead of returning 500s
- Current session - Add richer frontend generator controls for target concepts, constraints/notes, and interview style, and preserve them in draft generation metadata
- Current session - Render safe generated-draft metadata in the frontend problem statement
- Current session - Remove raw generation parameters and validation errors from the public draft metadata DTO
- Current session - Add generated-problem eval fixtures and parameterized validation tests for prompt and validator tuning
- Current session - Auto-discover generated-problem eval fixtures and validate generated examples against reference solutions
- Current session - Keep auto-discovered valid fixture assertions generic so non-array topics can join the eval corpus
- Current session - Let invalid generated-problem fixtures omit top-level sections so validator contract errors are asserted
- Current session - Add a one-shot OpenAI repair loop before deterministic fallback when validation fails
- Current session - Add deterministic tutor hints for failed submissions, exposed through backend API and frontend result/history panels
- Current session - Add a product plan and iteration roadmap for the agentic interview-practice end state
- Current session - Add opt-in OpenAI-backed tutor hints with deterministic fallback and hidden-test privacy guards
- Current session - Add persisted submission-scoped tutor follow-up chat with deterministic fallback and the same hidden-test privacy boundary
- Current session - Record first product-discovery decisions in the product plan and handoff notes
- Current session - Add a public draft quality DTO and frontend QA panel with validation checks, test counts, and repair-used signal
- Current session - Make local dev CORS origins and Vite API proxy target configurable for parallel branch smoke tests
- Current session - Add opt-in Anthropic/Claude support for generated problem drafts, tutor hints, and tutor follow-up chat
- Current session - Restore natural page scrolling by removing desktop-only internal statement/results scroll traps
- Current session - Add ignored local `.env` support plus a committed `.env.example` for Claude/OpenAI provider setup
- Current session - Add Generation Quality Studio v1 with persisted generation attempts, draft feedback tags/notes, and draft regeneration from critique
- Current session - Modernize the React UI with a studio-style shell, top command bar, lighter navigation rail, refreshed problem/QA/results panels, and responsive layout polish
- Current session - Backfill and lazily create generation attempts for drafts that existed before the generation-attempt audit table so feedback/publish/discard still work after migration
- Current session - Continue UI modernization toward the concept reference with rail search/difficulty filters, topbar actions, statement section tabs, editor runtime chip, and a bottom local-mode rail status
- Current session - Tighten the concept-matching UI pass with a collapsed generator drawer, vertical studio navigation, command shortcut/status controls, and split-pane app surfaces instead of floating cards
- Current session - Refine the modernized UI density with smaller typography, calmer control weights, a shorter editor viewport, and tighter rail/problem/result spacing after visual comparison with `concept.png`
- Current session - Correct the over-bold UI pass by lowering the global type scale, reducing 800/900 font weights across chrome/content/results, tightening example cards, and dropping Monaco to 13px
- Current session - Clean up the Studio shell by removing fake nav/topbar controls, moving draft generation into a first-class prompt composer, and adding Monaco v1 interview snippets/completions/hover plus reset/format editor actions
- Current session - Improve Monaco v1 suggestions with ranked Java/Python interview snippets, Java API/type completions, dot-context methods for common helpers/collections, and cleaner hover explanations

## Current Application Shape

### Frontend

Path: `frontend/`

The frontend is a Vite React app. It currently provides:

- Problem rail/sidebar
- Problem rail search and difficulty filters
- Studio generation composer with prompt, difficulty, target concepts, interview style, optional notes, and draft generation
- Studio-style top command bar with active problem/draft context
- Problem statement view
- Language tabs for Python and Java
- Monaco code editor
- Run samples and submit buttons
- Results panel with per-test output
- Tutor hint panel for failed current runs and saved submission-history details
- Opt-in OpenAI-backed tutor hints that send only public problem context, user code, compile output, visible failures, and hidden-test counts
- Opt-in Anthropic/Claude-backed tutor hints with the same safe-context boundary
- Submission-scoped tutor follow-up chat with persisted user/assistant messages
- Generator panel with topic, difficulty, target concepts, constraints/notes, and interview-style controls
- Generated draft preview with publish and discard actions
- Generated draft QA panel with provider, model, prompt version, repair signal, example/test counts, validation checks, and intended technique
- Generation Quality Studio v1 controls for saving draft feedback tags/notes and regenerating revised drafts from critique
- Submission history tab for the selected published problem
- Persisted submission detail view with saved code, compile output, and per-test results
- Load-code action for restoring a saved submission into Monaco

Important files:

- `frontend/src/App.tsx` - app-level state orchestration and API-driven workflows
- `frontend/src/components/GenerationComposer.tsx` - prompt-first draft generation controls
- `frontend/src/components/ProblemRail.tsx` - sidebar, draft actions, and problem list
- `frontend/src/components/ProblemStatement.tsx` - problem statement, formats, constraints, and examples
- `frontend/src/components/DraftMetadata.tsx` - safe generated draft QA panel for draft previews
- `frontend/src/components/CodingPanel.tsx` - language toolbar, Monaco editor, and results layout
- `frontend/src/components/ResultsPanel.tsx` - current run results and persisted submission history
- `frontend/src/components/TestResults.tsx` - per-test result rendering
- `frontend/src/components/TutorHintCard.tsx` - focused tutor nudges and follow-up chat for failed submissions
- `frontend/src/api.ts` - API client functions
- `frontend/src/types.ts` - shared TypeScript API shapes
- `frontend/src/ui.ts` - shared UI constants
- `frontend/src/editorIntelligence.ts` - Monaco v1 snippets, completion providers, and hover helpers for interview Java/Python
- `frontend/src/format.ts` - display formatting helpers
- `frontend/src/styles.css` - current app styling
- Current styling direction: concept-matched interview-practice studio with a light rail, vertical workspace navigation, collapsed draft generator drawer, white problem canvas, dark editor anchor, forest/amber action palette, compact 8px-radius controls, and document-level responsive scrolling
- `frontend/vite.config.ts` - Vite dev server and configurable `/api` proxy to backend

Current frontend limitation:

- The generator controls are persisted in draft metadata, but deterministic templates do not yet use them to alter the generated problem.
- Draft previews show safe generation metadata and quality signals, but intentionally omit prompt text, reference solutions, hidden tests, raw validation errors, and raw parameter JSON.
- Submission history is global per problem because there are no user accounts yet.
- Async problem, run, history, and draft requests now use request guards so stale responses cannot overwrite the currently selected problem, result, history, or draft state.
- The main UI now uses document-level vertical scrolling; the editor keeps a stable internal height while statement, QA, results, and history content extend the page naturally.

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
- `ProblemDraft.java` - private generated draft model with per-language reference solutions and metadata
- `ProblemDraftRepository.java` - JDBC generated draft store
- `GeneratedProblemSpec.java` - strict generated-problem contract used before drafts are persisted
- `GeneratedProblemValidator.java` - validates generated draft shape and runs Python/Java reference solutions
- `GenerationMetadata.java` - provider/model/prompt/validation metadata for generated drafts
- `PublicProblemDraft.java` - public draft response that hides reference solutions and hidden tests
- `GenerationAttemptRepository.java` - JDBC generation attempt and feedback persistence for draft quality review
- `ProblemGeneratorService.java` - generated-problem orchestration, AI-provider opt-in routing, deterministic fallback, and validation
- `OpenAiProblemGenerator.java` - Responses API request builder, structured JSON schema, response parser, and generated-problem mapper
- `OpenAiProblemGeneratorProperties.java` - OpenAI model, endpoint, timeout, token, and API key config
- `com.hackerprank.openai.JdkOpenAiTransport.java` - shared JDK `HttpClient` transport for OpenAI calls
- `AnthropicProblemGenerator.java` - Anthropic Messages API request builder, JSON response parser, and generated-problem mapper
- `AnthropicProblemGeneratorProperties.java` - Anthropic model, endpoint, API version, timeout, token, and API key config
- `com.hackerprank.anthropic.JdkAnthropicTransport.java` - shared JDK `HttpClient` transport for Anthropic calls
- `GeneratedProblemFixtureValidationTests.java` - fixture-driven generated-problem eval tests for valid drafts and expected contract failures
- `backend/src/test/resources/generated-problems/` - JSON fixture corpus for generated-problem validation evals
- `SubmissionController.java` - submission endpoint
- `SubmissionRepository.java` - JDBC submission and test-result persistence
- `SubmissionService.java` - prepares code, runs test cases, computes status
- `TutorHintService.java` - provider selection, deterministic fallback, and privacy-safe tutor hint generation from persisted submission details
- `OpenAiTutorHintGenerator.java` - Responses API request builder, structured JSON schema, response parser, and safe tutor hint mapper
- `AnthropicTutorHintGenerator.java` - Anthropic Messages API request builder, JSON response parser, and safe tutor hint mapper
- `TutorChatService.java` - persisted submission-scoped tutor follow-up orchestration
- `OpenAiTutorChatGenerator.java` - Responses API request builder and parser for follow-up tutor replies
- `AnthropicTutorChatGenerator.java` - Anthropic Messages API request builder and parser for follow-up tutor replies
- `TutorMessageRepository.java` - JDBC persistence for tutor chat messages
- `TutorHintContext.java` - safe context model that strips hidden test detail down to counts before model calls
- `OpenAiTutorProperties.java` - OpenAI tutor model, endpoint, timeout, token, prompt version, and API key config
- `TutorHintResponse.java` - public tutor hint DTO for failed-run nudges
- `DockerSandboxRunner.java` - Docker-based sandbox implementation
- `LocalSandboxRunner.java` - local process runner for tests/dev
- `application.properties` - default database, Flyway, pool, and runner config
- `db/migration/V1__initial_persistence.sql` - normalized persistence schema and indexes
- `db/migration/V2__generation_metadata.sql` - per-language reference solutions and generation audit metadata
- `db/migration/V3__tutor_messages.sql` - submission-scoped tutor message persistence and indexes
- `db/migration/V4__generation_attempts.sql` - generation attempt audit trail plus indexed feedback tags
- `db/migration/V5__backfill_generation_attempts.sql` - backfills audit rows for drafts that predate generation attempts

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
  "difficulty": "Medium",
  "targetConcepts": ["monotonic stack", "edge cases"],
  "constraintsNotes": "Avoid graph traversal. Include a boundary-heavy hidden test.",
  "interviewStyle": "Edge-case heavy"
}
```

Returns a validated `PublicProblemDraft`.

Current behavior:

- Creates a database-backed draft, not a public problem.
- Validates the generated draft schema before persistence.
- Validates both private Python and Java reference solutions through `SubmissionService`.
- Uses deterministic templates by default.
- Can use the OpenAI Responses API when `HACKERPRANK_GENERATOR_PROVIDER=openai` and `OPENAI_API_KEY` are configured.
- Can use the Anthropic Messages API when `HACKERPRANK_GENERATOR_PROVIDER=anthropic` and `ANTHROPIC_API_KEY` are configured.
- Attempts one provider-specific repair when a schema-valid OpenAI or Anthropic draft fails validation, then falls back to deterministic templates if repair also fails.
- Falls back to deterministic templates if the selected model provider is disabled, missing an API key, generation fails, or the returned draft cannot be repaired.
- Preserves requested target concepts, constraints/notes, and interview style in generation metadata parameters JSON.
- Stores provider, model id, prompt version, prompt text, parameters JSON, intended technique, validation status, validation errors, and validation summary.
- Stores a separate generation attempt row for each draft so feedback and outcomes survive publish, discard, and regeneration.
- Public draft responses include `id`, `topic`, `difficulty`, `validationStatus`, `createdAt`, `generationMetadata`, `quality`, `generationAttempt`, and `problem`.
- Public draft responses do not expose hidden test cases, prompt text, reference solutions, raw validation errors, or raw parameter JSON.

`GET /api/problems/drafts/{id}`

Returns a public draft preview if the draft still exists.

`POST /api/problems/drafts/{id}/feedback`

Saves quality feedback tags and notes for the active draft generation attempt.

`POST /api/problems/drafts/{id}/regenerate`

Creates a replacement draft from the prior draft's topic, difficulty, concepts, interview style, and feedback. The previous draft is discarded and its generation attempt is marked `REGENERATED`.

`POST /api/problems/drafts/{id}/publish`

Marks the draft problem as `PUBLISHED` and removes only the draft metadata.

`DELETE /api/problems/drafts/{id}`

Discards a draft.

`POST /api/problems/generate`

Request body:

```json
{
  "topic": "arrays",
  "difficulty": "Easy",
  "targetConcepts": ["two pointers"],
  "constraintsNotes": "Prefer exact-input/output interview format.",
  "interviewStyle": "Classic"
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

When OpenAI generation is enabled, the backend requests structured JSON from the Responses API. When Anthropic generation is enabled, the backend asks Claude through the Messages API for JSON matching the same schema. Both providers map into `GeneratedProblemSpec`, validate Python and Java reference solutions, attempt one repair after validation failure, and fall back to deterministic generation instead of surfacing a 500 to the user.

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

`POST /api/submissions/{id}/hint`

Returns a deterministic tutor nudge for a persisted submission. Visible failed tests may include visible expected vs actual output. Hidden-only failures stay generic and do not reveal hidden expected output, hidden actual output, or hidden test names.

`GET /api/submissions/{id}/tutor/messages`

Returns persisted tutor chat messages for the submission, ordered oldest to newest.

`POST /api/submissions/{id}/tutor/messages`

Request body:

```json
{
  "message": "Can you explain the visible mismatch?"
}
```

Stores the user message, creates an assistant reply, persists it, and returns the current submission-scoped tutor conversation. AI-backed paths receive only the same safe context used for tutor hints plus recent tutor messages.

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

When using `.env`, load it from the backend terminal before starting Spring Boot:

```sh
set -a
source ../.env
set +a
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

- 42 tests passed

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

Deterministic generator first, hosted model providers second:

- Gives us a stable endpoint and validation flow.
- Lets us test problem creation without spending tokens or debugging LLM variability.
- Provides a safe fallback when OpenAI or Anthropic is not configured or generation fails validation/parsing.

PostgreSQL + Flyway + JDBC persistence:

- PostgreSQL matches the likely production database direction better than an embedded-only store.
- Flyway gives explicit, reviewable schema history.
- Spring JDBC keeps query shape transparent while the user learns Spring persistence and while performance matters are still easy to reason about.
- Read projections are preferred for list endpoints so large text/code/test-output fields stay off hot paths.

## Known Limitations

- AI-backed generation is backend-only and opt-in.
- OpenAI and Anthropic prompt/eval quality still needs ongoing tuning.
- Tutor chat is submission-scoped, but explicit hint-level controls and solution unlocks are not implemented yet.
- No user accounts or sessions.
- Submission history is not user-scoped yet.
- No worker queue.
- Output matching only trims trailing whitespace.
- Docker sandbox is local-dev grade, not production hardened.
- No rate limiting or abuse controls.

## Recommended Next Milestones

Use `docs/PRODUCT_PLAN.md` as the canonical roadmap. As of this handoff, the next recommended implementation branch is `codex/generation-variant-controls`.

1. Add generation variant controls and deeper prompt behavior.
2. Add company/interview-style presets and variant regeneration.
3. Add user accounts and user-scoped submission history.
4. Move execution to a worker queue.

## OpenAI Tutor Notes

The OpenAI tutor is opt-in through `HACKERPRANK_TUTOR_PROVIDER=openai` and `OPENAI_API_KEY`. It uses the Responses API with a strict JSON schema and falls back to deterministic hints if the model call fails, returns unusable data, or is not configured.

The safe tutor context can include:

- public problem title, statement, formats, constraints, tags, and examples
- submission language, status, code, compile output, and pass counts
- visible failing test names, expected output, actual output, stderr, timeout flag, and exit code
- recent tutor chat messages for the same submission
- hidden test total, failed count, and timeout count

The safe tutor context must not include:

- hidden test names
- hidden inputs
- hidden expected outputs
- hidden actual outputs
- hidden stderr
- reference solutions

## Anthropic Provider Notes

Anthropic/Claude is opt-in and uses the direct Messages API. The app currently uses `claude-sonnet-4-6` as the default model for generation and tutoring, with all endpoint, model, version, timeout, and output-token settings overrideable by environment variable.

Generation setup:

```sh
export HACKERPRANK_GENERATOR_PROVIDER=anthropic
export ANTHROPIC_API_KEY=...
```

Tutor setup:

```sh
export HACKERPRANK_TUTOR_PROVIDER=anthropic
export ANTHROPIC_API_KEY=...
```

Important properties:

- `HACKERPRANK_ANTHROPIC_MODEL`
- `HACKERPRANK_ANTHROPIC_MESSAGES_URL`
- `HACKERPRANK_ANTHROPIC_VERSION`
- `HACKERPRANK_ANTHROPIC_MAX_OUTPUT_TOKENS`
- `HACKERPRANK_TUTOR_ANTHROPIC_MODEL`
- `HACKERPRANK_TUTOR_ANTHROPIC_MESSAGES_URL`
- `HACKERPRANK_TUTOR_ANTHROPIC_VERSION`
- `HACKERPRANK_TUTOR_ANTHROPIC_MAX_OUTPUT_TOKENS`

Provider behavior mirrors the OpenAI path:

- Problem drafts are validated before persistence.
- Validation failures get one repair attempt.
- Missing API keys, transport failures, parsing failures, validation failures, or repair failures fall back to deterministic generation or deterministic tutoring.
- Tutor hints and chat use the same safe context boundary and must never send hidden test details or reference solutions.

## OpenAI Generator Notes

The OpenAI generator produces structured data, not free-form markdown. It uses the Responses API with a strict JSON schema and stores the full prompt text and request parameters privately in generation metadata.

Generated draft shape:

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

The backend validates generated drafts before publishing:

- Schema validation
- Reference solution passes all generated tests
- Hidden tests include edge cases
- Starter code compiles/runs enough to fail predictably
- Problem statement does not leak hidden test answers
- No web-derived or copied problem wording

## Starter Code Harness Contract

Generated starter code should scaffold the boring I/O and leave the candidate focused on the intended algorithm.

What changed:

- OpenAI and Anthropic problem-generation prompts now require starter code to include complete stdin parsing and output wiring for the problem input format.
- Generated starter code must call a named TODO helper/function where the candidate implements the core algorithm.
- Generator prompts explicitly forbid leaving `main` as only `Scanner` setup plus generic TODO comments.
- `GeneratedProblemValidator` enforces the helper-call starter contract after generation so prompt drift triggers repair/fallback instead of accepting an unscaffolded draft.
- The validator also requires recognizable stdin reads before the helper call, preventing fake scaffolds such as `print(solve([]))`, and accepts idiomatic Java helpers with access modifiers like `private static`.
- Deterministic local fallback problems now follow the same pattern: `main` parses input, calls a helper, and prints the helper result.
- `V7__starter_code_harness_backfill.sql` updates existing persisted seeded/fallback starter-code rows so local databases created before this change get the same helper-call scaffolds.

Verification:

- `mvn -Dtest=GeneratedProblemFixtureValidationTests,OpenAiProblemGeneratorTests,AnthropicProblemGeneratorTests test`
- `mvn test`

## Java Editor Intelligence

The Java editor now has a backend-backed Eclipse JDT LS completion bridge for real language-server suggestions.

What changed:

- Spring exposes `GET /api/editor/java-lsp/status` and `POST /api/editor/java-lsp/completion`.
- Spring also exposes `POST /api/editor/java-lsp/hover` and `POST /api/editor/java-lsp/signature-help` so Monaco can show JDT-backed hover docs and method signatures.
- Java LSP v2 adds `POST /api/editor/java-lsp/diagnostics`. The backend listens for JDT LS `textDocument/publishDiagnostics`, caches diagnostics by document URI, and returns Monaco-friendly ranges after a short settle window.
- The backend starts `jdtls` over stdio, creates a scratch Java 21 Maven project under `.hackerprank-jdtls/`, syncs the Monaco buffer into `Main.java`, and forwards completion requests to JDT LS.
- Monaco merges JDT LS completion items with the local HackerPrank interview snippets, cleans noisy JDT labels for display, applies JDT diagnostics as editor markers, and falls back to local hovers/snippets when JDT LS is not installed.

Local setup:

```sh
brew install jdtls
```

If `jdtls` is not on `PATH`, set:

```sh
HACKERPRANK_JAVA_LSP_COMMAND=/path/to/jdtls
```

Notes:

- JDT LS requires Java 21, which matches the project runtime.
- `.hackerprank-jdtls/` is ignored because it stores generated workspace/project state.
- JDT LS can send client requests while HackerPrank is waiting for initialize/completion responses. Keep lifecycle, completion, and protocol write locks separate so the reader thread can answer those requests instead of deadlocking the language server.
- `workspace/configuration` responses must mirror the number of requested `params.items`, using `null` placeholders for settings HackerPrank does not provide.
- The Spring service owns the JDT LS child process and must destroy it during bean shutdown to avoid orphan Java processes and stale workspace locks during local/container restarts.
- If JDT LS startup fails after the process has been spawned, the service must close stdio, complete pending requests, and destroy the child before allowing the next retry. This prevents duplicate language servers from fighting over the same `.hackerprank-jdtls/data` workspace.
- Completion responses now preserve JDT LS `additionalTextEdits` so Monaco can apply import edits along with selected Java symbols.
- The frontend treats temporary JDT LS failures as a short retry backoff instead of permanently disabling language-server requests for the rest of the React component lifetime.
- LSP response mapping lives in `JavaLspProtocolMapper` so protocol edge cases can be unit tested without spawning JDT LS. Keep completion mapping, hover markup, signature help, and `workspace/configuration` shape changes covered there.
- JDT LS startup timeout is configurable through `HACKERPRANK_JAVA_LSP_STARTUP_TIMEOUT_MS`; production defaults to 20 seconds, while tests can use a short timeout to verify cleanup paths.
- JDT LS diagnostics settle time is configurable through `HACKERPRANK_JAVA_LSP_DIAGNOSTICS_SETTLE_MS`; production defaults to 1200ms so publishDiagnostics has a chance to arrive after didOpen/didChange, especially on the first cold JDT startup.
- LSP document versions are tracked separately from JSON-RPC request IDs. Every `didOpen` and `didChange`, including diagnostics-only syncs, must send a monotonically increasing text document version so JDT LS does not publish stale diagnostics for the wrong buffer.
- Diagnostics notifications are accepted only when their `params.version` is current for the synced document URI. This prevents an older asynchronous JDT LS publish from repopulating Monaco markers after the user has already edited or fixed the code.
- This is still a REST bridge, not a full Monaco language-client/WebSocket bridge. Future editor work can add code actions, organize imports, rename, and richer diagnostics streaming.

## Backend Optimization Notes

The backend is still intentionally simple Spring Boot plus JdbcTemplate, but some repository paths have been tightened so persistence does not become the first bottleneck as generated problems, submissions, and tutor sessions grow.

What changed:

- `ProblemRepository.findAll()` now hydrates full problems in batches instead of calling `findById()` for each problem. This avoids a classic N+1 pattern across tags, constraints, examples, test cases, and starter code.
- `SubmissionRepository.findById()` now loads the submission and ordered test-case results with one left-joined query instead of a parent query plus a second child query.
- Draft generation feedback tags are written with `batchUpdate`, and draft delete/regenerate paths avoid duplicate draft lookups.
- `V6__generation_attempt_lookup_indexes.sql` adds a composite index for the “latest generation attempt by draft” query: `(draft_id, created_at DESC, id)`.
- JDBC timestamp handling now lives in `JdbcInstant` so repository mappers do not duplicate driver-specific `OffsetDateTime` / `Timestamp` handling.

Verification:

- `mvn -Dtest=ProblemControllerTests,SubmissionControllerTests,SubmissionServiceTests test`
- `mvn clean test`

## Generation Usage Persistence

Generation attempts now record lightweight usage telemetry so the product can optimize prompt cost without persisting raw provider prompts or generated payloads in the query path.

What changed:

- `V8__generation_attempt_usage_metrics.sql` adds prompt/response hashes, character counts, and estimated token counts to `generation_attempts`.
- `GenerationUsageEstimator` approximates tokens as `ceil(characters / 4)` and stores SHA-256 hashes for prompt/response dedupe analysis.
- `ProblemGeneratorService` computes metrics from `GenerationMetadata.promptText()` and a serialized generated response payload when drafts are created or backfilled into attempts.
- `PublicGenerationAttempt` exposes only counts/hashes, not raw prompt text, and Draft QA shows compact estimated-token/prompt-character metrics.

Why:

- This supports the user's goal of reducing token usage while keeping persistence strong and queryable.
- Keeping raw prompt text out of `generation_attempts` avoids turning the attempt ledger into a sensitive blob table; richer private generation artifacts remain separate.

Verification:

- `mvn -Dtest=GenerationUsageEstimatorTests,ProblemControllerTests test`

## Problem Quality Contract

Generated problems now have an explicit LeetCode-style solution contract instead of relying on one generic description field.

What changed:

- `V9__problem_solution_contract.sql` adds `scenario`, `task`, `java_signature`, and `python_signature` columns to `problems`, backfilled from existing descriptions where needed.
- `Problem` now exposes separate scenario/task/signature fields, and `ProblemRepository` persists and hydrates them.
- OpenAI and Anthropic generation schemas require those fields, and prompts now demand a concrete real-world scenario, precise task paragraph, candidate method/function signatures, and examples that explain behavior.
- `GeneratedProblemValidator` requires sufficiently descriptive scenario/task fields and checks starter code calls the declared Java/Python helper signature.
- Deterministic fallback and seeded problems now include first-class method signatures, so local/fallback mode does not look like a thin toy problem.
- Deterministic fallback routing now considers topic, target concepts, notes, and interview style. This fixes the confusing behavior where broad prompts with `sliding window, hash map` concepts still fell through to the default `Signal Peaks` template because only the topic text was inspected.
- Starter-code validation now checks that the declared helper/function is both defined and called, without requiring the call to be directly nested inside `print(...)` or `System.out.println(...)`. This accepts more natural AI-generated harnesses where the helper result is assigned to a variable before printing.
- Generated problem construction now preserves blank `scenario` and `task` values so the validator can reject missing statement sections instead of silently backfilling them from `description`.
- Starter-code validation now checks the advertised helper signature more deeply: Java starter code must define the same helper name, return type, and parameter type list, Python starter code must define the same function and parameter list, and both starters must call the helper outside the definition. This prevents drafts where the UI advertises one method signature but the stdin harness calls another.
- Malformed but non-empty helper signatures now fail validation instead of silently skipping starter-code contract checks. This keeps generated drafts from advertising unusable signatures like `edgeSum(values)`.
- Python starter-code validation now accepts normal return type annotations in helper definitions, such as `def solve(items: list[int]) -> int:`. This fixed a real Anthropic fallback case where Claude generated a good typed helper but validation rejected it and fell back to the deterministic template.
- Deterministic fallback routing now prioritizes sliding-window/window-scanning language before generic hash-map/frequency language, so prompts like `sliding window, hash maps` no longer route to `First Solo Word` if AI generation is unavailable.
- The frontend Problem tab displays Scenario, Task, and Java/Python signatures before input/output format.
- The Studio topbar now distinguishes active drafts from saved problems. A saved/published problem says `Viewing saved problem` instead of `Local Mode`, and the last generated draft title/provider remains visible after generation so provider/fallback state is less ambiguous.

Why:

- This directly addresses the thin generated statement problem: a candidate should see the story, exact task, and method contract before writing code.
- The schema and validator enforce the shape instead of hoping prompt wording alone does the job.

Verification:

- `mvn -Dtest=GeneratedProblemFixtureValidationTests,OpenAiProblemGeneratorTests,AnthropicProblemGeneratorTests,ProblemControllerTests test`
- `mvn -Dtest=GeneratedProblemFixtureValidationTests,ProblemControllerTests,AnthropicProblemGeneratorValidationFallbackTests,OpenAiProblemGeneratorValidationFallbackTests test`
- `mvn -Dtest=GeneratedProblemFixtureValidationTests,ProblemControllerTests test`
- Live Anthropic smoke via `POST /api/problems/drafts` with a Meta/sliding-window/hash-map prompt returned provider `anthropic`, title `Unique Reaction Window`, and no repair.
- `npm run build` from `frontend`

## Studio Concept Polish

The frontend is being tightened toward `concept.png`: a quieter interview-workbench shell with a left problem rail, compact generation command bar, split problem/editor workspace, restrained typography, and a darker editor surface.

What changed:

- The Studio shell now uses lighter borders, softer type weights, a wider rail, and denser spacing so it feels closer to a production coding platform than a prototype.
- The generation composer remains first-class because live custom problem generation is the core job-to-be-done, but it has been reduced into a compact command bar instead of a large form block.
- Problem statement, examples, editor toolbar, and results areas were rebalanced to better match the concept proportions while keeping existing functionality intact.
- The desktop and mobile layouts were visually checked with Playwright captures after the CSS pass.
- Problem statement section tabs are now honest anchors: `Draft QA` only appears when a generated draft has QA metadata, empty sections are omitted, and the selected tab follows the clicked section instead of pretending the first tab is always active.
- The single-column workspace breakpoint is set above the combined rail and two-column workspace minimums so small laptop/tablet-landscape widths do not clip the editor.

Verification:

- `npm run build` from `frontend`
- Desktop capture at `1536x960`
- Mobile capture at `390x844`
- Playwright DOM check that published problems show only real `Problem`, `Examples`, and `Constraints` links and that clicked section state updates.

## Real Problem Tabs

Problem statement tabs are now stateful panels instead of anchor links over one long document.

What changed:

- `Problem` shows only the title, difficulty, tags, description, and input/output format.
- `Examples` shows only visible examples and their explanations.
- `Constraints` shows formal constraints plus a lightweight validation-focus note to clarify how hidden tests should relate to the stated limits.
- `Draft QA` remains conditional and only appears when a draft has generation metadata and quality checks.
- Selecting a new problem resets the statement area back to the `Problem` tab.

Verification:

- `npm run build` from `frontend`
- `git diff --check`
- Playwright DOM check that each tab mounts a distinct panel and updates `aria-selected`.

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
