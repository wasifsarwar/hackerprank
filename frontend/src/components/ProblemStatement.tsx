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
  return (
    <section className="statement">
      <nav className="statement-tabs" aria-label="Problem sections">
        <a href="#problem-overview">Problem</a>
        <a href="#problem-examples">Examples</a>
        <a href="#problem-constraints">Constraints</a>
        <a href="#draft-quality">Draft QA</a>
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

      <section id="problem-constraints">
        <h3>Constraints</h3>
        <ul>
          {problem.constraints.map((constraint) => (
            <li key={constraint}>{constraint}</li>
          ))}
        </ul>
      </section>

      {isDraftPreview && generationMetadata && quality ? (
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
    </section>
  );
}
