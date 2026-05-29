import type { Difficulty, InterviewStyle, ProblemDraft, ProblemSummary } from "../types";
import { difficultyOptions, interviewStyleOptions } from "../ui";

interface ProblemRailProps {
  draft: ProblemDraft | null;
  generatorConstraintsNotes: string;
  generatorDifficulty: Difficulty;
  generatorInterviewStyle: InterviewStyle;
  generatorTargetConcepts: string;
  generatorTopic: string;
  isGenerating: boolean;
  isPublishing: boolean;
  onDiscardDraft: () => void;
  onGenerate: () => void;
  onGeneratorConstraintsNotesChange: (constraintsNotes: string) => void;
  onGeneratorDifficultyChange: (difficulty: Difficulty) => void;
  onGeneratorInterviewStyleChange: (interviewStyle: InterviewStyle) => void;
  onGeneratorTargetConceptsChange: (targetConcepts: string) => void;
  onGeneratorTopicChange: (topic: string) => void;
  onPublishDraft: () => void;
  onSelectProblem: (id: string) => void;
  problems: ProblemSummary[];
  selectedId: string;
}

export function ProblemRail({
  draft,
  generatorConstraintsNotes,
  generatorDifficulty,
  generatorInterviewStyle,
  generatorTargetConcepts,
  generatorTopic,
  isGenerating,
  isPublishing,
  onDiscardDraft,
  onGenerate,
  onGeneratorConstraintsNotesChange,
  onGeneratorDifficultyChange,
  onGeneratorInterviewStyleChange,
  onGeneratorTargetConceptsChange,
  onGeneratorTopicChange,
  onPublishDraft,
  onSelectProblem,
  problems,
  selectedId
}: ProblemRailProps) {
  return (
    <aside className="problem-rail" aria-label="Problems">
      <div className="brand">
        <span className="brand-mark">HP</span>
        <div>
          <h1>HackerPrank</h1>
          <p>Interview practice studio</p>
        </div>
      </div>

      <nav className="rail-menu" aria-label="Workspace">
        <span className="selected">Studio</span>
        <span>Problems</span>
        <span>Drafts</span>
        <span>Submissions</span>
      </nav>

      <form
        className="generator-panel"
        onSubmit={(event) => {
          event.preventDefault();
          onGenerate();
        }}
      >
        <label>
          <span>Topic</span>
          <input
            onChange={(event) => onGeneratorTopicChange(event.target.value)}
            placeholder="arrays, stacks, strings"
            value={generatorTopic}
          />
        </label>
        <label>
          <span>Difficulty</span>
          <select
            onChange={(event) => onGeneratorDifficultyChange(event.target.value as Difficulty)}
            value={generatorDifficulty}
          >
            {difficultyOptions.map((difficulty) => (
              <option key={difficulty} value={difficulty}>
                {difficulty}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Concepts</span>
          <input
            onChange={(event) => onGeneratorTargetConceptsChange(event.target.value)}
            placeholder="two pointers, prefix sums"
            value={generatorTargetConcepts}
          />
        </label>
        <label>
          <span>Style</span>
          <select
            onChange={(event) => onGeneratorInterviewStyleChange(event.target.value as InterviewStyle)}
            value={generatorInterviewStyle}
          >
            {interviewStyleOptions.map((style) => (
              <option key={style} value={style}>
                {style}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Notes</span>
          <textarea
            onChange={(event) => onGeneratorConstraintsNotesChange(event.target.value)}
            placeholder="must include edge cases"
            rows={3}
            value={generatorConstraintsNotes}
          />
        </label>
        <button className="generate-action" disabled={isGenerating} type="submit">
          {isGenerating ? "Generating..." : "Generate Draft"}
        </button>
      </form>

      {draft && (
        <section className="draft-card">
          <div>
            <span>Draft</span>
            <strong>{draft.problem.title}</strong>
            <small>{draft.validationStatus}</small>
          </div>
          <div className="draft-actions">
            <button className="publish-action" disabled={isPublishing} onClick={onPublishDraft} type="button">
              {isPublishing ? "Publishing..." : "Publish"}
            </button>
            <button className="discard-action" disabled={isPublishing} onClick={onDiscardDraft} type="button">
              Discard
            </button>
          </div>
        </section>
      )}

      <div className="problem-list-header">
        <span>Problem List</span>
      </div>
      <nav className="problem-list">
        {problems.map((item) => (
          <button
            className={item.id === selectedId ? "problem-item active" : "problem-item"}
            key={item.id}
            onClick={() => onSelectProblem(item.id)}
            type="button"
          >
            <span>{item.title}</span>
            <small>{item.difficulty}</small>
          </button>
        ))}
      </nav>
    </aside>
  );
}
