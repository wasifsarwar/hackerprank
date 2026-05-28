export type Language = "python" | "java";
export type Difficulty = "Easy" | "Medium" | "Hard";

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
}

export interface GenerationMetadata {
  provider: string;
  modelId: string;
  promptVersion: string;
  parametersJson: string;
  validationStatus: string;
  validationErrors: string;
  validationSummary: string;
  intendedTechnique: string;
}

export interface ProblemDraft {
  id: string;
  topic: string;
  difficulty: Difficulty;
  validationStatus: "VALIDATED";
  createdAt: string;
  generationMetadata: GenerationMetadata;
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
