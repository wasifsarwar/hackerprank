import { useEffect, useRef, useState } from "react";
import Editor, { type Monaco, type OnMount } from "@monaco-editor/react";
import {
  fetchJavaLspCompletions,
  fetchJavaLspDiagnostics,
  fetchJavaLspHover,
  fetchJavaLspSignatureHelp,
  fetchJavaLspStatus
} from "../api";
import type {
  JavaLspDiagnostic,
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
  const monacoRef = useRef<Monaco | null>(null);
  const javaLspRetryAfterRef = useRef(0);
  const [editorReady, setEditorReady] = useState(false);
  const [javaLspStatus, setJavaLspStatus] = useState<"checking" | "enabled" | "disabled">("checking");
  const [javaLspDiagnosticCount, setJavaLspDiagnosticCount] = useState(0);

  const handleEditorMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
    setEditorReady(true);
  };

  useEffect(() => {
    let isCurrent = true;
    const retryDelayMs = 5000;

    function shouldSkipJavaLspRequest() {
      return Date.now() < javaLspRetryAfterRef.current;
    }

    function recordJavaLspAvailability(enabled: boolean) {
      javaLspRetryAfterRef.current = enabled ? 0 : Date.now() + retryDelayMs;
      if (isCurrent) {
        setJavaLspStatus(enabled ? "enabled" : "disabled");
      }
    }

    function recordJavaLspTransientFailure() {
      javaLspRetryAfterRef.current = Date.now() + retryDelayMs;
      if (isCurrent) {
        setJavaLspStatus("disabled");
      }
    }

    setJavaLspCompletionProvider(async (sourceCode, lineNumber, column) => {
      if (shouldSkipJavaLspRequest()) {
        return [];
      }
      try {
        const response = await fetchJavaLspCompletions({
          code: sourceCode,
          lineNumber,
          column
        });
        recordJavaLspAvailability(response.enabled);
        return response.enabled ? response.items : [];
      } catch {
        recordJavaLspTransientFailure();
        return [];
      }
    });

    setJavaLspHoverProvider(async (sourceCode, lineNumber, column) => {
      if (shouldSkipJavaLspRequest()) {
        return null;
      }
      try {
        const response = await fetchJavaLspHover({
          code: sourceCode,
          lineNumber,
          column
        });
        recordJavaLspAvailability(response.enabled);
        return response.enabled && response.contents ? { contents: response.contents } : null;
      } catch {
        recordJavaLspTransientFailure();
        return null;
      }
    });

    setJavaLspSignatureHelpProvider(async (sourceCode, lineNumber, column) => {
      if (shouldSkipJavaLspRequest()) {
        return null;
      }
      try {
        const response = await fetchJavaLspSignatureHelp({
          code: sourceCode,
          lineNumber,
          column
        });
        recordJavaLspAvailability(response.enabled);
        return response.enabled ? response : null;
      } catch {
        recordJavaLspTransientFailure();
        return null;
      }
    });

    fetchJavaLspStatus()
      .then((response) => {
        recordJavaLspAvailability(response.enabled);
      })
      .catch(() => {
        javaLspRetryAfterRef.current = Date.now() + retryDelayMs;
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

  useEffect(() => {
    let isCurrent = true;
    const monaco = monacoRef.current;
    const editor = editorRef.current;
    const model = editor?.getModel();

    if (!monaco || !editor || !model || language !== "java") {
      if (monaco && model) {
        monaco.editor.setModelMarkers(model, "jdtls", []);
      }
      setJavaLspDiagnosticCount(0);
      return () => {
        isCurrent = false;
      };
    }
    const currentMonaco = monaco;
    const currentEditor = editor;
    const currentModel = model;

    function requestDiagnostics(retryEmptyResponse: boolean) {
      fetchJavaLspDiagnostics({
        code,
        lineNumber: Math.max(1, currentEditor.getPosition()?.lineNumber ?? 1),
        column: Math.max(1, currentEditor.getPosition()?.column ?? 1)
      })
        .then((response) => {
          if (!isCurrent) {
            return;
          }
          if (!response.enabled) {
            currentMonaco.editor.setModelMarkers(currentModel, "jdtls", []);
            setJavaLspDiagnosticCount(0);
            return;
          }
          if (response.diagnostics.length === 0 && retryEmptyResponse) {
            window.setTimeout(() => {
              if (isCurrent) {
                requestDiagnostics(false);
              }
            }, 1500);
          }
          const markers = response.diagnostics.map((diagnostic) => diagnosticMarker(currentMonaco, diagnostic));
          currentMonaco.editor.setModelMarkers(currentModel, "jdtls", markers);
          setJavaLspDiagnosticCount(markers.length);
        })
        .catch(() => {
          if (isCurrent) {
            currentMonaco.editor.setModelMarkers(currentModel, "jdtls", []);
            setJavaLspDiagnosticCount(0);
          }
        });
    }

    const timeout = window.setTimeout(() => requestDiagnostics(true), 450);

    return () => {
      isCurrent = false;
      window.clearTimeout(timeout);
    };
  }, [activeProblem.id, code, editorReady, language]);

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
                ? `Java 21 + JDT LS${javaLspDiagnosticCount > 0 ? ` • ${javaLspDiagnosticCount} issue${javaLspDiagnosticCount === 1 ? "" : "s"}` : ""}`
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

function diagnosticMarker(monaco: Monaco, diagnostic: JavaLspDiagnostic) {
  return {
    code: diagnostic.code || undefined,
    endColumn: diagnostic.endColumn,
    endLineNumber: diagnostic.endLineNumber,
    message: diagnostic.message,
    severity: markerSeverity(monaco, diagnostic.severity),
    source: diagnostic.source || "jdtls",
    startColumn: diagnostic.startColumn,
    startLineNumber: diagnostic.startLineNumber
  };
}

function markerSeverity(monaco: Monaco, severity: number) {
  switch (severity) {
    case 1:
      return monaco.MarkerSeverity.Error;
    case 2:
      return monaco.MarkerSeverity.Warning;
    case 4:
      return monaco.MarkerSeverity.Hint;
    case 3:
    default:
      return monaco.MarkerSeverity.Info;
  }
}
