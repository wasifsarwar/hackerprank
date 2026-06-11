import { useRef, useState } from "react";
import {
  createProblemDraft,
  deleteProblemDraft,
  publishProblemDraft,
  regenerateProblemDraft,
  saveDraftFeedback
} from "../api";
import type { Difficulty, InterviewStyle, Problem, ProblemDraft } from "../types";

export interface LastGenerationStatus {
  id: string;
  provider: string;
  title: string;
}

interface UseDraftStudioOptions {
  setError: (message: string | null) => void;
  /** Called when a freshly generated or regenerated draft becomes active. */
  onDraftReady: (draft: ProblemDraft) => void;
  /** Called after a draft is successfully published. */
  onPublished: (problem: Problem) => Promise<void> | void;
  /** Called after the active draft is discarded. */
  onDiscarded: () => void;
}

function parseTargetConcepts(value: string) {
  return value
    .split(",")
    .map((concept) => concept.trim())
    .filter(Boolean);
}

/**
 * Owns the generation composer fields, the active draft, and the draft
 * feedback/publish lifecycle. Cross-cutting consequences (resetting history,
 * swapping editor code, updating the catalog) are delegated to callbacks so
 * this hook stays independent of the rest of the workspace.
 */
export function useDraftStudio({ setError, onDraftReady, onPublished, onDiscarded }: UseDraftStudioOptions) {
  const [draft, setDraft] = useState<ProblemDraft | null>(null);
  const [topic, setTopic] = useState("");
  const [difficulty, setDifficulty] = useState<Difficulty>("Easy");
  const [targetConcepts, setTargetConcepts] = useState("");
  const [constraintsNotes, setConstraintsNotes] = useState("");
  const [interviewStyle, setInterviewStyle] = useState<InterviewStyle>("Classic");
  const [lastGenerationStatus, setLastGenerationStatus] = useState<LastGenerationStatus | null>(null);
  const [feedbackTags, setFeedbackTags] = useState<string[]>([]);
  const [feedbackNotes, setFeedbackNotes] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [isSavingFeedback, setIsSavingFeedback] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const draftRequestRef = useRef(0);
  const currentDraftIdRef = useRef("");
  const isDraftPreviewRef = useRef(false);

  const isDraftPreview = draft !== null;
  currentDraftIdRef.current = draft?.id ?? "";
  isDraftPreviewRef.current = isDraftPreview;

  function isCurrentDraftAction(draftId: string, requestId: number) {
    return draftRequestRef.current === requestId && currentDraftIdRef.current === draftId;
  }

  function resetFeedback() {
    setFeedbackTags([]);
    setFeedbackNotes("");
    setIsSavingFeedback(false);
    setIsRegenerating(false);
  }

  function toggleFeedbackTag(tag: string) {
    setFeedbackTags((current) =>
      current.includes(tag) ? current.filter((value) => value !== tag) : [...current, tag]
    );
  }

  /** Drops the active draft (fire-and-forget) when the user navigates away. */
  function abandon() {
    if (draft) {
      deleteProblemDraft(draft.id).catch(() => {});
    }
    draftRequestRef.current += 1;
    currentDraftIdRef.current = "";
    isDraftPreviewRef.current = false;
    setDraft(null);
    resetFeedback();
    setIsGenerating(false);
    setIsPublishing(false);
  }

  async function generate() {
    const previousDraftId = draft?.id;
    const requestId = draftRequestRef.current + 1;
    draftRequestRef.current = requestId;

    setIsGenerating(true);
    setError(null);

    try {
      const nextDraft = await createProblemDraft({
        topic: topic.trim() || undefined,
        difficulty,
        targetConcepts: parseTargetConcepts(targetConcepts),
        constraintsNotes: constraintsNotes.trim() || undefined,
        interviewStyle
      });
      if (draftRequestRef.current !== requestId) {
        deleteProblemDraft(nextDraft.id).catch(() => {});
        return;
      }

      isDraftPreviewRef.current = true;
      currentDraftIdRef.current = nextDraft.id;
      setDraft(nextDraft);
      setLastGenerationStatus({
        id: nextDraft.id,
        provider: nextDraft.generationMetadata.provider,
        title: nextDraft.problem.title
      });
      resetFeedback();
      onDraftReady(nextDraft);
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

  async function publish() {
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

      isDraftPreviewRef.current = false;
      currentDraftIdRef.current = "";
      setDraft(null);
      resetFeedback();
      await onPublished(published);
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

  async function discard() {
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
      resetFeedback();
      onDiscarded();
    } catch (err) {
      if (isCurrentDraftAction(draftId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    }
  }

  async function saveFeedback() {
    if (!draft) {
      return;
    }

    const draftId = draft.id;
    const requestId = draftRequestRef.current;
    setIsSavingFeedback(true);
    setError(null);

    try {
      const attempt = await saveDraftFeedback(draftId, {
        tags: feedbackTags,
        notes: feedbackNotes.trim() || undefined
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
        setIsSavingFeedback(false);
      }
    }
  }

  async function regenerate(action: string) {
    if (!draft) {
      return;
    }

    const draftId = draft.id;
    const requestId = draftRequestRef.current + 1;
    draftRequestRef.current = requestId;
    setIsRegenerating(true);
    setError(null);

    try {
      const nextDraft = await regenerateProblemDraft(draftId, {
        action,
        tags: feedbackTags,
        notes: feedbackNotes.trim() || undefined
      });
      if (!isCurrentDraftAction(draftId, requestId)) {
        deleteProblemDraft(nextDraft.id).catch(() => {});
        return;
      }

      isDraftPreviewRef.current = true;
      currentDraftIdRef.current = nextDraft.id;
      setDraft(nextDraft);
      resetFeedback();
      onDraftReady(nextDraft);
    } catch (err) {
      if (isCurrentDraftAction(draftId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (draftRequestRef.current === requestId) {
        setIsRegenerating(false);
      }
    }
  }

  return {
    draft,
    isDraftPreview,
    isDraftPreviewRef,
    topic,
    setTopic,
    difficulty,
    setDifficulty,
    targetConcepts,
    setTargetConcepts,
    constraintsNotes,
    setConstraintsNotes,
    interviewStyle,
    setInterviewStyle,
    lastGenerationStatus,
    feedbackTags,
    feedbackNotes,
    setFeedbackNotes,
    toggleFeedbackTag,
    isGenerating,
    isRegenerating,
    isSavingFeedback,
    isPublishing,
    abandon,
    generate,
    publish,
    discard,
    saveFeedback,
    regenerate
  };
}
