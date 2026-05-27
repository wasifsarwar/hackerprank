# HackerPrank Agent Guide

This file is for coding agents working on HackerPrank. Read it before making changes.

## Startup Checklist

1. Read `PROJECT_NOTES.md`.
2. Read `README.md`.
3. Read `SKILLS.md`.
4. Run `git status --short --branch`.
5. Confirm you are on `main` unless the user explicitly asks for another branch.
6. Run `git log -5 --oneline` to see the latest committed milestones.
7. Inspect the relevant code before proposing or editing.
8. Before changing code, state the likely continuation point in plain language.

## Continuation Protocol

Every new chat should be able to continue from the repository alone.

At the start of a session, reconstruct:

- What the app currently does.
- What changed most recently.
- What the user was trying to accomplish next.
- What checks are currently trusted.
- What risks or limitations are already known.

If the next step is unclear, recommend one concrete next action based on `PROJECT_NOTES.md`, `SKILLS.md`, and the current code.

## Project Intent

HackerPrank is a learning-focused coding practice platform. The user wants to build toward an agentic tutor that can generate original HackerRank-style problems, test cases, starter code, and feedback.

Keep the work educational. When useful, explain why a change is being made, name tradeoffs, and preserve a path for the user to learn Spring Boot, React, TypeScript, and sandboxed code execution.

## Current Stack

- Frontend: React, TypeScript, Vite, Monaco editor
- Backend: Spring Boot 3.4.6, Java 21
- Runner: Docker by default, with a local runner for tests/dev
- Local Docker runtime: Colima with the Docker CLI
- Supported submission languages: Python and Java

## Working Rules

- Keep changes on `main` unless the user says otherwise.
- Do not rewrite, reset, or discard user changes.
- Use `rg` or `rg --files` for searching.
- Use `apply_patch` for manual file edits.
- Keep docs current whenever behavior, architecture, setup, API shape, workflow, or project direction changes.
- Do not finish a meaningful implementation without updating `PROJECT_NOTES.md`.
- Prefer small, verifiable increments over broad rewrites.
- Follow existing Spring and React patterns before introducing new abstractions.
- Keep generated problems private-test-safe: public API responses must not expose hidden tests or reference solutions.

## End Of Change Checklist

Before giving the final response for any non-trivial change:

1. Update `PROJECT_NOTES.md` with what changed, why it changed, and what should happen next.
2. Update `README.md` if setup or usage changed.
3. Update `AGENTS.md` or `SKILLS.md` if the development workflow changed.
4. Run the relevant verification commands.
5. Run `git status --short --branch`.
6. Commit on `main` when the user has asked for final changes to land there or when prior project context requires committed checkpoints.

The final response should include:

- Files changed at a high level.
- Verification performed.
- Commit hash if committed.
- Any skipped checks or known follow-up.

## Verification Expectations

Run the smallest useful checks for the change:

- Backend change: `cd backend && mvn test`
- Frontend change: `cd frontend && npm run build`
- API change: add or update a focused backend test
- Runner/sandbox change: verify timeout and cleanup behavior when practical
- Docs-only change: `git diff --check`

If a check is skipped, say why.

## Local Services

Frontend:

```sh
cd frontend
npm run dev
```

Backend:

```sh
cd backend
mvn spring-boot:run
```

Useful smoke test for generated problems:

```sh
curl -sS -w '\nHTTP %{http_code}\n' \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{"topic":"arrays","difficulty":"Easy"}' \
  http://127.0.0.1:5173/api/problems/generate
```

Check for leaked sandbox containers:

```sh
docker ps -a --filter name=hackerprank --format '{{.Names}}'
```

## Documentation Contract

Use the docs this way:

- `README.md`: setup and ordinary usage.
- `PROJECT_NOTES.md`: project memory, decisions, milestones, handoff context.
- `AGENTS.md`: operating instructions for coding agents.
- `SKILLS.md`: repeatable workflows for common project tasks.

When adding a major feature, update `PROJECT_NOTES.md`. When changing agent workflow, update this file or `SKILLS.md`.
