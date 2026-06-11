import { useMemo, useState } from "react";
import { useTheme } from "../hooks/useTheme";
import type { Difficulty, ProblemDraft, ProblemSummary } from "../types";
import { difficultyOptions } from "../ui";

interface ProblemRailProps {
  draft: ProblemDraft | null;
  isPublishing: boolean;
  onDeleteProblem: (id: string) => void;
  onDiscardDraft: () => void;
  onPublishDraft: () => void;
  onSelectProblem: (id: string) => void;
  problems: ProblemSummary[];
  selectedId: string;
  solvedIds: Set<string>;
}

export function ProblemRail({
  draft,
  isPublishing,
  onDeleteProblem,
  onDiscardDraft,
  onPublishDraft,
  onSelectProblem,
  problems,
  selectedId,
  solvedIds
}: ProblemRailProps) {
  const [search, setSearch] = useState("");
  const [difficultyFilter, setDifficultyFilter] = useState<"All" | Difficulty>("All");
  const [hideSolved, setHideSolved] = useState(false);
  const { theme, toggleTheme } = useTheme();
  const filteredProblems = useMemo(() => {
    const query = search.trim().toLowerCase();
    return problems.filter((problem) => {
      const matchesQuery =
        query.length === 0 ||
        problem.title.toLowerCase().includes(query) ||
        problem.tags.some((tag) => tag.toLowerCase().includes(query));
      const matchesDifficulty = difficultyFilter === "All" || problem.difficulty === difficultyFilter;
      const matchesSolved = !hideSolved || !solvedIds.has(problem.id);
      return matchesQuery && matchesDifficulty && matchesSolved;
    });
  }, [difficultyFilter, hideSolved, problems, search, solvedIds]);

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
        <small>{filteredProblems.length}</small>
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
        <button
          className={hideSolved ? "selected" : ""}
          onClick={() => setHideSolved((current) => !current)}
          title="Hide problems you have already solved"
          type="button"
        >
          Hide solved
        </button>
      </div>
      <nav className="problem-list">
        {filteredProblems.map((item) => (
          <div className={item.id === selectedId ? "problem-item active" : "problem-item"} key={item.id}>
            <button className="problem-item-main" onClick={() => onSelectProblem(item.id)} type="button">
              {solvedIds.has(item.id) ? (
                <span aria-label="Solved" className="solved-check" title="Solved">
                  ✓
                </span>
              ) : null}
              <span className="problem-item-title">{item.title}</span>
              <small className={`diff-badge ${item.difficulty.toLowerCase()}`}>{item.difficulty}</small>
            </button>
            <button
              aria-label={`Delete ${item.title}`}
              className="problem-delete"
              onClick={() => onDeleteProblem(item.id)}
              title="Delete problem"
              type="button"
            >
              ✕
            </button>
          </div>
        ))}
        {filteredProblems.length === 0 && <p className="rail-empty">No matching problems.</p>}
      </nav>
      <div className="rail-footer">
        <span>Local workspace</span>
        <button
          aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
          className="theme-toggle"
          onClick={toggleTheme}
          title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
          type="button"
        >
          {theme === "dark" ? "☀" : "☾"}
        </button>
      </div>
    </aside>
  );
}
