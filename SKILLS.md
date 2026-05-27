# HackerPrank Project Skills

This file describes repeatable workflows for agents working on HackerPrank. Treat each section as a project-specific skill.

## Skill: Project Handoff

Use when starting a new chat or resuming unknown work.

Steps:

1. Read `PROJECT_NOTES.md`.
2. Read `README.md`.
3. Read `AGENTS.md`.
4. Read the relevant workflow in this file.
5. Run `git status --short --branch`.
6. Run `git log -5 --oneline`.
7. Identify the current milestone, latest commit, and any dirty worktree changes.
8. Summarize only the context needed for the next action.

Done when:

- The agent knows the current architecture, branch state, trusted checks, known limitations, and next intended task.

## Skill: Backend API Change

Use when adding or changing Spring Boot endpoints, DTOs, services, or backend behavior.

Steps:

1. Inspect the relevant controller, service, model, and tests.
2. Keep request/response DTOs explicit and typed.
3. Keep controller logic thin.
4. Put behavior in a service when it has business rules, validation, or orchestration.
5. Add focused tests with `@SpringBootTest`, `MockMvc`, or service-level tests as appropriate.
6. Run `cd backend && mvn test`.
7. Update `PROJECT_NOTES.md` if the API shape or decision changes.

Done when:

- Tests pass, the endpoint behavior is documented if meaningful, and public responses do not leak hidden test cases or reference solutions.

## Skill: Frontend Feature Change

Use when changing React, TypeScript, Monaco, API calls, or app styling.

Steps:

1. Inspect `frontend/src/App.tsx`, `api.ts`, `types.ts`, and `styles.css`.
2. Keep shared API shapes in `types.ts`.
3. Keep fetch wrappers in `api.ts`.
4. Match the existing dense tool-style UI.
5. Avoid landing-page patterns and decorative-only UI.
6. Run `cd frontend && npm run build`.
7. Verify through the browser connector when available, or through API/build smoke tests when it is not.

Done when:

- TypeScript and Vite build pass, and the changed UI has a clear state path for loading, success, and error.

## Skill: Generated Problem Work

Use when changing problem generation, templates, validation, or the future OpenAI-backed tutor flow.

Steps:

1. Inspect `ProblemGeneratorService.java`, `ProblemRepository.java`, `ProblemController.java`, and `SubmissionService.java`.
2. Keep generation output structured.
3. Validate generated problems before publishing them.
4. Keep reference solutions out of public API responses.
5. Include visible examples and hidden edge cases.
6. Prefer deterministic tests around generator behavior.
7. Update `PROJECT_NOTES.md` when the generation contract changes.

Done when:

- A generated problem can be fetched by id and its reference solution passes all visible and hidden tests.

## Skill: Submission Runner Change

Use when changing code execution, timeouts, Docker isolation, process handling, or supported languages.

Steps:

1. Inspect `SubmissionService.java`, `SandboxRunner.java`, `DockerSandboxRunner.java`, `LocalSandboxRunner.java`, and runner tests.
2. Preserve the `SandboxRunner` abstraction.
3. Keep Docker network disabled.
4. Preserve memory, CPU, pid, capability, read-only root, and no-new-privileges controls unless the user explicitly chooses otherwise.
5. Verify compile errors, wrong answers, accepted submissions, and timeout behavior when the change touches execution semantics.
6. Check for leftover containers with:

```sh
docker ps -a --filter name=hackerprank --format '{{.Names}}'
```

Done when:

- `mvn test` passes, timeouts are handled, and no stale `hackerprank` containers remain after live runner tests.

## Skill: Persistence Planning

Use when introducing a database or persisting problems/submissions.

Steps:

1. Identify what must persist: problems, generated drafts, submissions, test results, reference solutions, and generation metadata.
2. Keep public problem data separate from private validation data.
3. Decide whether the next step needs JPA, Flyway/Liquibase, or a lighter file/database approach.
4. Add migrations and repository tests if using a relational database.
5. Update `README.md` with setup commands and `PROJECT_NOTES.md` with the storage decision.

Done when:

- Restarting the backend no longer loses the persisted data covered by the change.

## Skill: OpenAI Tutor Integration

Use when adding OpenAI-backed problem generation or agentic tutoring.

Steps:

1. Check official OpenAI docs before choosing models or API shapes.
2. Keep the existing `POST /api/problems/generate` contract stable unless there is a deliberate API migration.
3. Generate structured JSON, not free-form markdown.
4. Validate the JSON schema before converting it into `Problem`.
5. Run generated reference solutions through the sandbox before publishing.
6. Store prompts, model metadata, validation status, and draft state when persistence exists.
7. Add guardrails against copied web problems and hidden-answer leakage.

Done when:

- The model output is schema-valid, sandbox-validated, and reviewable before becoming a public problem.

## Skill: Documentation Update

Use after meaningful changes or decisions.

Steps:

1. Update `PROJECT_NOTES.md` for project memory.
2. Record what changed, why it changed, how it was verified, and what should happen next.
3. Update `README.md` for setup or usage changes.
4. Update `AGENTS.md` or `SKILLS.md` for workflow changes.
5. Run `git diff --check`.
6. Check `git status --short --branch`.

Done when:

- A future chat can pick up the project without needing unstated conversation context.

## Skill: End Of Session Handoff

Use before ending a substantial implementation session.

Steps:

1. Confirm the worktree state with `git status --short --branch`.
2. Confirm the latest commits with `git log -5 --oneline`.
3. Make sure `PROJECT_NOTES.md` reflects the latest implementation state.
4. Make sure the next recommended task is written down.
5. Commit final changes on `main` when appropriate for the project flow.
6. In the final response, mention the verification performed and the latest commit hash.

Done when:

- Another chat can continue without asking what changed or where to resume.
