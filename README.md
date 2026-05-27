# HackerPrank

A local proof of concept for a LeetCode/HackerRank-style coding practice platform.

## Shape of the App

- `frontend/`: React + TypeScript + Vite
- `backend/`: Spring Boot API that serves problems and runs submissions

The first version intentionally uses stdin/stdout problems. That keeps the runner simple while still teaching the important pieces: APIs, DTOs, process execution, timeouts, test results, and frontend state.

## Local Commands

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

If Maven is not installed globally, install Maven or use the project with an IDE that can import Maven projects.

## Current Limitations

- User code runs locally, not inside Docker yet.
- Output matching is exact after trimming trailing whitespace.
- Problems are seeded in memory.
- There is no database or auth yet.

## Next Milestones

1. Add Docker-based execution for safer isolation.
2. Add persistence for problems and submissions.
3. Add a generated-problem draft flow.
4. Add Monaco editor and richer result inspection.
