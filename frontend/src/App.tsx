import { useEffect, useMemo, useRef, useState } from "react";
import {
  createProblemDraft,
  deleteProblemDraft,
  fetchProblem,
  fetchProblems,
  fetchSubmissionDetail,
  fetchSubmissionHint,
  fetchSubmissionHistory,
  fetchTutorMessages,
  publishProblemDraft,
  regenerateProblemDraft,
  runSubmission,
  saveDraftFeedback,
  sendTutorMessage
} from "./api";
import { CodingPanel } from "./components/CodingPanel";
import { ProblemRail } from "./components/ProblemRail";
import { ProblemStatement } from "./components/ProblemStatement";
import type {
  Difficulty,
  InterviewStyle,
  Language,
  Problem,
  ProblemDraft,
  ProblemSummary,
  SubmissionDetail,
  SubmissionResult,
  SubmissionSummary,
  TutorHint,
  TutorMessage
} from "./types";
import type { ResultView } from "./ui";

function parseTargetConcepts(value: string) {
  return value
    .split(",")
    .map((concept) => concept.trim())
    .filter(Boolean);
}

function App() {
  const [problems, setProblems] = useState<ProblemSummary[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [draft, setDraft] = useState<ProblemDraft | null>(null);
  const [generatorTopic, setGeneratorTopic] = useState("arrays");
  const [generatorDifficulty, setGeneratorDifficulty] = useState<Difficulty>("Easy");
  const [generatorTargetConcepts, setGeneratorTargetConcepts] = useState("");
  const [generatorConstraintsNotes, setGeneratorConstraintsNotes] = useState("");
  const [generatorInterviewStyle, setGeneratorInterviewStyle] = useState<InterviewStyle>("Classic");
  const [draftFeedbackTags, setDraftFeedbackTags] = useState<string[]>([]);
  const [draftFeedbackNotes, setDraftFeedbackNotes] = useState("");
  const [language, setLanguage] = useState<Language>("python");
  const [code, setCode] = useState("");
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [resultView, setResultView] = useState<ResultView>("current");
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
  const [selectedSubmissionId, setSelectedSubmissionId] = useState<string>("");
  const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetail | null>(null);
  const [tutorHint, setTutorHint] = useState<TutorHint | null>(null);
  const [tutorHintSubmissionId, setTutorHintSubmissionId] = useState<string>("");
  const [tutorMessages, setTutorMessages] = useState<TutorMessage[]>([]);
  const [tutorMessagesSubmissionId, setTutorMessagesSubmissionId] = useState<string>("");
  const [isRunning, setIsRunning] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isRegeneratingDraft, setIsRegeneratingDraft] = useState(false);
  const [isSavingDraftFeedback, setIsSavingDraftFeedback] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isLoadingSubmission, setIsLoadingSubmission] = useState(false);
  const [isLoadingTutorHint, setIsLoadingTutorHint] = useState(false);
  const [isLoadingTutorMessages, setIsLoadingTutorMessages] = useState(false);
  const [isSendingTutorMessage, setIsSendingTutorMessage] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const codeOverrideRef = useRef<string | null>(null);
  const selectedSubmissionRequestRef = useRef<string>("");
  const selectedSubmissionIdRef = useRef<string>("");
  const resultSubmissionIdRef = useRef<string>("");
  const selectedProblemIdRef = useRef<string>("");
  const currentDraftIdRef = useRef<string>("");
  const isDraftPreviewRef = useRef(false);
  const problemRequestRef = useRef(0);
  const submissionHistoryRequestRef = useRef(0);
  const runRequestRef = useRef(0);
  const draftRequestRef = useRef(0);
  const tutorHintRequestRef = useRef(0);
  const tutorChatRequestRef = useRef(0);

  const activeProblem = draft?.problem ?? problem;
  const isDraftPreview = draft !== null;
  selectedProblemIdRef.current = selectedId;
  selectedSubmissionIdRef.current = selectedSubmissionId;
  resultSubmissionIdRef.current = result?.submissionId ?? "";
  currentDraftIdRef.current = draft?.id ?? "";
  isDraftPreviewRef.current = isDraftPreview;

  function resetTutorHint() {
    tutorHintRequestRef.current += 1;
    tutorChatRequestRef.current += 1;
    setTutorHint(null);
    setTutorHintSubmissionId("");
    setIsLoadingTutorHint(false);
    setTutorMessages([]);
    setTutorMessagesSubmissionId("");
    setIsLoadingTutorMessages(false);
    setIsSendingTutorMessage(false);
  }

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
    resetTutorHint();
  }

  function resetDraftFeedback() {
    setDraftFeedbackTags([]);
    setDraftFeedbackNotes("");
    setIsSavingDraftFeedback(false);
    setIsRegeneratingDraft(false);
  }

  function isCurrentPublishedProblem(problemId: string) {
    return selectedProblemIdRef.current === problemId && !isDraftPreviewRef.current;
  }

  function isCurrentHistoryRequest(problemId: string, requestId: number) {
    return submissionHistoryRequestRef.current === requestId && isCurrentPublishedProblem(problemId);
  }

  function isCurrentDraftAction(draftId: string, requestId: number) {
    return draftRequestRef.current === requestId && currentDraftIdRef.current === draftId;
  }

  function isCurrentTutorHintRequest(submissionId: string, requestId: number) {
    return (
      tutorHintRequestRef.current === requestId &&
      (resultSubmissionIdRef.current === submissionId || selectedSubmissionIdRef.current === submissionId)
    );
  }

  function isCurrentTutorConversationRequest(submissionId: string, requestId: number) {
    return (
      tutorChatRequestRef.current === requestId &&
      (resultSubmissionIdRef.current === submissionId || selectedSubmissionIdRef.current === submissionId)
    );
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
    resetTutorHint();

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
    const requestId = draftRequestRef.current + 1;
    draftRequestRef.current = requestId;

    setIsGenerating(true);
    setError(null);
    setResult(null);

    try {
      const nextDraft = await createProblemDraft({
        topic: generatorTopic.trim() || undefined,
        difficulty: generatorDifficulty,
        targetConcepts: parseTargetConcepts(generatorTargetConcepts),
        constraintsNotes: generatorConstraintsNotes.trim() || undefined,
        interviewStyle: generatorInterviewStyle
      });
      if (draftRequestRef.current !== requestId) {
        deleteProblemDraft(nextDraft.id).catch(() => {});
        return;
      }

      isDraftPreviewRef.current = true;
      currentDraftIdRef.current = nextDraft.id;
      setDraft(nextDraft);
      resetDraftFeedback();
      resetSubmissionHistory();
      setCode(nextDraft.problem.starterCode[language]);
      if (previousDraftId && previousDraftId !== nextDraft.id) {
        deleteProblemDraft(previousDraftId).catch(() => {});
      }
    } catch (err) {
      if (draftRequestRef.current === requestId) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (draftRequestRef.current === requestId) {
        setIsGenerating(false);
      }
    }
  }

  async function handlePublishDraft() {
    if (!draft) {
      return;
    }

    const draftId = draft.id;
    const requestId = draftRequestRef.current + 1;
    draftRequestRef.current = requestId;

    setIsPublishing(true);
    setError(null);

    try {
      const published = await publishProblemDraft(draftId);
      if (!isCurrentDraftAction(draftId, requestId)) {
        return;
      }

      selectedProblemIdRef.current = published.id;
      isDraftPreviewRef.current = false;
      currentDraftIdRef.current = "";
      const publishedSummary: ProblemSummary = {
        id: published.id,
        title: published.title,
        difficulty: published.difficulty,
        tags: published.tags
      };

      setProblems((items) => [...items.filter((item) => item.id !== published.id), publishedSummary]);
      setDraft(null);
      resetDraftFeedback();
      setProblem(published);
      setSelectedId(published.id);
      setCode(published.starterCode[language]);
      setResultView("current");
      await refreshSubmissionHistory(published.id);
    } catch (err) {
      if (isCurrentDraftAction(draftId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (draftRequestRef.current === requestId) {
        setIsPublishing(false);
      }
    }
  }

  async function handleDiscardDraft() {
    if (!draft) {
      return;
    }

    const draftId = draft.id;
    const requestId = draftRequestRef.current + 1;
    draftRequestRef.current = requestId;

    setError(null);

    try {
      await deleteProblemDraft(draftId);
      if (!isCurrentDraftAction(draftId, requestId)) {
        return;
      }

      isDraftPreviewRef.current = false;
      currentDraftIdRef.current = "";
      setDraft(null);
      resetDraftFeedback();
      if (problem) {
        setCode(problem.starterCode[language]);
      }
    } catch (err) {
      if (isCurrentDraftAction(draftId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    }
  }

  function handleSelectProblem(id: string) {
    if (draft) {
      deleteProblemDraft(draft.id).catch(() => {});
    }
    draftRequestRef.current += 1;
    selectedProblemIdRef.current = id;
    currentDraftIdRef.current = "";
    isDraftPreviewRef.current = false;
    problemRequestRef.current += 1;
    resetSubmissionHistory();
    runRequestRef.current += 1;
    setIsRunning(false);
    setIsGenerating(false);
    setIsRegeneratingDraft(false);
    setIsSavingDraftFeedback(false);
    setIsPublishing(false);
    setDraft(null);
    resetDraftFeedback();
    setSelectedId(id);
  }

  function handleDraftFeedbackTagToggle(tag: string) {
    setDraftFeedbackTags((current) =>
      current.includes(tag) ? current.filter((value) => value !== tag) : [...current, tag]
    );
  }

  async function handleSaveDraftFeedback() {
    if (!draft) {
      return;
    }

    const draftId = draft.id;
    const requestId = draftRequestRef.current;
    setIsSavingDraftFeedback(true);
    setError(null);

    try {
      const attempt = await saveDraftFeedback(draftId, {
        tags: draftFeedbackTags,
        notes: draftFeedbackNotes.trim() || undefined
      });
      if (isCurrentDraftAction(draftId, requestId)) {
        setDraft((current) => (current?.id === draftId ? { ...current, generationAttempt: attempt } : current));
      }
    } catch (err) {
      if (isCurrentDraftAction(draftId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentDraftAction(draftId, requestId)) {
        setIsSavingDraftFeedback(false);
      }
    }
  }

  async function handleRegenerateDraft(action: string) {
    if (!draft) {
      return;
    }

    const draftId = draft.id;
    const requestId = draftRequestRef.current + 1;
    draftRequestRef.current = requestId;
    setIsRegeneratingDraft(true);
    setError(null);
    setResult(null);

    try {
      const nextDraft = await regenerateProblemDraft(draftId, {
        action,
        tags: draftFeedbackTags,
        notes: draftFeedbackNotes.trim() || undefined
      });
      if (!isCurrentDraftAction(draftId, requestId)) {
        deleteProblemDraft(nextDraft.id).catch(() => {});
        return;
      }

      isDraftPreviewRef.current = true;
      currentDraftIdRef.current = nextDraft.id;
      setDraft(nextDraft);
      resetDraftFeedback();
      resetSubmissionHistory();
      setCode(nextDraft.problem.starterCode[language]);
    } catch (err) {
      if (isCurrentDraftAction(draftId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (draftRequestRef.current === requestId) {
        setIsRegeneratingDraft(false);
      }
    }
  }

  async function handleSelectSubmission(summary: SubmissionSummary) {
    selectedSubmissionRequestRef.current = summary.id;
    setResultView("history");
    setSelectedSubmissionId(summary.id);
    setSelectedSubmission(null);
    setIsLoadingSubmission(true);
    setError(null);
    resetTutorHint();

    try {
      const detail = await fetchSubmissionDetail(summary.id);
      if (selectedSubmissionRequestRef.current === summary.id) {
        setSelectedSubmission(detail);
        void loadTutorMessages(summary.id);
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

  async function handleRequestTutorHint(submissionId: string) {
    if (!submissionId) {
      return;
    }

    const requestId = tutorHintRequestRef.current + 1;
    tutorHintRequestRef.current = requestId;
    setTutorHint(null);
    setTutorHintSubmissionId(submissionId);
    setIsLoadingTutorHint(true);
    setError(null);

    try {
      const hint = await fetchSubmissionHint(submissionId);
      if (isCurrentTutorHintRequest(submissionId, requestId)) {
        setTutorHint(hint);
      }
    } catch (err) {
      if (isCurrentTutorHintRequest(submissionId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentTutorHintRequest(submissionId, requestId)) {
        setIsLoadingTutorHint(false);
      }
    }
  }

  async function loadTutorMessages(submissionId: string) {
    const requestId = tutorChatRequestRef.current + 1;
    tutorChatRequestRef.current = requestId;
    setTutorMessages([]);
    setTutorMessagesSubmissionId(submissionId);
    setIsLoadingTutorMessages(true);

    try {
      const messages = await fetchTutorMessages(submissionId);
      if (isCurrentTutorConversationRequest(submissionId, requestId)) {
        setTutorMessages(messages);
      }
    } catch (err) {
      if (isCurrentTutorConversationRequest(submissionId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentTutorConversationRequest(submissionId, requestId)) {
        setIsLoadingTutorMessages(false);
      }
    }
  }

  async function handleSendTutorMessage(submissionId: string, message: string) {
    if (!submissionId || !message.trim()) {
      return;
    }

    const requestId = tutorChatRequestRef.current + 1;
    tutorChatRequestRef.current = requestId;
    setTutorMessagesSubmissionId(submissionId);
    setIsSendingTutorMessage(true);
    setError(null);

    try {
      const response = await sendTutorMessage(submissionId, message);
      if (isCurrentTutorConversationRequest(submissionId, requestId)) {
        setTutorMessages(response.messages);
      }
    } catch (err) {
      if (isCurrentTutorConversationRequest(submissionId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentTutorConversationRequest(submissionId, requestId)) {
        setIsSendingTutorMessage(false);
      }
    }
  }

  return (
    <main className="app-shell">
      <ProblemRail
        draft={draft}
        generatorConstraintsNotes={generatorConstraintsNotes}
        generatorDifficulty={generatorDifficulty}
        generatorInterviewStyle={generatorInterviewStyle}
        generatorTargetConcepts={generatorTargetConcepts}
        generatorTopic={generatorTopic}
        isGenerating={isGenerating}
        isPublishing={isPublishing}
        onDiscardDraft={handleDiscardDraft}
        onGenerate={handleGenerate}
        onGeneratorConstraintsNotesChange={setGeneratorConstraintsNotes}
        onGeneratorDifficultyChange={setGeneratorDifficulty}
        onGeneratorInterviewStyleChange={setGeneratorInterviewStyle}
        onGeneratorTargetConceptsChange={setGeneratorTargetConcepts}
        onGeneratorTopicChange={setGeneratorTopic}
        onPublishDraft={handlePublishDraft}
        onSelectProblem={handleSelectProblem}
        problems={problems}
        selectedId={selectedId}
      />

      <section className="app-main">
        <header className="topbar">
          <div>
            <span>Studio</span>
            <strong>{activeProblem?.title ?? "Loading problem"}</strong>
          </div>
          <div className="topbar-status">
            {draft ? <span>Draft ID: {draft.id}</span> : <span>Local Mode</span>}
            {draft?.generationMetadata.provider ? <span>{draft.generationMetadata.provider}</span> : null}
            {draft ? (
              <button disabled={isSavingDraftFeedback || isRegeneratingDraft} onClick={handleSaveDraftFeedback} type="button">
                {isSavingDraftFeedback ? "Saving..." : "Save Feedback"}
              </button>
            ) : null}
            <button className="topbar-primary" disabled={isGenerating} onClick={handleGenerate} type="button">
              {isGenerating ? "Generating..." : "Generate Draft"}
            </button>
          </div>
        </header>

        <section className="workspace">
          {error && <div className="alert">{error}</div>}

          {activeProblem ? (
            <>
              <ProblemStatement
                draftFeedbackNotes={draftFeedbackNotes}
                draftFeedbackTags={draftFeedbackTags}
                generationAttempt={draft?.generationAttempt}
                generationMetadata={draft?.generationMetadata}
                isDraftPreview={isDraftPreview}
                isRegeneratingDraft={isRegeneratingDraft}
                isSavingDraftFeedback={isSavingDraftFeedback}
                onDraftFeedbackNotesChange={setDraftFeedbackNotes}
                onDraftFeedbackTagToggle={handleDraftFeedbackTagToggle}
                onRegenerateDraft={handleRegenerateDraft}
                onSaveDraftFeedback={handleSaveDraftFeedback}
                problem={activeProblem}
                quality={draft?.quality}
              />
              <CodingPanel
                activeProblem={activeProblem}
                code={code}
                draft={draft}
                isDraftPreview={isDraftPreview}
                isLoadingHistory={isLoadingHistory}
                isLoadingSubmission={isLoadingSubmission}
                isLoadingTutorHint={isLoadingTutorHint}
                isLoadingTutorMessages={isLoadingTutorMessages}
                isPublishing={isPublishing}
                isRunning={isRunning}
                isSendingTutorMessage={isSendingTutorMessage}
                language={language}
                onCodeChange={setCode}
                onDiscardDraft={handleDiscardDraft}
                onLanguageChange={setLanguage}
                onLoadSubmissionCode={handleLoadSubmissionCode}
                onPublishDraft={handlePublishDraft}
                onResultViewChange={setResultView}
                onRequestTutorHint={handleRequestTutorHint}
                onRun={handleRun}
                onSelectSubmission={handleSelectSubmission}
                onSendTutorMessage={handleSendTutorMessage}
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
            </>
          ) : (
            <div className="loading">Loading problem...</div>
          )}
        </section>
      </section>
    </main>
  );
}

export default App;
