import { useEffect, useMemo, useState } from "react";
import Editor from "@monaco-editor/react";
import {
  createProblemDraft,
  deleteProblemDraft,
  fetchProblem,
  fetchProblems,
  publishProblemDraft,
  runSubmission
} from "./api";
import type { Difficulty, Language, Problem, ProblemDraft, ProblemSummary, SubmissionResult } from "./types";

const languageLabels: Record<Language, string> = {
  python: "Python",
  java: "Java"
};

const editorLanguages: Record<Language, string> = {
  python: "python",
  java: "java"
};

const difficultyOptions: Difficulty[] = ["Easy", "Medium", "Hard"];

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
  const [isRunning, setIsRunning] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activeProblem = draft?.problem ?? problem;
  const isDraftPreview = draft !== null;

  useEffect(() => {
    fetchProblems()
      .then((items) => {
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
    fetchProblem(selectedId)
      .then((loaded) => {
        setProblem(loaded);
        setCode(loaded.starterCode[language]);
      })
      .catch((err: Error) => setError(err.message));
  }, [selectedId]);

  useEffect(() => {
    if (activeProblem) {
      setCode(activeProblem.starterCode[language]);
      setResult(null);
    }
  }, [activeProblem?.id, language]);

  const statusTone = useMemo(() => {
    if (!result) {
      return "idle";
    }

    return result.status === "ACCEPTED" ? "success" : "danger";
  }, [result]);

  async function handleRun(runHiddenTests: boolean) {
    if (!problem || isDraftPreview) {
      return;
    }

    setIsRunning(true);
    setError(null);
    setResult(null);

    try {
      const nextResult = await runSubmission({
        problemId: problem.id,
        language,
        code,
        runHiddenTests
      });
      setResult(nextResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setIsRunning(false);
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
      setDraft(nextDraft);
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
    setDraft(null);
    setSelectedId(id);
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

              <section className={`results ${statusTone}`}>
                <div className="result-summary">
                  <h3>{isDraftPreview ? "Draft" : result ? result.status.replace(/_/g, " ") : "Results"}</h3>
                  {draft && <span>{draft.validationStatus.toLowerCase()}</span>}
                  {!isDraftPreview && isRunning && <span>Running...</span>}
                  {!isDraftPreview && result && (
                    <span>
                      {result.passedCount}/{result.totalCount} passed
                    </span>
                  )}
                </div>

                {!isDraftPreview && result?.compileOutput && (
                  <pre className="compile-output">{result.compileOutput}</pre>
                )}

                <div className="test-results">
                  {!isDraftPreview && result?.results.map((test) => (
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
                  ))}
                </div>
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
