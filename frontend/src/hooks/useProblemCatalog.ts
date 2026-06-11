import { useEffect, useRef, useState } from "react";
import { deleteProblem as deleteProblemRequest, fetchProblem, fetchProblems, fetchSolvedProblemIds } from "../api";
import type { Problem, ProblemSummary } from "../types";

interface UseProblemCatalogOptions {
  setError: (message: string | null) => void;
}

/**
 * Owns the published problem list, the currently selected problem, and the
 * solved-problem set. Stale responses are discarded via a request counter and
 * a ref mirroring the latest selection.
 */
export function useProblemCatalog({ setError }: UseProblemCatalogOptions) {
  const [problems, setProblems] = useState<ProblemSummary[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [solvedIds, setSolvedIds] = useState<Set<string>>(new Set());
  const selectedIdRef = useRef("");
  const problemRequestRef = useRef(0);

  useEffect(() => {
    fetchProblems()
      .then((items) => {
        selectedIdRef.current = items[0]?.id ?? "";
        setProblems(items);
        setSelectedId(items[0]?.id ?? "");
      })
      .catch((err: Error) => setError(err.message));
    fetchSolvedProblemIds()
      .then((ids) => setSolvedIds(new Set(ids)))
      .catch(() => {});
  }, []);

  function select(id: string) {
    selectedIdRef.current = id;
    problemRequestRef.current += 1;
    setSelectedId(id);
  }

  async function loadSelected(id: string) {
    const requestId = problemRequestRef.current + 1;
    problemRequestRef.current = requestId;
    setProblem(null);

    try {
      const loaded = await fetchProblem(id);
      if (problemRequestRef.current === requestId && selectedIdRef.current === id) {
        setProblem(loaded);
      }
    } catch (err) {
      if (problemRequestRef.current === requestId && selectedIdRef.current === id) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    }
  }

  function markSolved(problemId: string) {
    setSolvedIds((current) => {
      if (current.has(problemId)) {
        return current;
      }
      const next = new Set(current);
      next.add(problemId);
      return next;
    });
  }

  async function deleteProblem(id: string) {
    await deleteProblemRequest(id);
    const remaining = problems.filter((item) => item.id !== id);
    setProblems(remaining);
    return remaining;
  }

  function upsertSummary(summary: ProblemSummary) {
    setProblems((items) => [...items.filter((item) => item.id !== summary.id), summary]);
  }

  function clearSelection() {
    selectedIdRef.current = "";
    setSelectedId("");
    setProblem(null);
  }

  return {
    problems,
    selectedId,
    selectedIdRef,
    problem,
    setProblem,
    solvedIds,
    select,
    loadSelected,
    markSolved,
    deleteProblem,
    upsertSummary,
    clearSelection
  };
}
