import { useEffect, useMemo, useRef, useState } from "react";
import Editor from "@monaco-editor/react";
import {
  createProblemDraft,
  deleteProblemDraft,
  fetchProblem,
  fetchProblems,
  fetchSubmissionDetail,
  fetchSubmissionHistory,
  publishProblemDraft,
  runSubmission
} from "./api";
import type {
  Difficulty,
  Language,
  Problem,
  ProblemDraft,
  ProblemSummary,
  SubmissionDetail,
  SubmissionResult,
  SubmissionSummary,
  TestCaseResult
} from "./types";

const languageLabels: Record<Language, string> = {
  python: "Python",
  java: "Java"
};

const editorLanguages: Record<Language, string> = {
  python: "python",
  java: "java"
};

const difficultyOptions: Difficulty[] = ["Easy", "Medium", "Hard"];
type ResultView = "current" | "history";

function formatStatus(status: string) {
  return status.replace(/_/g, " ");
}

function formatTimestamp(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit"
  }).format(new Date(value));
}

function App() {
  const [problems, setProblems] = useState<ProblemSummary[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [draft, setDraft] = useState<ProblemDraft | null>(null);
  const [generatorTopic, setGeneratorTopic] = useState("arrays");
  const [generatorDifficulty, setGeneratorDifficulty] = useState<Difficulty>("Easy");
  const [language, setLanguage] = useState<Language>("python");
  const [code, setCode] = useState("");
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [resultView, setResultView] = useState<ResultView>("current");
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
  const [selectedSubmissionId, setSelectedSubmissionId] = useState<string>("");
  const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetail | null>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isLoadingSubmission, setIsLoadingSubmission] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const codeOverrideRef = useRef<string | null>(null);
  const selectedSubmissionRequestRef = useRef<string>("");
  const selectedProblemIdRef = useRef<string>("");
  const isDraftPreviewRef = useRef(false);
  const problemRequestRef = useRef(0);
  const submissionHistoryRequestRef = useRef(0);
  const runRequestRef = useRef(0);

  const activeProblem = draft?.problem ?? problem;
  const isDraftPreview = draft !== null;
  selectedProblemIdRef.current = selectedId;
  isDraftPreviewRef.current = isDraftPreview;

  function clearSelectedSubmission() {
    setSelectedSubmission(null);
    setSelectedSubmissionId("");
    setIsLoadingSubmission(false);
    selectedSubmissionRequestRef.current = "";
  }

  function resetSubmissionHistory() {
    submissionHistoryRequestRef.current += 1;
    setIsLoadingHistory(false);
    setSubmissions([]);
    clearSelectedSubmission();
  }

  function isCurrentPublishedProblem(problemId: string) {
    return selectedProblemIdRef.current === problemId && !isDraftPreviewRef.current;
  }

  function isCurrentHistoryRequest(problemId: string, requestId: number) {
    return submissionHistoryRequestRef.current === requestId && isCurrentPublishedProblem(problemId);
  }

  useEffect(() => {
    fetchProblems()
      .then((items) => {
        selectedProblemIdRef.current = items[0]?.id ?? "";
        setProblems(items);
        setSelectedId(items[0]?.id ?? "");
      })
      .catch((err: Error) => setError(err.message));
  }, []);

  useEffect(() => {
    if (!selectedId) {
      return;
    }

    setProblem(null);
    setResult(null);
    setResultView("current");
    resetSubmissionHistory();
    runRequestRef.current += 1;
    setIsRunning(false);
    const requestId = problemRequestRef.current + 1;
    problemRequestRef.current = requestId;
    fetchProblem(selectedId)
      .then((loaded) => {
        if (problemRequestRef.current === requestId && selectedProblemIdRef.current === selectedId) {
          setProblem(loaded);
          setCode(loaded.starterCode[language]);
        }
      })
      .catch((err: Error) => {
        if (problemRequestRef.current === requestId && selectedProblemIdRef.current === selectedId) {
          setError(err.message);
        }
      });
  }, [selectedId]);

  useEffect(() => {
    if (activeProblem) {
      if (codeOverrideRef.current !== null) {
        setCode(codeOverrideRef.current);
        codeOverrideRef.current = null;
      } else {
        setCode(activeProblem.starterCode[language]);
      }
      setResult(null);
    }
  }, [activeProblem?.id, language]);

  useEffect(() => {
    if (!problem || isDraftPreview) {
      resetSubmissionHistory();
      return;
    }

    refreshSubmissionHistory(problem.id);
  }, [problem?.id, isDraftPreview]);

  const statusTone = useMemo(() => {
    if (!result) {
      return "idle";
    }

    return result.status === "ACCEPTED" ? "success" : "danger";
  }, [result]);

  const historyTone = useMemo(() => {
    if (!selectedSubmission) {
      return "idle";
    }

    return selectedSubmission.status === "ACCEPTED" ? "success" : "danger";
  }, [selectedSubmission]);

  const resultsTone = resultView === "history" ? historyTone : statusTone;

  async function refreshSubmissionHistory(problemId: string) {
    if (!isCurrentPublishedProblem(problemId)) {
      return;
    }

    const requestId = submissionHistoryRequestRef.current + 1;
    submissionHistoryRequestRef.current = requestId;
    setIsLoadingHistory(true);
    try {
      const items = await fetchSubmissionHistory(problemId);
      if (isCurrentHistoryRequest(problemId, requestId)) {
        setSubmissions(items);
      }
    } catch (err) {
      if (isCurrentHistoryRequest(problemId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentHistoryRequest(problemId, requestId)) {
        setIsLoadingHistory(false);
      }
    }
  }

  async function handleRun(runHiddenTests: boolean) {
    if (!problem || isDraftPreview) {
      return;
    }

    const runProblemId = problem.id;
    const runRequestId = runRequestRef.current + 1;
    runRequestRef.current = runRequestId;
    setIsRunning(true);
    setError(null);
    setResult(null);

    try {
      const nextResult = await runSubmission({
        problemId: runProblemId,
        language,
        code,
        runHiddenTests
      });
      if (runRequestRef.current === runRequestId && isCurrentPublishedProblem(runProblemId)) {
        setResult(nextResult);
        setResultView("current");
      }
      await refreshSubmissionHistory(runProblemId);
    } catch (err) {
      if (runRequestRef.current === runRequestId && isCurrentPublishedProblem(runProblemId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (runRequestRef.current === runRequestId) {
        setIsRunning(false);
      }
    }
  }

  async function handleGenerate() {
    const previousDraftId = draft?.id;

    setIsGenerating(true);
    setError(null);
    setResult(null);

    try {
      const nextDraft = await createProblemDraft({
        topic: generatorTopic.trim() || undefined,
        difficulty: generatorDifficulty
      });
      isDraftPreviewRef.current = true;
      setDraft(nextDraft);
      resetSubmissionHistory();
      setCode(nextDraft.problem.starterCode[language]);
      if (previousDraftId && previousDraftId !== nextDraft.id) {
        deleteProblemDraft(previousDraftId).catch(() => {});
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setIsGenerating(false);
    }
  }

  async function handlePublishDraft() {
    if (!draft) {
      return;
    }

    setIsPublishing(true);
    setError(null);

    try {
      const published = await publishProblemDraft(draft.id);
      selectedProblemIdRef.current = published.id;
      isDraftPreviewRef.current = false;
      const publishedSummary: ProblemSummary = {
        id: published.id,
        title: published.title,
        difficulty: published.difficulty,
        tags: published.tags
      };

      setProblems((items) => [...items.filter((item) => item.id !== published.id), publishedSummary]);
      setDraft(null);
      setProblem(published);
      setSelectedId(published.id);
      setCode(published.starterCode[language]);
      setResultView("current");
      await refreshSubmissionHistory(published.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setIsPublishing(false);
    }
  }

  async function handleDiscardDraft() {
    if (!draft) {
      return;
    }

    setError(null);

    try {
      await deleteProblemDraft(draft.id);
      isDraftPreviewRef.current = false;
      setDraft(null);
      if (problem) {
        setCode(problem.starterCode[language]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    }
  }

  function handleSelectProblem(id: string) {
    if (draft) {
      deleteProblemDraft(draft.id).catch(() => {});
    }
    selectedProblemIdRef.current = id;
    isDraftPreviewRef.current = false;
    problemRequestRef.current += 1;
    resetSubmissionHistory();
    runRequestRef.current += 1;
    setIsRunning(false);
    setDraft(null);
    setSelectedId(id);
  }

  async function handleSelectSubmission(summary: SubmissionSummary) {
    selectedSubmissionRequestRef.current = summary.id;
    setResultView("history");
    setSelectedSubmissionId(summary.id);
    setSelectedSubmission(null);
    setIsLoadingSubmission(true);
    setError(null);

    try {
      const detail = await fetchSubmissionDetail(summary.id);
      if (selectedSubmissionRequestRef.current === summary.id) {
        setSelectedSubmission(detail);
      }
    } catch (err) {
      if (selectedSubmissionRequestRef.current === summary.id) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (selectedSubmissionRequestRef.current === summary.id) {
        setIsLoadingSubmission(false);
      }
    }
  }

  function handleLoadSubmissionCode() {
    if (!selectedSubmission) {
      return;
    }

    if (selectedSubmission.language !== language) {
      codeOverrideRef.current = selectedSubmission.code;
      setLanguage(selectedSubmission.language);
    } else {
      setCode(selectedSubmission.code);
    }
    setResultView("current");
  }

  function renderTestResults(results: TestCaseResult[]) {
    return results.map((test) => (
      <article className={test.passed ? "test-result pass" : "test-result fail"} key={test.name}>
        <div>
          <strong>{test.name}</strong>
          <span>{test.hidden ? "Hidden" : "Sample"}</span>
        </div>
        <p>
          {test.passed
            ? "Passed"
            : test.timedOut
              ? "Timed out"
              : test.exitCode !== 0
                ? "Runtime error"
                : "Failed"}{" "}
          in {test.runtimeMs}ms
        </p>
        {!test.hidden && (
          <div className="io-grid">
            <div>
              <span>Expected</span>
              <pre>{test.expectedOutput}</pre>
            </div>
            <div>
              <span>Actual</span>
              <pre>{test.actualOutput || "(empty)"}</pre>
            </div>
          </div>
        )}
        {test.stderr && <pre className="stderr">{test.stderr}</pre>}
      </article>
    ));
  }

  return (
    <main className="app-shell">
      <aside className="problem-rail" aria-label="Problems">
        <div className="brand">
          <span className="brand-mark">HP</span>
          <div>
            <h1>HackerPrank</h1>
            <p>Local coding practice</p>
          </div>
        </div>

        <form className="generator-panel" onSubmit={(event) => {
          event.preventDefault();
          handleGenerate();
        }}>
          <label>
            <span>Topic</span>
            <input
              onChange={(event) => setGeneratorTopic(event.target.value)}
              placeholder="arrays, stacks, strings"
              value={generatorTopic}
            />
          </label>
          <label>
            <span>Difficulty</span>
            <select
              onChange={(event) => setGeneratorDifficulty(event.target.value as Difficulty)}
              value={generatorDifficulty}
            >
              {difficultyOptions.map((difficulty) => (
                <option key={difficulty} value={difficulty}>
                  {difficulty}
                </option>
              ))}
            </select>
          </label>
          <button className="generate-action" disabled={isGenerating} type="submit">
            {isGenerating ? "Generating..." : "Generate Draft"}
          </button>
        </form>

        {draft && (
          <section className="draft-card">
            <div>
              <span>Draft</span>
              <strong>{draft.problem.title}</strong>
              <small>{draft.validationStatus}</small>
            </div>
            <div className="draft-actions">
              <button className="publish-action" disabled={isPublishing} onClick={handlePublishDraft} type="button">
                {isPublishing ? "Publishing..." : "Publish"}
              </button>
              <button className="discard-action" disabled={isPublishing} onClick={handleDiscardDraft} type="button">
                Discard
              </button>
            </div>
          </section>
        )}

        <nav className="problem-list">
          {problems.map((item) => (
            <button
              className={item.id === selectedId ? "problem-item active" : "problem-item"}
              key={item.id}
              onClick={() => handleSelectProblem(item.id)}
              type="button"
            >
              <span>{item.title}</span>
              <small>{item.difficulty}</small>
            </button>
          ))}
        </nav>
      </aside>

      <section className="workspace">
        {error && <div className="alert">{error}</div>}

        {activeProblem ? (
          <>
            <section className="statement">
              <div className="problem-heading">
                <div>
                  <p className="eyebrow">
                    {activeProblem.difficulty}
                    {isDraftPreview ? " Draft" : ""}
                  </p>
                  <h2>{activeProblem.title}</h2>
                </div>
                <div className="tags">
                  {activeProblem.tags.map((tag) => (
                    <span key={tag}>{tag}</span>
                  ))}
                </div>
              </div>

              <p>{activeProblem.description}</p>

              <div className="format-grid">
                <section>
                  <h3>Input</h3>
                  <p>{activeProblem.inputFormat}</p>
                </section>
                <section>
                  <h3>Output</h3>
                  <p>{activeProblem.outputFormat}</p>
                </section>
              </div>

              <section>
                <h3>Constraints</h3>
                <ul>
                  {activeProblem.constraints.map((constraint) => (
                    <li key={constraint}>{constraint}</li>
                  ))}
                </ul>
              </section>

              {activeProblem.examples.map((example, index) => (
                <section className="example" key={`${example.input}-${index}`}>
                  <h3>Example {index + 1}</h3>
                  <div className="example-grid">
                    <div>
                      <span>Input</span>
                      <pre>{example.input}</pre>
                    </div>
                    <div>
                      <span>Output</span>
                      <pre>{example.output}</pre>
                    </div>
                  </div>
                  <p>{example.explanation}</p>
                </section>
              ))}
            </section>

            <section className="coding-panel">
              <div className="toolbar">
                <div className="language-tabs" aria-label="Language">
                  {(Object.keys(languageLabels) as Language[]).map((item) => (
                    <button
                      className={item === language ? "selected" : ""}
                      key={item}
                      onClick={() => setLanguage(item)}
                      type="button"
                    >
                      {languageLabels[item]}
                    </button>
                  ))}
                </div>

                {isDraftPreview ? (
                  <div className="run-actions">
                    <button className="publish-action" disabled={isPublishing} onClick={handlePublishDraft} type="button">
                      {isPublishing ? "Publishing..." : "Publish"}
                    </button>
                    <button className="discard-action" disabled={isPublishing} onClick={handleDiscardDraft} type="button">
                      Discard
                    </button>
                  </div>
                ) : (
                  <div className="run-actions">
                    <button disabled={isRunning} onClick={() => handleRun(false)} type="button">
                      Run Samples
                    </button>
                    <button disabled={isRunning} onClick={() => handleRun(true)} type="button">
                      Submit
                    </button>
                  </div>
                )}
              </div>

              <div className="code-editor-shell">
                <Editor
                  key={`${activeProblem.id}-${language}`}
                  aria-label="Code editor"
                  defaultLanguage={editorLanguages[language]}
                  language={editorLanguages[language]}
                  loading={<div className="code-editor-loading">Loading editor...</div>}
                  onChange={(value) => setCode(value ?? "")}
                  options={{
                    automaticLayout: true,
                    fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
                    fontSize: 15,
                    formatOnPaste: true,
                    minimap: { enabled: false },
                    padding: { top: 16, bottom: 16 },
                    renderLineHighlight: "all",
                    scrollBeyondLastLine: false,
                    tabSize: 4,
                    wordWrap: "on"
                  }}
                  path={`${activeProblem.id}.${language}`}
                  theme="vs-dark"
                  value={code}
                />
              </div>

              <section className={`results ${resultsTone}`}>
                {!isDraftPreview && (
                  <div className="result-tabs" aria-label="Result view">
                    <button
                      className={resultView === "current" ? "selected" : ""}
                      onClick={() => setResultView("current")}
                      type="button"
                    >
                      Current
                    </button>
                    <button
                      className={resultView === "history" ? "selected" : ""}
                      onClick={() => setResultView("history")}
                      type="button"
                    >
                      History
                    </button>
                  </div>
                )}

                {isDraftPreview ? (
                  <div className="result-summary">
                    <h3>Draft</h3>
                    {draft && <span>{draft.validationStatus.toLowerCase()}</span>}
                  </div>
                ) : resultView === "current" ? (
                  <>
                    <div className="result-summary">
                      <h3>{result ? formatStatus(result.status) : "Results"}</h3>
                      {isRunning && <span>Running...</span>}
                      {result && (
                        <span>
                          {result.passedCount}/{result.totalCount} passed
                        </span>
                      )}
                    </div>

                    {result?.submissionId && result.createdAt && (
                      <div className="submission-meta">
                        <span>{formatTimestamp(result.createdAt)}</span>
                        <code>{result.submissionId}</code>
                      </div>
                    )}

                    {result?.compileOutput && (
                      <pre className="compile-output">{result.compileOutput}</pre>
                    )}

                    {!result && !isRunning && <p className="empty-state">No result yet.</p>}

                    <div className="test-results">
                      {result && renderTestResults(result.results)}
                    </div>
                  </>
                ) : (
                  <>
                    <div className="result-summary">
                      <h3>History</h3>
                      <span>{isLoadingHistory ? "Loading..." : `${submissions.length} saved`}</span>
                    </div>

                    <div className="history-layout">
                      <div className="history-list">
                        {submissions.map((submission) => (
                          <button
                            className={submission.id === selectedSubmissionId ? "history-item selected" : "history-item"}
                            key={submission.id}
                            onClick={() => handleSelectSubmission(submission)}
                            type="button"
                          >
                            <span>{formatStatus(submission.status)}</span>
                            <strong>
                              {submission.passedCount}/{submission.totalCount}
                            </strong>
                            <small>
                              {languageLabels[submission.language]} - {formatTimestamp(submission.createdAt)}
                            </small>
                          </button>
                        ))}
                        {!isLoadingHistory && submissions.length === 0 && (
                          <p className="empty-state">No saved runs.</p>
                        )}
                      </div>

                      <div className="history-detail">
                        {isLoadingSubmission ? (
                          <p className="empty-state">Loading run...</p>
                        ) : selectedSubmission ? (
                          <>
                            <div className="history-detail-header">
                              <div>
                                <span>{formatTimestamp(selectedSubmission.createdAt)}</span>
                                <strong>{formatStatus(selectedSubmission.status)}</strong>
                              </div>
                              <button onClick={handleLoadSubmissionCode} type="button">
                                Load Code
                              </button>
                            </div>

                            <div className="submission-meta">
                              <span>
                                {languageLabels[selectedSubmission.language]} -{" "}
                                {selectedSubmission.runHiddenTests ? "hidden tests" : "samples only"}
                              </span>
                              <code>{selectedSubmission.id}</code>
                            </div>

                            <pre className="history-code">{selectedSubmission.code}</pre>

                            {selectedSubmission.compileOutput && (
                              <pre className="compile-output">{selectedSubmission.compileOutput}</pre>
                            )}

                            <div className="test-results">
                              {renderTestResults(selectedSubmission.results)}
                            </div>
                          </>
                        ) : (
                          <p className="empty-state">Select a saved run.</p>
                        )}
                      </div>
                    </div>
                  </>
                )}
              </section>
            </section>
          </>
        ) : (
          <div className="loading">Loading problem...</div>
        )}
      </section>
    </main>
  );
}

export default App;
