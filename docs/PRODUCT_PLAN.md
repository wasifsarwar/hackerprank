# HackerPrank Product Plan

Last updated: 2026-05-28

This document is the product north star for HackerPrank. Use it to choose the next iteration, shape PR scope, and keep the work pointed at the final experience: an agentic interview-practice platform that generates original problems, validates them, watches how you solve, and coaches you without giving the game away too early.

## Product North Star

HackerPrank should feel like a private interview gym with an agentic tutor beside you.

The user chooses a topic, difficulty, interview style, target concepts, and constraints. The platform generates a new, original coding problem that has not simply been scraped from LeetCode, validates the examples and hidden tests, gives Python and Java starter code, runs submissions in a sandbox, tracks attempts, and teaches from failures.

The end-state should be polished enough that practicing feels motivating, not like filling out a form around an API call.

## End-State Experience

1. Pick a practice goal: topic, difficulty, company style, target concepts, time limit, and weak spots.
2. Generate a fresh interview-style problem with examples, constraints, starter code, and hidden tests.
3. Review a transparent quality panel that proves the problem was validated before you touch it.
4. Solve in Monaco with Java or Python, run samples, submit hidden tests, and inspect results.
5. Ask for progressive hints that nudge rather than reveal the full solution.
6. Get post-attempt feedback on correctness, complexity, edge cases, code quality, and interview explanation.
7. Save progress across sessions, compare attempts, and see skill trends over time.
8. Let the tutor build an adaptive practice path from your history.

## Product Pillars

### 1. Original Problem Generation

The generator should create interview-grade problems on demand rather than only rephrasing known public problems.

Desired features:

- Topic, difficulty, target concept, and interview-style controls.
- Company-style presets such as "startup practical", "big tech algorithms", and "edge-case heavy".
- Constraint knobs for input size, time pressure, data structures, and forbidden approaches.
- Generated examples, visible tests, hidden tests, starter code, reference solutions, hints, and editorial metadata.
- Draft preview before publishing into the local problem bank.
- Regenerate variants while preserving the same learning objective.
- Problem families: arrays, strings, hash maps, stacks, queues, heaps, trees, graphs, dynamic programming, intervals, binary search, recursion, simulation, parsing, and systems-flavored practical coding.

Quality requirements:

- Validate examples as runnable cases.
- Validate visible and hidden tests against Python and Java reference solutions.
- Require both Python and Java starter code and reference solutions.
- Reject drafts that leak hidden-test data into public responses.
- Keep prompt/version metadata server-side for audit and tuning.
- Maintain an eval fixture corpus for generator regressions.

### 2. Safe Code Execution

The execution layer should be boring in the best way: predictable, isolated, and observable.

Desired features:

- Python and Java support as first-class languages.
- Docker or worker-backed sandbox isolation with no network, resource limits, and timeout enforcement.
- Separate sample runs from submitted hidden-test runs.
- Per-test result detail for visible tests.
- Hidden-test result summaries without leaking hidden inputs or expected outputs.
- Compile errors, runtime errors, timeouts, and wrong-answer feedback as distinct states.
- Async execution queue for long-running or multi-user scenarios.
- Run cancellation and retry.
- Execution telemetry for runtime, memory approximation, failure mode, and queue wait.

Production direction:

- Move from synchronous in-process execution to a dedicated worker service.
- Add a jobs table or queue.
- Store execution artifacts with bounded retention.
- Keep sandbox implementation replaceable.

### 3. Agentic Tutor

The tutor should behave like a good interviewer: precise, patient, and careful not to spoil the learning.

Desired features:

- Deterministic fallback hints for reliability.
- OpenAI-backed hints using only public problem data, user code, compile output, visible failures, and hidden failure counts.
- Progressive hint levels:
  - Nudge: ask where to look.
  - Diagnostic: name the likely class of bug.
  - Strategy: suggest an approach.
  - Pseudocode: outline steps without full code.
  - Solution unlock: only after explicit user request or repeated attempts.
- Follow-up chat attached to a failed submission.
- "Explain my bug" mode that traces the mismatch on a visible case.
- "Interview me" mode that asks questions instead of directly instructing.
- Complexity coaching: Big-O target, current likely complexity, and where the bottleneck lives.
- Code-review mode: readability, edge-case coverage, input parsing, naming, and Java/Python idioms.
- Post-solve editorial: approach, proof sketch, edge cases, alternative solutions, and common traps.

Safety and learning constraints:

- Do not send hidden inputs, hidden expected outputs, hidden actual outputs, or hidden test names to the tutor model.
- Do not expose full reference solutions by default.
- Keep hints short and actionable.
- Make the user do the thinking unless they explicitly unlock the answer.

### 4. Learning Memory And Progress

The platform should remember enough to make practice compound.

Desired features:

- User accounts or local profiles.
- Attempt history by problem, topic, difficulty, language, and outcome.
- Skill map showing strengths and weak spots.
- Spaced repetition for missed concepts.
- Adaptive problem recommendations.
- Streaks and session summaries.
- "Why I got this wrong" tagging: parsing, off-by-one, complexity, data structure choice, edge case, syntax, runtime error.
- Personal problem bank: generated, published, attempted, mastered, archived.

Data model direction:

- Add user/profile table before global history becomes too limiting.
- Scope submissions, drafts, and progress to users.
- Keep summary queries cheap; hydrate details only on demand.
- Index for recent attempts, topic filtering, status filtering, and learning dashboards.

### 5. Polished Practice Workspace

The frontend should feel like a serious tool, not a demo wrapper.

Desired features:

- Clean split between problem, editor, results, tutor, and history.
- Keyboard-friendly workflows.
- Resizable panes.
- Better loading states for generation and submissions.
- Monaco quality-of-life settings for Java/Python.
- Submission history with filters and comparison.
- Result details with visible failure diffs.
- Tutor panel that can live beside the editor.
- Draft QA panel for generated problems.
- Problem list filters by topic, difficulty, status, and source.
- Session mode with timer and interview-style constraints.
- Dark/light theme later if it earns its keep.

Design direction:

- Dense, calm, work-focused UI.
- Keep cards for actual repeated items and panels; avoid decorative clutter.
- Make the first screen the usable workspace.
- Show confidence, quality, and state clearly without over-explaining the app inside the app.

### 6. Generator Quality Lab

We need a small internal workbench for improving generation quality over time.

Desired features:

- Fixture corpus for valid and invalid generated problems.
- Prompt version tracking.
- Draft validation summaries.
- Repair attempt tracking.
- Side-by-side comparison of generated draft vs repaired draft.
- Red-team checks for hidden-test leakage, invalid examples, missing language support, and bad constraints.
- Topic coverage dashboard.
- Manual approval flow for generated problems before they become part of the problem bank.

Useful eval categories:

- Wrong reference solution.
- Bad example output.
- Missing Java or Python reference solution.
- Missing hidden tests.
- Missing visible tests.
- Missing problem section.
- Inconsistent constraints.
- Starter code that solves the problem accidentally.
- Hidden-test leakage in public fields.

### 7. Deployment And Operations

The app should remain easy to run locally while moving toward deployable architecture.

Desired features:

- Local Compose stack remains one-command friendly.
- GitHub Actions continue to run backend tests, frontend build, and Compose smoke tests.
- Published images for frontend and backend.
- Environment-variable-driven OpenAI configuration.
- Database migrations for every persistence change.
- Netlify deployment path for frontend if useful.
- Backend hosting strategy documented separately when the sandbox worker is split out.

Operational direction:

- Keep production-grade execution separate from the web API.
- Do not mount a host Docker socket into production web containers.
- Treat generated problem prompts, hidden tests, and reference solutions as private server-side artifacts.

## Iteration Roadmap

### Iteration 1: OpenAI Tutor Hints

Goal: make the tutor feel intelligent while keeping deterministic fallback.

Scope:

- Add OpenAI-backed tutor service.
- Reuse existing OpenAI transport/config where practical.
- Send only public problem data, user code, compile output, visible failures, and hidden failure counts.
- Return structured hint JSON.
- Fall back to deterministic hints on failure.
- Test that hidden details are never sent.
- Update frontend hint card with provider and maybe "another hint".

Why next:

The current deterministic hint API is the perfect place to add an AI tutor. This is the first feature that makes HackerPrank feel meaningfully agentic.

### Iteration 2: Tutor Follow-Up Chat

Goal: let the user ask for clarification without leaving the failed submission context.

Scope:

- Add a submission-scoped tutor message endpoint.
- Store short tutor sessions.
- Support hint-level requests like "smaller nudge", "explain the visible failure", or "ask me a question".
- Keep no-solution/no-hidden-test rules enforced.

### Iteration 3: Draft Quality Panel

Goal: make generated-problem quality visible and trustworthy.

Scope:

- Show validation summary, provider, model, prompt version, repair used, visible/hidden test counts, language validation status, and example validation status.
- Keep private prompt text, hidden tests, raw validation errors, and reference solutions hidden.
- Add backend DTO fields if needed.

### Iteration 4: Generation Variant Controls

Goal: make generation controls affect the actual generated problem more deeply.

Scope:

- Expand prompt controls.
- Add company/interview-style presets.
- Add constraints/forbidden-approach controls.
- Add regenerate variant flow.
- Add eval fixtures for every new generation behavior.

### Iteration 5: User Profiles And Scoped History

Goal: stop treating all submissions as global.

Scope:

- Add local user/profile model.
- Scope submissions and drafts to user/profile.
- Add indexes for user history queries.
- Update frontend to pick or create a local profile.

### Iteration 6: Adaptive Practice Dashboard

Goal: turn attempt history into learning direction.

Scope:

- Topic and difficulty performance summaries.
- Common failure tags.
- Recommended next practice set.
- Recent improvement trend.
- Session recap.

### Iteration 7: Async Runner Worker

Goal: decouple execution from the API request path.

Scope:

- Add submission jobs.
- Introduce queued/running/completed states.
- Poll results from frontend.
- Move runner execution behind a worker abstraction.
- Keep local mode simple for development.

### Iteration 8: Problem Editorials

Goal: make solved problems teach deeply.

Scope:

- Generate or store editorials.
- Show approach, proof sketch, complexity, edge cases, and alternative implementations.
- Gate editorials until accepted or user unlocks.
- Add editorial quality checks.

## Near-Term PR Backlog

Use this list when choosing the next branch.

- `codex/openai-tutor-hints`
- `codex/tutor-follow-up-chat`
- `codex/draft-quality-panel`
- `codex/generation-variant-controls`
- `codex/user-profiles`
- `codex/adaptive-practice-dashboard`
- `codex/async-submission-worker`
- `codex/problem-editorials`

## Decision Rules

When choosing what to build next:

- Prefer features that make the core loop better: generate, solve, fail, learn, retry.
- Keep hidden tests and reference solutions private.
- Add tests for privacy boundaries, validation rules, and stale-response risks.
- Keep PRs focused enough for Codex/GitHub review to be useful.
- Update `PROJECT_NOTES.md` whenever the plan or implementation changes.
- Ship vertical slices over abstract infrastructure unless the infrastructure unlocks the next visible capability.

## Current Best Next Step

Build `codex/openai-tutor-hints`.

This turns the current deterministic hint panel into the first truly agentic learning feature while preserving safety, fallback behavior, and the existing UI contract.
