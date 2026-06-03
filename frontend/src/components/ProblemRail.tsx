import { useMemo, useState } from "react";
import type { Difficulty, ProblemDraft, ProblemSummary } from "../types";
import { difficultyOptions } from "../ui";

interface ProblemRailProps {
  draft: ProblemDraft | null;
  isPublishing: boolean;
  onDiscardDraft: () => void;
  onPublishDraft: () => void;
  onSelectProblem: (id: string) => void;
  problems: ProblemSummary[];
  selectedId: string;
}

export function ProblemRail({
  draft,
  isPublishing,
  onDiscardDraft,
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

      <nav className="rail-menu" aria-label="Workspace">
        <span className="selected">
          Studio
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
        <span>Local workspace</span>
        <small>v0.9.0</small>
      </div>
    </aside>
  );
}
