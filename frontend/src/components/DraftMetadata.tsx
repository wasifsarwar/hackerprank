import type { DraftQuality, GenerationMetadata } from "../types";

interface DraftMetadataProps {
  generationMetadata: GenerationMetadata;
  quality: DraftQuality;
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

export function DraftMetadata({ generationMetadata, quality }: DraftMetadataProps) {
  const metrics = [
    { label: "Provider", value: displayValue(generationMetadata.provider) },
    { label: "Model", value: displayValue(generationMetadata.modelId) },
    { label: "Prompt", value: displayValue(generationMetadata.promptVersion) },
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
    </section>
  );
}
