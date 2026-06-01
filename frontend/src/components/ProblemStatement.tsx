import { useEffect, useMemo, useState } from "react";
import type { DraftQuality, GenerationAttempt, GenerationMetadata, Problem } from "../types";
import { DraftMetadata } from "./DraftMetadata";

interface ProblemStatementProps {
  draftFeedbackNotes: string;
  draftFeedbackTags: string[];
  generationAttempt?: GenerationAttempt | null;
  generationMetadata?: GenerationMetadata;
  isRegeneratingDraft: boolean;
  quality?: DraftQuality;
  isDraftPreview: boolean;
  isSavingDraftFeedback: boolean;
  onDraftFeedbackNotesChange: (notes: string) => void;
  onDraftFeedbackTagToggle: (tag: string) => void;
  onRegenerateDraft: (action: string) => void;
  onSaveDraftFeedback: () => void;
  problem: Problem;
}

export function ProblemStatement({
  draftFeedbackNotes,
  draftFeedbackTags,
  generationAttempt,
  generationMetadata,
  isDraftPreview,
  isRegeneratingDraft,
  isSavingDraftFeedback,
  onDraftFeedbackNotesChange,
  onDraftFeedbackTagToggle,
  onRegenerateDraft,
  onSaveDraftFeedback,
  problem,
  quality
}: ProblemStatementProps) {
  const [activeSection, setActiveSection] = useState("#problem-overview");
  const hasExamples = problem.examples.length > 0;
  const hasConstraints = problem.constraints.length > 0;
  const hasDraftQuality = isDraftPreview && generationMetadata !== undefined && quality !== undefined;
  const sections = useMemo(
    () =>
      [
        { href: "#problem-overview", label: "Problem", show: true },
        { href: "#problem-examples", label: "Examples", show: hasExamples },
        { href: "#problem-constraints", label: "Constraints", show: hasConstraints },
        { href: "#draft-quality", label: "Draft QA", show: hasDraftQuality }
      ].filter((section) => section.show),
    [hasConstraints, hasDraftQuality, hasExamples]
  );

  useEffect(() => {
    if (!sections.some((section) => section.href === activeSection)) {
      setActiveSection("#problem-overview");
    }
  }, [activeSection, sections]);

  return (
    <section className="statement">
      <nav className="statement-tabs" aria-label="Problem sections">
        {sections.map((section) => (
          <a
            aria-current={activeSection === section.href ? "location" : undefined}
            href={section.href}
            key={section.href}
            onClick={() => setActiveSection(section.href)}
          >
            {section.label}
          </a>
        ))}
      </nav>
      <div className="problem-heading">
        <div id="problem-overview">
          <p className="eyebrow">
            {problem.difficulty}
            {isDraftPreview ? " Draft" : ""}
          </p>
          <h2>{problem.title}</h2>
        </div>
        <div className="tags">
          {problem.tags.map((tag) => (
            <span key={tag}>{tag}</span>
          ))}
        </div>
      </div>

      <p>{problem.description}</p>

      <div className="format-grid">
        <section>
          <h3>Input</h3>
          <p>{problem.inputFormat}</p>
        </section>
        <section>
          <h3>Output</h3>
          <p>{problem.outputFormat}</p>
        </section>
      </div>

      {hasExamples ? (
        <div id="problem-examples" className="examples-stack">
          {problem.examples.map((example, index) => (
            <section className="example" key={`${example.input}-${index}`}>
              <h3>Example {index + 1}</h3>
              <div className="example-grid">
                <div>
                  <span>Input</span>
                  <pre>{example.input}</pre>
                </div>
                <div>
                  <span>Output</span>
                  <pre>{example.output}</pre>
                </div>
              </div>
              <p>{example.explanation}</p>
            </section>
          ))}
        </div>
      ) : null}

      {hasConstraints ? (
        <section id="problem-constraints">
          <h3>Constraints</h3>
          <ul>
            {problem.constraints.map((constraint) => (
              <li key={constraint}>{constraint}</li>
            ))}
          </ul>
        </section>
      ) : null}

      {hasDraftQuality ? (
        <DraftMetadata
          feedbackNotes={draftFeedbackNotes}
          feedbackTags={draftFeedbackTags}
          generationAttempt={generationAttempt}
          generationMetadata={generationMetadata}
          isRegenerating={isRegeneratingDraft}
          isSavingFeedback={isSavingDraftFeedback}
          onFeedbackNotesChange={onDraftFeedbackNotesChange}
          onFeedbackTagToggle={onDraftFeedbackTagToggle}
          onRegenerateDraft={onRegenerateDraft}
          onSaveFeedback={onSaveDraftFeedback}
          quality={quality}
        />
      ) : null}
    </section>
  );
}
