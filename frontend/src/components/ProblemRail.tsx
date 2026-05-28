import type { Difficulty, ProblemDraft, ProblemSummary } from "../types";
import { difficultyOptions } from "../ui";

interface ProblemRailProps {
  draft: ProblemDraft | null;
  generatorDifficulty: Difficulty;
  generatorTopic: string;
  isGenerating: boolean;
  isPublishing: boolean;
  onDiscardDraft: () => void;
  onGenerate: () => void;
  onGeneratorDifficultyChange: (difficulty: Difficulty) => void;
  onGeneratorTopicChange: (topic: string) => void;
  onPublishDraft: () => void;
  onSelectProblem: (id: string) => void;
  problems: ProblemSummary[];
  selectedId: string;
}

export function ProblemRail({
  draft,
  generatorDifficulty,
  generatorTopic,
  isGenerating,
  isPublishing,
  onDiscardDraft,
  onGenerate,
  onGeneratorDifficultyChange,
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
          <p>Local coding practice</p>
        </div>
      </div>

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
