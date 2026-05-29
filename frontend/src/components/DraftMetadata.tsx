import type { DraftQuality, GenerationAttempt, GenerationMetadata } from "../types";

interface DraftMetadataProps {
  feedbackNotes: string;
  feedbackTags: string[];
  generationAttempt?: GenerationAttempt | null;
  generationMetadata: GenerationMetadata;
  isRegenerating: boolean;
  isSavingFeedback: boolean;
  onFeedbackNotesChange: (notes: string) => void;
  onFeedbackTagToggle: (tag: string) => void;
  onRegenerateDraft: (action: string) => void;
  onSaveFeedback: () => void;
  quality: DraftQuality;
}

const feedbackOptions = [
  "Too easy",
  "Too hard",
  "Unclear statement",
  "Weak examples",
  "Needs edge cases",
  "Not original enough",
  "Strong interview feel"
];

const regenerateActions = ["Make harder", "Add edge cases", "Cleaner examples", "Different premise"];

function displayValue(value: string | null | undefined) {
  const normalized = value?.trim() ?? "";
  return normalized.length > 0 ? normalized : "Not recorded";
}

function formatStatus(status: string | null | undefined) {
  const formatted = (status ?? "")
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((part) => `${part.charAt(0).toUpperCase()}${part.slice(1)}`)
    .join(" ");

  return displayValue(formatted);
}

export function DraftMetadata({
  feedbackNotes,
  feedbackTags,
  generationAttempt,
  generationMetadata,
  isRegenerating,
  isSavingFeedback,
  onFeedbackNotesChange,
  onFeedbackTagToggle,
  onRegenerateDraft,
  onSaveFeedback,
  quality
}: DraftMetadataProps) {
  const metrics = [
    { label: "Provider", value: displayValue(generationMetadata.provider) },
    { label: "Model", value: displayValue(generationMetadata.modelId) },
    { label: "Prompt", value: displayValue(generationMetadata.promptVersion) },
    { label: "Attempt", value: generationAttempt?.outcome ?? "Drafted" },
    { label: "Repair", value: quality.repairUsed ? "Used" : "Not used" },
    { label: "Examples", value: String(quality.exampleCount) },
    { label: "Visible", value: String(quality.visibleTestCount) },
    { label: "Hidden", value: String(quality.hiddenTestCount) },
    { label: "Total Tests", value: String(quality.totalTestCount) }
  ];

  return (
    <section className="draft-quality" aria-label="Draft quality">
      <div className="draft-metadata-header">
        <div>
          <span>Draft QA</span>
          <strong>{displayValue(generationMetadata.intendedTechnique)}</strong>
        </div>
        <span className="validation-pill">{formatStatus(quality.status)}</span>
      </div>

      <dl className="draft-metadata-grid">
        {metrics.map((metric) => (
          <div key={metric.label}>
            <dt>{metric.label}</dt>
            <dd>{metric.value}</dd>
          </div>
        ))}
      </dl>

      <ul className="draft-quality-checks" aria-label="Validation checks">
        {quality.checks.map((check) => (
          <li key={check.label}>
            <span>{check.status}</span>
            <div>
              <strong>{check.label}</strong>
              <p>{check.detail}</p>
            </div>
          </li>
        ))}
      </ul>

      <p>{displayValue(quality.summary || generationMetadata.validationSummary)}</p>

      <div className="draft-studio">
        <div className="draft-feedback-options" aria-label="Draft feedback tags">
          {feedbackOptions.map((tag) => {
            const isSelected = feedbackTags.includes(tag);
            return (
              <button
                className={isSelected ? "selected" : ""}
                key={tag}
                onClick={() => onFeedbackTagToggle(tag)}
                type="button"
              >
                {tag}
              </button>
            );
          })}
        </div>
        <textarea
          aria-label="Draft feedback notes"
          onChange={(event) => onFeedbackNotesChange(event.target.value)}
          placeholder="Feedback for the next variant"
          rows={3}
          value={feedbackNotes}
        />
        <div className="draft-studio-actions">
          <button disabled={isSavingFeedback || isRegenerating} onClick={onSaveFeedback} type="button">
            {isSavingFeedback ? "Saving..." : "Save Feedback"}
          </button>
          <div>
            {regenerateActions.map((action) => (
              <button
                disabled={isRegenerating || isSavingFeedback}
                key={action}
                onClick={() => onRegenerateDraft(action)}
                type="button"
              >
                {isRegenerating ? "Regenerating..." : action}
              </button>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
