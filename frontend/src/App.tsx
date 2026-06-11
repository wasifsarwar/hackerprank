import { useEffect, useMemo, useRef, useState } from "react";
import { CodingPanel } from "./components/CodingPanel";
import { ErrorToast } from "./components/ErrorToast";
import { GenerationComposer } from "./components/GenerationComposer";
import { ProblemRail } from "./components/ProblemRail";
import { ProblemStatement } from "./components/ProblemStatement";
import { StatementSkeleton } from "./components/StatementSkeleton";
import { TopBar } from "./components/TopBar";
import { useAutosave } from "./hooks/useAutosave";
import { useDraftStudio } from "./hooks/useDraftStudio";
import { useProblemCatalog } from "./hooks/useProblemCatalog";
import { useSubmissions } from "./hooks/useSubmissions";
import { useTutor } from "./hooks/useTutor";
import { clearStoredCode, loadStoredCode } from "./lib/codeStorage";
import type { Language, Problem, ProblemDraft, ProblemSummary, SubmissionSummary } from "./types";

function App() {
  const [language, setLanguage] = useState<Language>("python");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const codeOverrideRef = useRef<string | null>(null);

  const catalog = useProblemCatalog({ setError });

  const draftStudio = useDraftStudio({
    setError,
    onDraftReady: handleDraftReady,
    onPublished: handlePublished,
    onDiscarded: handleDiscarded
  });

  const submissions = useSubmissions({
    isCurrentPublishedProblem,
    setError
  });

  const tutor = useTutor({
    isSubmissionVisible: submissions.isSubmissionVisible,
    setError
  });

  const activeProblem = draftStudio.draft?.problem ?? catalog.problem;
  const isDraftPreview = draftStudio.isDraftPreview;

  const autosave = useAutosave(activeProblem, language, code, !isDraftPreview);

  function isCurrentPublishedProblem(problemId: string) {
    return catalog.selectedIdRef.current === problemId && !draftStudio.isDraftPreviewRef.current;
  }

  function resetWorkbench() {
    submissions.resetHistory();
    tutor.reset();
  }

  // Load the problem whenever the selection changes, resetting run state.
  useEffect(() => {
    if (!catalog.selectedId) {
      return;
    }

    submissions.clearResult();
    submissions.setResultView("current");
    resetWorkbench();
    submissions.cancelRun();
    void catalog.loadSelected(catalog.selectedId);
  }, [catalog.selectedId]);

  // Swap editor contents when the active problem or language changes.
  useEffect(() => {
    if (activeProblem) {
      if (codeOverrideRef.current !== null) {
        setCode(codeOverrideRef.current);
        codeOverrideRef.current = null;
      } else {
        const stored = isDraftPreview ? null : loadStoredCode(activeProblem.id, language);
        setCode(stored ?? activeProblem.starterCode[language]);
      }
      autosave.resetAutosavedAt();
      submissions.clearResult();
    }
  }, [activeProblem?.id, language]);

  // Keep submission history in sync with the visible published problem.
  useEffect(() => {
    if (!catalog.problem || isDraftPreview) {
      resetWorkbench();
      return;
    }

    void submissions.refreshHistory(catalog.problem.id);
  }, [catalog.problem?.id, isDraftPreview]);

  const statusTone = useMemo(() => {
    if (!submissions.result) {
      return "idle";
    }
    return submissions.result.status === "ACCEPTED" ? "success" : "danger";
  }, [submissions.result]);

  const historyTone = useMemo(() => {
    if (!submissions.selectedSubmission) {
      return "idle";
    }
    return submissions.selectedSubmission.status === "ACCEPTED" ? "success" : "danger";
  }, [submissions.selectedSubmission]);

  const resultsTone = submissions.resultView === "history" ? historyTone : statusTone;

  function handleDraftReady(draft: ProblemDraft) {
    resetWorkbench();
    setCode(draft.problem.starterCode[language]);
  }

  async function handlePublished(published: Problem) {
    const summary: ProblemSummary = {
      id: published.id,
      title: published.title,
      difficulty: published.difficulty,
      tags: published.tags
    };

    catalog.upsertSummary(summary);
    catalog.setProblem(published);
    catalog.select(published.id);
    setCode(published.starterCode[language]);
    submissions.setResultView("current");
    await submissions.refreshHistory(published.id);
  }

  function handleDiscarded() {
    if (catalog.problem) {
      setCode(loadStoredCode(catalog.problem.id, language) ?? catalog.problem.starterCode[language]);
    }
  }

  async function handleRun(runHiddenTests: boolean) {
    if (!catalog.problem || isDraftPreview) {
      return;
    }

    const runProblemId = catalog.problem.id;
    tutor.reset();
    const result = await submissions.run(catalog.problem, language, code, runHiddenTests);
    if (result?.status === "ACCEPTED") {
      catalog.markSolved(runProblemId);
    }
  }

  async function handleGenerate() {
    submissions.clearResult();
    await draftStudio.generate();
  }

  async function handleRegenerateDraft(action: string) {
    submissions.clearResult();
    await draftStudio.regenerate(action);
  }

  function handleSelectProblem(id: string) {
    draftStudio.abandon();
    resetWorkbench();
    submissions.cancelRun();
    catalog.select(id);
  }

  async function handleDeleteProblem(id: string) {
    const target = catalog.problems.find((item) => item.id === id);
    const confirmed = window.confirm(
      `Delete "${target?.title ?? id}"? It is archived and removed from the list; submission history is kept.`
    );
    if (!confirmed) {
      return;
    }

    setError(null);
    try {
      const remaining = await catalog.deleteProblem(id);
      (["python", "java"] as Language[]).forEach((lang) => clearStoredCode(id, lang));
      if (catalog.selectedIdRef.current === id) {
        if (remaining[0]) {
          handleSelectProblem(remaining[0].id);
        } else {
          catalog.clearSelection();
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    }
  }

  function handleResetCode() {
    if (!activeProblem) {
      return;
    }
    if (!isDraftPreview) {
      clearStoredCode(activeProblem.id, language);
    }
    autosave.resetAutosavedAt();
    setCode(activeProblem.starterCode[language]);
  }

  async function handleSelectSubmission(summary: SubmissionSummary) {
    tutor.reset();
    const detail = await submissions.selectSubmission(summary);
    if (detail) {
      void tutor.loadMessages(summary.id);
    }
  }

  function handleLoadSubmissionCode() {
    const selected = submissions.selectedSubmission;
    if (!selected) {
      return;
    }

    if (selected.language !== language) {
      codeOverrideRef.current = selected.code;
      setLanguage(selected.language);
    } else {
      setCode(selected.code);
    }
    submissions.setResultView("current");
  }

  return (
    <main className="app-shell">
      <ProblemRail
        draft={draftStudio.draft}
        isPublishing={draftStudio.isPublishing}
        onDeleteProblem={handleDeleteProblem}
        onDiscardDraft={draftStudio.discard}
        onPublishDraft={draftStudio.publish}
        onSelectProblem={handleSelectProblem}
        problems={catalog.problems}
        selectedId={catalog.selectedId}
        solvedIds={catalog.solvedIds}
      />

      <section className="app-main">
        <TopBar
          autosavedAt={autosave.autosavedAt}
          draft={draftStudio.draft}
          isGenerating={draftStudio.isGenerating}
          isRegeneratingDraft={draftStudio.isRegenerating}
          isSavingDraftFeedback={draftStudio.isSavingFeedback}
          lastGenerationStatus={draftStudio.lastGenerationStatus}
          onSaveDraftFeedback={draftStudio.saveFeedback}
          title={activeProblem?.title ?? "Loading problem"}
        />

        <GenerationComposer
          constraintsNotes={draftStudio.constraintsNotes}
          difficulty={draftStudio.difficulty}
          interviewStyle={draftStudio.interviewStyle}
          isGenerating={draftStudio.isGenerating}
          onConstraintsNotesChange={draftStudio.setConstraintsNotes}
          onDifficultyChange={draftStudio.setDifficulty}
          onGenerate={handleGenerate}
          onInterviewStyleChange={draftStudio.setInterviewStyle}
          onTargetConceptsChange={draftStudio.setTargetConcepts}
          onTopicChange={draftStudio.setTopic}
          targetConcepts={draftStudio.targetConcepts}
          topic={draftStudio.topic}
        />

        <section className="workspace">
          <ErrorToast message={error} onDismiss={() => setError(null)} />

          {activeProblem ? (
            <>
              <ProblemStatement
                draftFeedbackNotes={draftStudio.feedbackNotes}
                draftFeedbackTags={draftStudio.feedbackTags}
                generationAttempt={draftStudio.draft?.generationAttempt}
                generationMetadata={draftStudio.draft?.generationMetadata}
                isDraftPreview={isDraftPreview}
                isRegeneratingDraft={draftStudio.isRegenerating}
                isSavingDraftFeedback={draftStudio.isSavingFeedback}
                onDraftFeedbackNotesChange={draftStudio.setFeedbackNotes}
                onDraftFeedbackTagToggle={draftStudio.toggleFeedbackTag}
                onRegenerateDraft={handleRegenerateDraft}
                onSaveDraftFeedback={draftStudio.saveFeedback}
                problem={activeProblem}
                quality={draftStudio.draft?.quality}
              />
              <CodingPanel
                activeProblem={activeProblem}
                code={code}
                draft={draftStudio.draft}
                isDraftPreview={isDraftPreview}
                isLoadingHistory={submissions.isLoadingHistory}
                isLoadingSubmission={submissions.isLoadingSubmission}
                isLoadingTutorHint={tutor.isLoadingHint}
                isLoadingTutorMessages={tutor.isLoadingMessages}
                isPublishing={draftStudio.isPublishing}
                isRunning={submissions.isRunning}
                isSendingTutorMessage={tutor.isSendingMessage}
                language={language}
                onCodeChange={setCode}
                onDiscardDraft={draftStudio.discard}
                onLanguageChange={setLanguage}
                onLoadSubmissionCode={handleLoadSubmissionCode}
                onPublishDraft={draftStudio.publish}
                onResetCode={handleResetCode}
                onResultViewChange={submissions.setResultView}
                onRequestTutorHint={tutor.requestHint}
                onRun={handleRun}
                onSelectSubmission={handleSelectSubmission}
                onSendTutorMessage={tutor.sendMessage}
                result={submissions.result}
                resultView={submissions.resultView}
                resultsTone={resultsTone}
                selectedSubmission={submissions.selectedSubmission}
                selectedSubmissionId={submissions.selectedSubmissionId}
                submissions={submissions.submissions}
                tutorHint={tutor.hint}
                tutorHintSubmissionId={tutor.hintSubmissionId}
                tutorMessages={tutor.messages}
                tutorMessagesSubmissionId={tutor.messagesSubmissionId}
              />
            </>
          ) : (
            <StatementSkeleton />
          )}
        </section>
      </section>
    </main>
  );
}

export default App;
