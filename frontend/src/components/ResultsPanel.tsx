import type { ProblemDraft, SubmissionDetail, SubmissionResult, SubmissionSummary, TutorHint } from "../types";
import type { ResultView } from "../ui";
import { languageLabels } from "../ui";
import { formatStatus, formatTimestamp } from "../format";
import { TestResults } from "./TestResults";
import { TutorHintCard } from "./TutorHintCard";

interface ResultsPanelProps {
  draft: ProblemDraft | null;
  isDraftPreview: boolean;
  isLoadingHistory: boolean;
  isLoadingSubmission: boolean;
  isLoadingTutorHint: boolean;
  isRunning: boolean;
  onLoadSubmissionCode: () => void;
  onResultViewChange: (view: ResultView) => void;
  onRequestTutorHint: (submissionId: string) => void;
  onSelectSubmission: (summary: SubmissionSummary) => void;
  result: SubmissionResult | null;
  resultView: ResultView;
  resultsTone: string;
  selectedSubmission: SubmissionDetail | null;
  selectedSubmissionId: string;
  submissions: SubmissionSummary[];
  tutorHint: TutorHint | null;
  tutorHintSubmissionId: string;
}

export function ResultsPanel({
  draft,
  isDraftPreview,
  isLoadingHistory,
  isLoadingSubmission,
  isLoadingTutorHint,
  isRunning,
  onLoadSubmissionCode,
  onResultViewChange,
  onRequestTutorHint,
  onSelectSubmission,
  result,
  resultView,
  resultsTone,
  selectedSubmission,
  selectedSubmissionId,
  submissions,
  tutorHint,
  tutorHintSubmissionId
}: ResultsPanelProps) {
  const currentSubmissionId = result?.submissionId ?? "";
  const currentHint = currentSubmissionId === tutorHintSubmissionId ? tutorHint : null;
  const historyHint = selectedSubmissionId === tutorHintSubmissionId ? tutorHint : null;

  return (
    <section className={`results ${resultsTone}`}>
      {!isDraftPreview && (
        <div className="result-tabs" aria-label="Result view">
          <button
            className={resultView === "current" ? "selected" : ""}
            onClick={() => onResultViewChange("current")}
            type="button"
          >
            Current
          </button>
          <button
            className={resultView === "history" ? "selected" : ""}
            onClick={() => onResultViewChange("history")}
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

          {result?.compileOutput && <pre className="compile-output">{result.compileOutput}</pre>}

          {!result && !isRunning && <p className="empty-state">No result yet.</p>}

          {result && (
            <TutorHintCard
              hint={currentHint}
              isLoading={isLoadingTutorHint && currentSubmissionId === tutorHintSubmissionId}
              onRequest={() => onRequestTutorHint(currentSubmissionId)}
              status={result.status}
              submissionId={currentSubmissionId}
            />
          )}

          {result && <TestResults results={result.results} />}
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
                  onClick={() => onSelectSubmission(submission)}
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
              {!isLoadingHistory && submissions.length === 0 && <p className="empty-state">No saved runs.</p>}
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
                    <button onClick={onLoadSubmissionCode} type="button">
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

                  <TutorHintCard
                    hint={historyHint}
                    isLoading={isLoadingTutorHint && selectedSubmission.id === tutorHintSubmissionId}
                    onRequest={() => onRequestTutorHint(selectedSubmission.id)}
                    status={selectedSubmission.status}
                    submissionId={selectedSubmission.id}
                  />

                  <TestResults results={selectedSubmission.results} />
                </>
              ) : (
                <p className="empty-state">Select a saved run.</p>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  );
}
