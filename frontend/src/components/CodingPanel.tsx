import { useEffect, useRef, useState } from "react";
import Editor, { type OnMount } from "@monaco-editor/react";
import { fetchJavaLspCompletions, fetchJavaLspHover, fetchJavaLspSignatureHelp, fetchJavaLspStatus } from "../api";
import type {
  Language,
  Problem,
  ProblemDraft,
  SubmissionDetail,
  SubmissionResult,
  SubmissionSummary,
  TutorHint,
  TutorMessage
} from "../types";
import type { ResultView } from "../ui";
import { editorLanguages, languageLabels } from "../ui";
import {
  configureMonacoIntelligence,
  setJavaLspCompletionProvider,
  setJavaLspHoverProvider,
  setJavaLspSignatureHelpProvider
} from "../editorIntelligence";
import { ResultsPanel } from "./ResultsPanel";

interface CodingPanelProps {
  activeProblem: Problem;
  code: string;
  draft: ProblemDraft | null;
  isDraftPreview: boolean;
  isLoadingHistory: boolean;
  isLoadingSubmission: boolean;
  isLoadingTutorHint: boolean;
  isLoadingTutorMessages: boolean;
  isPublishing: boolean;
  isRunning: boolean;
  isSendingTutorMessage: boolean;
  language: Language;
  onCodeChange: (code: string) => void;
  onDiscardDraft: () => void;
  onLanguageChange: (language: Language) => void;
  onLoadSubmissionCode: () => void;
  onPublishDraft: () => void;
  onResetCode: () => void;
  onResultViewChange: (view: ResultView) => void;
  onRun: (runHiddenTests: boolean) => void;
  onRequestTutorHint: (submissionId: string) => void;
  onSelectSubmission: (summary: SubmissionSummary) => void;
  onSendTutorMessage: (submissionId: string, message: string) => Promise<void>;
  result: SubmissionResult | null;
  resultView: ResultView;
  resultsTone: string;
  selectedSubmission: SubmissionDetail | null;
  selectedSubmissionId: string;
  submissions: SubmissionSummary[];
  tutorHint: TutorHint | null;
  tutorHintSubmissionId: string;
  tutorMessages: TutorMessage[];
  tutorMessagesSubmissionId: string;
}

export function CodingPanel({
  activeProblem,
  code,
  draft,
  isDraftPreview,
  isLoadingHistory,
  isLoadingSubmission,
  isLoadingTutorHint,
  isLoadingTutorMessages,
  isPublishing,
  isRunning,
  isSendingTutorMessage,
  language,
  onCodeChange,
  onDiscardDraft,
  onLanguageChange,
  onLoadSubmissionCode,
  onPublishDraft,
  onResetCode,
  onResultViewChange,
  onRun,
  onRequestTutorHint,
  onSelectSubmission,
  onSendTutorMessage,
  result,
  resultView,
  resultsTone,
  selectedSubmission,
  selectedSubmissionId,
  submissions,
  tutorHint,
  tutorHintSubmissionId,
  tutorMessages,
  tutorMessagesSubmissionId
}: CodingPanelProps) {
  const editorRef = useRef<Parameters<OnMount>[0] | null>(null);
  const javaLspEnabledRef = useRef(true);
  const [javaLspStatus, setJavaLspStatus] = useState<"checking" | "enabled" | "disabled">("checking");

  const handleEditorMount: OnMount = (editor) => {
    editorRef.current = editor;
  };

  useEffect(() => {
    let isCurrent = true;

    setJavaLspCompletionProvider(async (sourceCode, lineNumber, column) => {
      if (!javaLspEnabledRef.current) {
        return [];
      }
      const response = await fetchJavaLspCompletions({
        code: sourceCode,
        lineNumber,
        column
      });
      javaLspEnabledRef.current = response.enabled;
      if (isCurrent) {
        setJavaLspStatus(response.enabled ? "enabled" : "disabled");
      }
      return response.enabled ? response.items : [];
    });

    setJavaLspHoverProvider(async (sourceCode, lineNumber, column) => {
      if (!javaLspEnabledRef.current) {
        return null;
      }
      const response = await fetchJavaLspHover({
        code: sourceCode,
        lineNumber,
        column
      });
      javaLspEnabledRef.current = response.enabled;
      if (isCurrent) {
        setJavaLspStatus(response.enabled ? "enabled" : "disabled");
      }
      return response.enabled && response.contents ? { contents: response.contents } : null;
    });

    setJavaLspSignatureHelpProvider(async (sourceCode, lineNumber, column) => {
      if (!javaLspEnabledRef.current) {
        return null;
      }
      const response = await fetchJavaLspSignatureHelp({
        code: sourceCode,
        lineNumber,
        column
      });
      javaLspEnabledRef.current = response.enabled;
      if (isCurrent) {
        setJavaLspStatus(response.enabled ? "enabled" : "disabled");
      }
      return response.enabled ? response : null;
    });

    fetchJavaLspStatus()
      .then((response) => {
        javaLspEnabledRef.current = response.enabled;
        if (isCurrent) {
          setJavaLspStatus(response.enabled ? "enabled" : "disabled");
        }
      })
      .catch(() => {
        javaLspEnabledRef.current = false;
        if (isCurrent) {
          setJavaLspStatus("disabled");
        }
      });

    return () => {
      isCurrent = false;
      setJavaLspCompletionProvider(null);
      setJavaLspHoverProvider(null);
      setJavaLspSignatureHelpProvider(null);
    };
  }, []);

  function handleFormatCode() {
    editorRef.current?.getAction("editor.action.formatDocument")?.run().catch(() => {});
  }

  return (
    <section className="coding-panel">
      <div className="toolbar">
        <div className="editor-toolbar-left">
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
          <span className="runtime-pill">
            {language === "java"
              ? javaLspStatus === "enabled"
                ? "Java 21 + JDT LS"
                : "Java 21"
              : "Python 3.12"}
          </span>
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
            <button className="editor-action" onClick={onResetCode} type="button">
              Reset
            </button>
            <button className="editor-action" onClick={handleFormatCode} type="button">
              Format
            </button>
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
          beforeMount={configureMonacoIntelligence}
          onMount={handleEditorMount}
          onChange={(value) => onCodeChange(value ?? "")}
          options={{
            automaticLayout: true,
            bracketPairColorization: { enabled: true },
            cursorBlinking: "smooth",
            cursorSmoothCaretAnimation: "on",
            fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
            fontSize: 13,
            formatOnPaste: true,
            guides: { bracketPairs: true, indentation: true },
            lineHeight: 21,
            minimap: { enabled: false },
            padding: { top: 16, bottom: 16 },
            quickSuggestions: { comments: false, other: true, strings: true },
            renderLineHighlight: "all",
            scrollBeyondLastLine: false,
            smoothScrolling: true,
            suggest: {
              showClasses: true,
              showFunctions: true,
              showKeywords: true,
              showMethods: true,
              showSnippets: true
            },
            suggestOnTriggerCharacters: true,
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
        isLoadingTutorHint={isLoadingTutorHint}
        isLoadingTutorMessages={isLoadingTutorMessages}
        isRunning={isRunning}
        isSendingTutorMessage={isSendingTutorMessage}
        onLoadSubmissionCode={onLoadSubmissionCode}
        onResultViewChange={onResultViewChange}
        onRequestTutorHint={onRequestTutorHint}
        onSelectSubmission={onSelectSubmission}
        onSendTutorMessage={onSendTutorMessage}
        result={result}
        resultView={resultView}
        resultsTone={resultsTone}
        selectedSubmission={selectedSubmission}
        selectedSubmissionId={selectedSubmissionId}
        submissions={submissions}
        tutorHint={tutorHint}
        tutorHintSubmissionId={tutorHintSubmissionId}
        tutorMessages={tutorMessages}
        tutorMessagesSubmissionId={tutorMessagesSubmissionId}
      />
    </section>
  );
}
