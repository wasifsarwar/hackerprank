import type { TestCaseResult } from "../types";

interface TestResultsProps {
  results: TestCaseResult[];
}

export function TestResults({ results }: TestResultsProps) {
  return (
    <div className="test-results">
      {results.map((test) => (
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
  );
}
