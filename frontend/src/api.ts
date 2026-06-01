import type {
  DraftFeedbackRequest,
  GenerateProblemRequest,
  GenerationAttempt,
  JavaLspCompletionRequest,
  JavaLspCompletionResponse,
  JavaLspDiagnosticsResponse,
  JavaLspHoverResponse,
  JavaLspPositionRequest,
  JavaLspSignatureHelpResponse,
  Problem,
  ProblemDraft,
  ProblemSummary,
  RegenerateDraftRequest,
  SubmissionRequest,
  SubmissionDetail,
  SubmissionResult,
  SubmissionSummary,
  TutorChatResponse,
  TutorHint,
  TutorMessage
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

export function saveDraftFeedback(id: string, payload: DraftFeedbackRequest): Promise<GenerationAttempt> {
  return request<GenerationAttempt>(`/api/problems/drafts/${id}/feedback`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function regenerateProblemDraft(id: string, payload: RegenerateDraftRequest): Promise<ProblemDraft> {
  return request<ProblemDraft>(`/api/problems/drafts/${id}/regenerate`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function runSubmission(payload: SubmissionRequest): Promise<SubmissionResult> {
  return request<SubmissionResult>("/api/submissions/run", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchSubmissionHistory(problemId: string, limit = 20): Promise<SubmissionSummary[]> {
  const params = new URLSearchParams({
    problemId,
    limit: String(limit)
  });
  return request<SubmissionSummary[]>(`/api/submissions?${params.toString()}`);
}

export function fetchSubmissionDetail(id: string): Promise<SubmissionDetail> {
  return request<SubmissionDetail>(`/api/submissions/${id}`);
}

export function fetchSubmissionHint(id: string): Promise<TutorHint> {
  return request<TutorHint>(`/api/submissions/${id}/hint`, {
    method: "POST"
  });
}

export function fetchTutorMessages(id: string): Promise<TutorMessage[]> {
  return request<TutorMessage[]>(`/api/submissions/${id}/tutor/messages`);
}

export function sendTutorMessage(id: string, message: string): Promise<TutorChatResponse> {
  return request<TutorChatResponse>(`/api/submissions/${id}/tutor/messages`, {
    method: "POST",
    body: JSON.stringify({ message })
  });
}

export function fetchJavaLspStatus(): Promise<JavaLspCompletionResponse> {
  return request<JavaLspCompletionResponse>("/api/editor/java-lsp/status");
}

export function fetchJavaLspCompletions(payload: JavaLspCompletionRequest): Promise<JavaLspCompletionResponse> {
  return request<JavaLspCompletionResponse>("/api/editor/java-lsp/completion", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchJavaLspDiagnostics(payload: JavaLspCompletionRequest): Promise<JavaLspDiagnosticsResponse> {
  return request<JavaLspDiagnosticsResponse>("/api/editor/java-lsp/diagnostics", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchJavaLspHover(payload: JavaLspPositionRequest): Promise<JavaLspHoverResponse> {
  return request<JavaLspHoverResponse>("/api/editor/java-lsp/hover", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchJavaLspSignatureHelp(payload: JavaLspPositionRequest): Promise<JavaLspSignatureHelpResponse> {
  return request<JavaLspSignatureHelpResponse>("/api/editor/java-lsp/signature-help", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
