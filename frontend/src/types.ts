export type Language = "python" | "java";
export type Difficulty = "Easy" | "Medium" | "Hard";
export type InterviewStyle = "Classic" | "Edge-case heavy" | "Performance" | "Practical";

export type StarterCode = Record<Language, string>;

export interface Example {
  input: string;
  output: string;
  explanation: string;
}

export interface ProblemSummary {
  id: string;
  title: string;
  difficulty: Difficulty;
  tags: string[];
}

export interface Problem extends ProblemSummary {
  description: string;
  inputFormat: string;
  outputFormat: string;
  constraints: string[];
  examples: Example[];
  starterCode: StarterCode;
}

export interface GenerateProblemRequest {
  topic?: string;
  difficulty?: Difficulty;
  targetConcepts?: string[];
  constraintsNotes?: string;
  interviewStyle?: InterviewStyle;
}

export interface GenerationMetadata {
  provider: string;
  modelId: string;
  promptVersion: string;
  validationStatus: string;
  validationSummary: string;
  intendedTechnique: string;
}

export interface DraftQualityCheck {
  label: string;
  status: "PASSED";
  detail: string;
}

export interface DraftQuality {
  status: "VALIDATED";
  summary: string;
  repairUsed: boolean;
  exampleCount: number;
  visibleTestCount: number;
  hiddenTestCount: number;
  totalTestCount: number;
  checks: DraftQualityCheck[];
}

export interface GenerationAttempt {
  id: string;
  outcome: "DRAFTED" | "PUBLISHED" | "DISCARDED" | "REGENERATED";
  feedbackTags: string[];
  feedbackNotes: string;
  createdAt: string;
  updatedAt: string;
}

export interface DraftFeedbackRequest {
  tags: string[];
  notes?: string;
}

export interface RegenerateDraftRequest extends DraftFeedbackRequest {
  action?: string;
}

export interface ProblemDraft {
  id: string;
  topic: string;
  difficulty: Difficulty;
  validationStatus: "VALIDATED";
  createdAt: string;
  generationMetadata: GenerationMetadata;
  quality: DraftQuality;
  generationAttempt?: GenerationAttempt | null;
  problem: Problem;
}

export interface SubmissionRequest {
  problemId: string;
  language: Language;
  code: string;
  runHiddenTests: boolean;
}

export interface TestCaseResult {
  name: string;
  hidden: boolean;
  passed: boolean;
  expectedOutput?: string;
  actualOutput: string;
  stderr: string;
  timedOut: boolean;
  exitCode: number;
  runtimeMs: number;
}

export interface SubmissionResult {
  submissionId?: string;
  createdAt?: string;
  status: "ACCEPTED" | "WRONG_ANSWER" | "RUNTIME_ERROR" | "COMPILE_ERROR" | "TIME_LIMIT_EXCEEDED";
  passedCount: number;
  totalCount: number;
  compileOutput: string;
  results: TestCaseResult[];
}

export interface TutorHint {
  submissionId: string;
  status: SubmissionResult["status"];
  provider: string;
  level: string;
  summary: string;
  hints: string[];
  nextStep: string;
}

export interface TutorMessage {
  id: string;
  submissionId: string;
  role: "user" | "assistant";
  provider: string;
  content: string;
  createdAt: string;
}

export interface TutorChatResponse {
  submissionId: string;
  messages: TutorMessage[];
}

export interface JavaLspCompletionRequest {
  code: string;
  lineNumber: number;
  column: number;
}

export type JavaLspPositionRequest = JavaLspCompletionRequest;

export interface JavaLspTextEdit {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
  text: string;
}

export interface JavaLspCompletionItem {
  label: string;
  detail: string;
  insertText: string;
  kind: string;
  additionalTextEdits: JavaLspTextEdit[];
}

export interface JavaLspDiagnostic {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
  severity: number;
  message: string;
  source: string;
  code: string;
}

export interface JavaLspCompletionResponse {
  enabled: boolean;
  source: string;
  message: string;
  items: JavaLspCompletionItem[];
}

export interface JavaLspDiagnosticsResponse {
  enabled: boolean;
  source: string;
  message: string;
  diagnostics: JavaLspDiagnostic[];
}

export interface JavaLspHoverResponse {
  enabled: boolean;
  source: string;
  message: string;
  contents: string;
}

export interface JavaLspSignature {
  label: string;
  documentation: string;
  parameters: string[];
}

export interface JavaLspSignatureHelpResponse {
  enabled: boolean;
  source: string;
  message: string;
  activeSignature: number;
  activeParameter: number;
  signatures: JavaLspSignature[];
}

export interface SubmissionSummary {
  id: string;
  problemId: string | null;
  problemTitle: string;
  problemDifficulty: Difficulty;
  language: Language;
  status: SubmissionResult["status"];
  passedCount: number;
  totalCount: number;
  createdAt: string;
}

export interface SubmissionDetail extends SubmissionSummary {
  code: string;
  runHiddenTests: boolean;
  compileOutput: string;
  results: TestCaseResult[];
}
