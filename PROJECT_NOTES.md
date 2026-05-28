# HackerPrank Project Notes

Last updated: 2026-05-27

This is the living handoff file for HackerPrank. Future chats should read this first, then run `git status --short --branch`, then check `README.md` for setup commands.

Agentic development docs:

- `AGENTS.md` - operating instructions for coding agents.
- `SKILLS.md` - repeatable project workflows for agents.

Agent memory rule:

- Agents must update this file after meaningful implementation or workflow changes so a future chat can reconstruct where the project left off from the repo alone.
- Agents should start new sessions by reading `PROJECT_NOTES.md`, `AGENTS.md`, `SKILLS.md`, and `README.md`, then checking `git status --short --branch` and `git log -5 --oneline`.

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
- `0e44f06` - Add project handoff notes
- `956d2c6` - Add agent workflow docs
- `c5dc1bb` - Strengthen agent handoff rules
- Current session - Add generated problem draft flow

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

Important files:

- `frontend/src/App.tsx` - main UI and state orchestration
- `frontend/src/api.ts` - API client functions
- `frontend/src/types.ts` - shared TypeScript API shapes
- `frontend/src/styles.css` - current app styling
- `frontend/vite.config.ts` - Vite dev server and `/api` proxy to backend

Current frontend limitation:

- The generator UI supports topic and difficulty, but not richer constraints such as target concepts, company style, time limits, or prompt notes.
- Draft preview is in-memory only because backend drafts are in-memory only.

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
- `ProblemDraft.java` - private generated draft model with reference solution
- `ProblemDraftRepository.java` - in-memory generated draft store
- `PublicProblemDraft.java` - public draft response that hides reference solutions and hidden tests
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

- Creates an in-memory draft, not a public problem.
- Validates the generated draft by running its private Python reference solution through `SubmissionService`.
- Public draft responses include `id`, `topic`, `difficulty`, `validationStatus`, `createdAt`, and `problem`.
- Public draft responses do not expose hidden test cases or reference solutions.

`GET /api/problems/drafts/{id}`

Returns a public draft preview if the draft still exists.

`POST /api/problems/drafts/{id}/publish`

Moves a draft into the public in-memory problem repository and deletes the draft.

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
- Internally creates a validated draft, publishes it immediately, then deletes the draft.
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

Generated drafts and generated problems are currently stored in memory only. They disappear when the backend restarts.

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
- Generated drafts and generated problems disappear on backend restart.
- Generated-problem templates are deterministic and limited.
- Generator controls only cover topic and difficulty.
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

1. Add persistence for problems, drafts, reference solutions, and submissions.
2. Introduce an OpenAI-backed generator behind `ProblemGeneratorService`.
3. Define a strict JSON schema for generated problem drafts.
4. Store reference solutions separately from public problem data in persistent storage.
5. Add Java reference-solution validation in addition to Python.
6. Add richer generator controls for concepts, constraints, and interview style.
7. Add submission history and result detail pages.
8. Move execution to a worker queue.
9. Harden sandboxing before any remote or multi-user deployment.

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
