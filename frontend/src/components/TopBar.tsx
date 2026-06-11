import type { ProblemDraft } from "../types";
import type { LastGenerationStatus } from "../hooks/useDraftStudio";

interface TopBarProps {
  autosavedAt: Date | null;
  draft: ProblemDraft | null;
  isGenerating: boolean;
  isRegeneratingDraft: boolean;
  isSavingDraftFeedback: boolean;
  lastGenerationStatus: LastGenerationStatus | null;
  onSaveDraftFeedback: () => void;
  title: string;
}

export function TopBar({
  autosavedAt,
  draft,
  isGenerating,
  isRegeneratingDraft,
  isSavingDraftFeedback,
  lastGenerationStatus,
  onSaveDraftFeedback,
  title
}: TopBarProps) {
  return (
    <header className="topbar">
      <div className="topbar-breadcrumb">
        <span>Studio</span>
        <span aria-hidden="true">&gt;</span>
        <strong>{title}</strong>
      </div>
      <div className="topbar-status">
        {isGenerating ? <span className="autosave-pill">Generating draft...</span> : null}
        {!isGenerating && autosavedAt ? (
          <span className="saved-pill">
            Auto-saved {autosavedAt.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
          </span>
        ) : null}
        {draft ? <span>Draft ID: {draft.id}</span> : <span>Viewing saved problem</span>}
        {draft?.generationMetadata.provider ? <span>{draft.generationMetadata.provider}</span> : null}
        {!draft && lastGenerationStatus ? (
          <span>
            Last draft: {lastGenerationStatus.title} ({lastGenerationStatus.provider})
          </span>
        ) : null}
        {draft ? (
          <button
            disabled={isSavingDraftFeedback || isRegeneratingDraft}
            onClick={onSaveDraftFeedback}
            type="button"
          >
            {isSavingDraftFeedback ? "Saving..." : "Save Feedback"}
          </button>
        ) : null}
      </div>
    </header>
  );
}
