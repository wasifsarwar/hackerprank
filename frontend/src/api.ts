import type { Problem, ProblemSummary, SubmissionRequest, SubmissionResult } from "./types";

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function fetchProblems(): Promise<ProblemSummary[]> {
  return request<ProblemSummary[]>("/api/problems");
}

export function fetchProblem(id: string): Promise<Problem> {
  return request<Problem>(`/api/problems/${id}`);
}

export function runSubmission(payload: SubmissionRequest): Promise<SubmissionResult> {
  return request<SubmissionResult>("/api/submissions/run", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
