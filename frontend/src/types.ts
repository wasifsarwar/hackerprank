export type Language = "python" | "java";

export type StarterCode = Record<Language, string>;

export interface Example {
  input: string;
  output: string;
  explanation: string;
}

export interface ProblemSummary {
  id: string;
  title: string;
  difficulty: "Easy" | "Medium" | "Hard";
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
  status: "ACCEPTED" | "WRONG_ANSWER" | "RUNTIME_ERROR" | "COMPILE_ERROR" | "TIME_LIMIT_EXCEEDED";
  passedCount: number;
  totalCount: number;
  compileOutput: string;
  results: TestCaseResult[];
}
