import { useMemo, useState } from "react";
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
  const [search, setSearch] = useState("");
  const [difficultyFilter, setDifficultyFilter] = useState<"All" | Difficulty>("All");
  const filteredProblems = useMemo(() => {
    const query = search.trim().toLowerCase();
    return problems.filter((problem) => {
      const matchesQuery =
        query.length === 0 ||
        problem.title.toLowerCase().includes(query) ||
        problem.tags.some((tag) => tag.toLowerCase().includes(query));
      const matchesDifficulty = difficultyFilter === "All" || problem.difficulty === difficultyFilter;
      return matchesQuery && matchesDifficulty;
    });
  }, [difficultyFilter, problems, search]);

  return (
    <aside className="problem-rail" aria-label="Problems">
      <div className="brand">
        <span className="brand-mark">HP</span>
        <div>
          <h1>HackerPrank</h1>
          <p>Interview practice studio</p>
        </div>
      </div>

      <details className="generator-drawer">
        <summary>{isGenerating ? "Generating..." : "Generate Draft"}</summary>
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
      </details>

      <nav className="rail-menu" aria-label="Workspace">
        <span className="selected">
          <span aria-hidden="true">&lt;/&gt;</span>
          Studio
        </span>
        <span>
          <span aria-hidden="true">()</span>
          Problems
        </span>
        <span>
          <span aria-hidden="true">[]</span>
          Drafts
        </span>
        <span>
          <span aria-hidden="true">ok</span>
          Submissions
        </span>
        <span>
          <span aria-hidden="true">{">_"}</span>
          Playground
        </span>
        <span>
          <span aria-hidden="true">%</span>
          Analytics
        </span>
        <span>
          <span aria-hidden="true">*</span>
          Settings
        </span>
      </nav>

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
      <div className="problem-search">
        <input
          aria-label="Search problems"
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search problems..."
          type="search"
          value={search}
        />
      </div>
      <div className="problem-filters" aria-label="Problem difficulty filter">
        {(["All", ...difficultyOptions] as Array<"All" | Difficulty>).map((difficulty) => (
          <button
            className={difficultyFilter === difficulty ? "selected" : ""}
            key={difficulty}
            onClick={() => setDifficultyFilter(difficulty)}
            type="button"
          >
            {difficulty}
          </button>
        ))}
      </div>
      <nav className="problem-list">
        {filteredProblems.map((item) => (
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
        {filteredProblems.length === 0 && <p className="rail-empty">No matching problems.</p>}
      </nav>
      <div className="rail-footer">
        <span>Local Mode</span>
        <small>v0.9.0</small>
      </div>
    </aside>
  );
}
