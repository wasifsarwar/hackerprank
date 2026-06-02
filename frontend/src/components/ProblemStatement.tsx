import { useEffect, useMemo, useState } from "react";
import type { DraftQuality, GenerationAttempt, GenerationMetadata, Problem } from "../types";
import { DraftMetadata } from "./DraftMetadata";

type ProblemTab = "problem" | "examples" | "constraints" | "draft-qa";

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
  const [activeTab, setActiveTab] = useState<ProblemTab>("problem");
  const hasExamples = problem.examples.length > 0;
  const hasConstraints = problem.constraints.length > 0;
  const hasDraftQuality = isDraftPreview && generationMetadata !== undefined && quality !== undefined;
  const sections = useMemo(
    () =>
      [
        { id: "problem" as const, label: "Problem", show: true },
        { id: "examples" as const, label: "Examples", show: hasExamples },
        { id: "constraints" as const, label: "Constraints", show: hasConstraints },
        { id: "draft-qa" as const, label: "Draft QA", show: hasDraftQuality }
      ].filter((section) => section.show),
    [hasConstraints, hasDraftQuality, hasExamples]
  );

  useEffect(() => {
    if (!sections.some((section) => section.id === activeTab)) {
      setActiveTab("problem");
    }
  }, [activeTab, sections]);

  useEffect(() => {
    setActiveTab("problem");
  }, [problem.id]);

  return (
    <section className="statement">
      <nav className="statement-tabs" aria-label="Problem sections" role="tablist">
        {sections.map((section) => (
          <button
            aria-selected={activeTab === section.id}
            key={section.id}
            onClick={() => setActiveTab(section.id)}
            role="tab"
            type="button"
          >
            {section.label}
          </button>
        ))}
      </nav>

      {activeTab === "problem" ? (
        <div className="statement-panel" role="tabpanel">
          <div className="problem-heading">
            <div>
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

          <section className="statement-copy">
            <h3>Scenario</h3>
            <p>{problem.scenario || problem.description}</p>
          </section>

          <section className="statement-copy">
            <h3>Task</h3>
            <p>{problem.task || problem.description}</p>
          </section>

          <div className="signature-grid" aria-label="Solution method signatures">
            <section>
              <h3>Java</h3>
              <code>{problem.javaSignature || "Implement the helper method in Main."}</code>
            </section>
            <section>
              <h3>Python</h3>
              <code>{problem.pythonSignature || "Implement the helper function."}</code>
            </section>
          </div>

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
        </div>
      ) : null}

      {activeTab === "examples" && hasExamples ? (
        <div className="statement-panel examples-stack" role="tabpanel">
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

      {activeTab === "constraints" && hasConstraints ? (
        <section className="statement-panel constraints-panel" role="tabpanel">
          <h3>Constraints</h3>
          <ul>
            {problem.constraints.map((constraint) => (
              <li key={constraint}>{constraint}</li>
            ))}
          </ul>
          <div className="constraints-note">
            <h3>Validation Focus</h3>
            <p>
              These limits define the input space used by visible samples and hidden tests. Hidden cases should cover
              boundaries, empty or minimal inputs, duplicate values, and performance-sensitive sizes without revealing
              exact answers.
            </p>
          </div>
        </section>
      ) : null}

      {activeTab === "draft-qa" && hasDraftQuality ? (
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
