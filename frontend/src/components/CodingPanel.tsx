import Editor from "@monaco-editor/react";
import type {
  Language,
  Problem,
  ProblemDraft,
  SubmissionDetail,
  SubmissionResult,
  SubmissionSummary
} from "../types";
import type { ResultView } from "../ui";
import { editorLanguages, languageLabels } from "../ui";
import { ResultsPanel } from "./ResultsPanel";

interface CodingPanelProps {
  activeProblem: Problem;
  code: string;
  draft: ProblemDraft | null;
  isDraftPreview: boolean;
  isLoadingHistory: boolean;
  isLoadingSubmission: boolean;
  isPublishing: boolean;
  isRunning: boolean;
  language: Language;
  onCodeChange: (code: string) => void;
  onDiscardDraft: () => void;
  onLanguageChange: (language: Language) => void;
  onLoadSubmissionCode: () => void;
  onPublishDraft: () => void;
  onResultViewChange: (view: ResultView) => void;
  onRun: (runHiddenTests: boolean) => void;
  onSelectSubmission: (summary: SubmissionSummary) => void;
  result: SubmissionResult | null;
  resultView: ResultView;
  resultsTone: string;
  selectedSubmission: SubmissionDetail | null;
  selectedSubmissionId: string;
  submissions: SubmissionSummary[];
}

export function CodingPanel({
  activeProblem,
  code,
  draft,
  isDraftPreview,
  isLoadingHistory,
  isLoadingSubmission,
  isPublishing,
  isRunning,
  language,
  onCodeChange,
  onDiscardDraft,
  onLanguageChange,
  onLoadSubmissionCode,
  onPublishDraft,
  onResultViewChange,
  onRun,
  onSelectSubmission,
  result,
  resultView,
  resultsTone,
  selectedSubmission,
  selectedSubmissionId,
  submissions
}: CodingPanelProps) {
  return (
    <section className="coding-panel">
      <div className="toolbar">
        <div className="language-tabs" aria-label="Language">
          {(Object.keys(languageLabels) as Language[]).map((item) => (
            <button
              className={item === language ? "selected" : ""}
              key={item}
              onClick={() => onLanguageChange(item)}
              type="button"
            >
              {languageLabels[item]}
            </button>
          ))}
        </div>

        {isDraftPreview ? (
          <div className="run-actions">
            <button className="publish-action" disabled={isPublishing} onClick={onPublishDraft} type="button">
              {isPublishing ? "Publishing..." : "Publish"}
            </button>
            <button className="discard-action" disabled={isPublishing} onClick={onDiscardDraft} type="button">
              Discard
            </button>
          </div>
        ) : (
          <div className="run-actions">
            <button disabled={isRunning} onClick={() => onRun(false)} type="button">
              Run Samples
            </button>
            <button disabled={isRunning} onClick={() => onRun(true)} type="button">
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
          onChange={(value) => onCodeChange(value ?? "")}
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

      <ResultsPanel
        draft={draft}
        isDraftPreview={isDraftPreview}
        isLoadingHistory={isLoadingHistory}
        isLoadingSubmission={isLoadingSubmission}
        isRunning={isRunning}
        onLoadSubmissionCode={onLoadSubmissionCode}
        onResultViewChange={onResultViewChange}
        onSelectSubmission={onSelectSubmission}
        result={result}
        resultView={resultView}
        resultsTone={resultsTone}
        selectedSubmission={selectedSubmission}
        selectedSubmissionId={selectedSubmissionId}
        submissions={submissions}
      />
    </section>
  );
}
