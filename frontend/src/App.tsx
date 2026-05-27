import { useEffect, useMemo, useState } from "react";
import Editor from "@monaco-editor/react";
import { fetchProblem, fetchProblems, runSubmission } from "./api";
import type { Language, Problem, ProblemSummary, SubmissionResult } from "./types";

const languageLabels: Record<Language, string> = {
  python: "Python",
  java: "Java"
};

const editorLanguages: Record<Language, string> = {
  python: "python",
  java: "java"
};

function App() {
  const [problems, setProblems] = useState<ProblemSummary[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [language, setLanguage] = useState<Language>("python");
  const [code, setCode] = useState("");
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
    if (problem) {
      setCode(problem.starterCode[language]);
      setResult(null);
    }
  }, [language]);

  const statusTone = useMemo(() => {
    if (!result) {
      return "idle";
    }

    return result.status === "ACCEPTED" ? "success" : "danger";
  }, [result]);

  async function handleRun(runHiddenTests: boolean) {
    if (!problem) {
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

        <nav className="problem-list">
          {problems.map((item) => (
            <button
              className={item.id === selectedId ? "problem-item active" : "problem-item"}
              key={item.id}
              onClick={() => setSelectedId(item.id)}
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

        {problem ? (
          <>
            <section className="statement">
              <div className="problem-heading">
                <div>
                  <p className="eyebrow">{problem.difficulty}</p>
                  <h2>{problem.title}</h2>
                </div>
                <div className="tags">
                  {problem.tags.map((tag) => (
                    <span key={tag}>{tag}</span>
                  ))}
                </div>
              </div>

              <p>{problem.description}</p>

              <div className="format-grid">
                <section>
                  <h3>Input</h3>
                  <p>{problem.inputFormat}</p>
                </section>
                <section>
                  <h3>Output</h3>
                  <p>{problem.outputFormat}</p>
                </section>
              </div>

              <section>
                <h3>Constraints</h3>
                <ul>
                  {problem.constraints.map((constraint) => (
                    <li key={constraint}>{constraint}</li>
                  ))}
                </ul>
              </section>

              {problem.examples.map((example, index) => (
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

                <div className="run-actions">
                  <button disabled={isRunning} onClick={() => handleRun(false)} type="button">
                    Run Samples
                  </button>
                  <button disabled={isRunning} onClick={() => handleRun(true)} type="button">
                    Submit
                  </button>
                </div>
              </div>

              <div className="code-editor-shell">
                <Editor
                  key={`${problem.id}-${language}`}
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
                  path={`${problem.id}.${language}`}
                  theme="vs-dark"
                  value={code}
                />
              </div>

              <section className={`results ${statusTone}`}>
                <div className="result-summary">
                  <h3>{result ? result.status.replace(/_/g, " ") : "Results"}</h3>
                  {isRunning && <span>Running...</span>}
                  {result && (
                    <span>
                      {result.passedCount}/{result.totalCount} passed
                    </span>
                  )}
                </div>

                {result?.compileOutput && (
                  <pre className="compile-output">{result.compileOutput}</pre>
                )}

                <div className="test-results">
                  {result?.results.map((test) => (
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
