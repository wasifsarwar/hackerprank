import type { GenerationMetadata } from "../types";

interface DraftMetadataProps {
  generationMetadata: GenerationMetadata;
}

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

export function DraftMetadata({ generationMetadata }: DraftMetadataProps) {
  return (
    <section className="draft-metadata" aria-label="Draft generation metadata">
      <div className="draft-metadata-header">
        <div>
          <span>Technique</span>
          <strong>{displayValue(generationMetadata.intendedTechnique)}</strong>
        </div>
        <span className="validation-pill">{formatStatus(generationMetadata.validationStatus)}</span>
      </div>

      <dl className="draft-metadata-grid">
        <div>
          <dt>Provider</dt>
          <dd>{displayValue(generationMetadata.provider)}</dd>
        </div>
        <div>
          <dt>Model</dt>
          <dd>{displayValue(generationMetadata.modelId)}</dd>
        </div>
        <div>
          <dt>Prompt Version</dt>
          <dd>{displayValue(generationMetadata.promptVersion)}</dd>
        </div>
      </dl>

      <p>{displayValue(generationMetadata.validationSummary)}</p>
    </section>
  );
}
