import type {
  GenerateProblemRequest,
  Problem,
  ProblemDraft,
  ProblemSummary,
  SubmissionRequest,
  SubmissionResult
} from "./types";

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

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export function fetchProblems(): Promise<ProblemSummary[]> {
  return request<ProblemSummary[]>("/api/problems");
}

export function fetchProblem(id: string): Promise<Problem> {
  return request<Problem>(`/api/problems/${id}`);
}

export function generateProblem(payload: GenerateProblemRequest): Promise<Problem> {
  return request<Problem>("/api/problems/generate", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function createProblemDraft(payload: GenerateProblemRequest): Promise<ProblemDraft> {
  return request<ProblemDraft>("/api/problems/drafts", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function publishProblemDraft(id: string): Promise<Problem> {
  return request<Problem>(`/api/problems/drafts/${id}/publish`, {
    method: "POST"
  });
}

export function deleteProblemDraft(id: string): Promise<void> {
  return request<void>(`/api/problems/drafts/${id}`, {
    method: "DELETE"
  });
}

export function runSubmission(payload: SubmissionRequest): Promise<SubmissionResult> {
  return request<SubmissionResult>("/api/submissions/run", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
