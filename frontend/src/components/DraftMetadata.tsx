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
  "Company flavor missing",
  "Too template-like",
  "Strong interview feel"
];

const regenerateActions = ["More realistic", "Add harder hidden cases", "Improve statement", "Change company flavor"];

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

function formatNumber(value: number | null | undefined) {
  return typeof value === "number" ? new Intl.NumberFormat().format(value) : "Not recorded";
}

function formatProvider(provider: string | null | undefined) {
  const normalized = provider?.trim().toLowerCase() ?? "";

  if (normalized === "openai") {
    return "OpenAI";
  }

  if (normalized === "anthropic") {
    return "Claude";
  }

  if (normalized === "deterministic") {
    return "Local template";
  }

  return displayValue(provider);
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "Not recorded";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not recorded";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function abbreviateHash(value: string | null | undefined) {
  const normalized = value?.trim() ?? "";
  return normalized.length > 8 ? normalized.slice(0, 8) : displayValue(normalized);
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
  const provider = generationMetadata.provider?.toLowerCase() ?? "";
  const isDeterministic = provider === "deterministic";
  const repairUsed = quality.repairUsed;
  const reliabilityTone = isDeterministic ? "warning" : repairUsed ? "attention" : "success";
  const reliabilityTitle = isDeterministic
    ? "Local template fallback"
    : repairUsed
      ? "AI draft repaired"
      : "AI draft validated";
  const reliabilityCopy = isDeterministic
    ? "The provider was unavailable or did not produce a valid draft, so HackerPrank used a vetted built-in template. Good for practice, less original than a live model draft."
    : repairUsed
      ? "The first model response failed the draft contract. The repair loop produced this validated version before it reached you."
      : "The selected provider produced a draft that passed schema, reference-solution, example, and hidden-test validation on the first accepted pass.";
  const estimatedTokens =
    (generationAttempt?.estimatedPromptTokens ?? 0) + (generationAttempt?.estimatedResponseTokens ?? 0);
  const metrics = [
    { label: "Provider", value: formatProvider(generationMetadata.provider) },
    { label: "Model", value: displayValue(generationMetadata.modelId) },
    { label: "Prompt Version", value: displayValue(generationMetadata.promptVersion) },
    { label: "Outcome", value: formatStatus(generationAttempt?.outcome ?? "DRAFTED") },
    { label: "Prompt Tokens", value: formatNumber(generationAttempt?.estimatedPromptTokens) },
    { label: "Response Tokens", value: formatNumber(generationAttempt?.estimatedResponseTokens) },
    { label: "Total Tokens", value: estimatedTokens > 0 ? formatNumber(estimatedTokens) : "Not recorded" },
    { label: "Prompt Chars", value: formatNumber(generationAttempt?.promptCharCount) },
    { label: "Response Chars", value: formatNumber(generationAttempt?.responseCharCount) },
    { label: "Examples", value: formatNumber(quality.exampleCount) },
    { label: "Visible Tests", value: formatNumber(quality.visibleTestCount) },
    { label: "Hidden Tests", value: formatNumber(quality.hiddenTestCount) },
    { label: "Total Tests", value: formatNumber(quality.totalTestCount) }
  ];
  const timeline = [
    { label: "Draft requested", value: formatDateTime(generationAttempt?.createdAt) },
    { label: "Provider path", value: formatProvider(generationMetadata.provider) },
    { label: "Repair loop", value: repairUsed ? "Used" : "Not used" },
    { label: "Validation", value: formatStatus(generationMetadata.validationStatus || quality.status) },
    { label: "Last updated", value: formatDateTime(generationAttempt?.updatedAt) }
  ];

  return (
    <section className="draft-quality" id="draft-quality" aria-label="Draft quality">
      <div className="draft-metadata-header">
        <div>
          <span>Draft QA</span>
          <strong>{displayValue(generationMetadata.intendedTechnique)}</strong>
        </div>
        <div className="draft-quality-actions">
          <span className="quality-pill">Quality: {formatStatus(quality.status)}</span>
          <span className="validation-pill">{formatStatus(quality.status)}</span>
        </div>
      </div>

      <div className={`generation-reliability ${reliabilityTone}`}>
        <div>
          <span>Generation Path</span>
          <strong>{reliabilityTitle}</strong>
        </div>
        <p>{reliabilityCopy}</p>
      </div>

      <dl className="draft-metadata-grid">
        {metrics.map((metric) => (
          <div key={metric.label}>
            <dt>{metric.label}</dt>
            <dd>{metric.value}</dd>
          </div>
        ))}
      </dl>

      <div className="attempt-card">
        <div className="attempt-card-header">
          <div>
            <span>Attempt Timeline</span>
            <strong>{generationAttempt?.id ? `Attempt ${generationAttempt.id.slice(0, 8)}` : "Current draft"}</strong>
          </div>
          <div className="trace-row" aria-label="Generation trace identifiers">
            <span>Prompt {abbreviateHash(generationAttempt?.promptHash)}</span>
            <span>Response {abbreviateHash(generationAttempt?.responseHash)}</span>
          </div>
        </div>
        <ol className="attempt-timeline">
          {timeline.map((item) => (
            <li key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </li>
          ))}
        </ol>
      </div>

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
