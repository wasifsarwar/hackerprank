import { useRef, useState } from "react";
import { fetchSubmissionDetail, fetchSubmissionHistory, runSubmission } from "../api";
import type { Language, Problem, SubmissionDetail, SubmissionResult, SubmissionSummary } from "../types";
import type { ResultView } from "../ui";

interface UseSubmissionsOptions {
  isCurrentPublishedProblem: (problemId: string) => boolean;
  setError: (message: string | null) => void;
}

/**
 * Owns the run/submit lifecycle, the submission history list, and the
 * selected historical submission. Every async path is guarded against stale
 * responses with request counters so out-of-order completions are dropped.
 */
export function useSubmissions({ isCurrentPublishedProblem, setError }: UseSubmissionsOptions) {
  const [result, setResultState] = useState<SubmissionResult | null>(null);
  const [resultView, setResultView] = useState<ResultView>("current");
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
  const [selectedSubmissionId, setSelectedSubmissionId] = useState("");
  const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetail | null>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isLoadingSubmission, setIsLoadingSubmission] = useState(false);
  const runRequestRef = useRef(0);
  const historyRequestRef = useRef(0);
  const selectedSubmissionRequestRef = useRef("");
  const selectedSubmissionIdRef = useRef("");
  const resultSubmissionIdRef = useRef("");

  function setResult(next: SubmissionResult | null) {
    resultSubmissionIdRef.current = next?.submissionId ?? "";
    setResultState(next);
  }

  function isSubmissionVisible(submissionId: string) {
    return resultSubmissionIdRef.current === submissionId || selectedSubmissionIdRef.current === submissionId;
  }

  function isCurrentHistoryRequest(problemId: string, requestId: number) {
    return historyRequestRef.current === requestId && isCurrentPublishedProblem(problemId);
  }

  function clearSelectedSubmission() {
    selectedSubmissionIdRef.current = "";
    selectedSubmissionRequestRef.current = "";
    setSelectedSubmission(null);
    setSelectedSubmissionId("");
    setIsLoadingSubmission(false);
  }

  function resetHistory() {
    historyRequestRef.current += 1;
    setIsLoadingHistory(false);
    setSubmissions([]);
    clearSelectedSubmission();
  }

  function cancelRun() {
    runRequestRef.current += 1;
    setIsRunning(false);
  }

  async function refreshHistory(problemId: string) {
    if (!isCurrentPublishedProblem(problemId)) {
      return;
    }

    const requestId = historyRequestRef.current + 1;
    historyRequestRef.current = requestId;
    setIsLoadingHistory(true);
    try {
      const items = await fetchSubmissionHistory(problemId);
      if (isCurrentHistoryRequest(problemId, requestId)) {
        setSubmissions(items);
      }
    } catch (err) {
      if (isCurrentHistoryRequest(problemId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentHistoryRequest(problemId, requestId)) {
        setIsLoadingHistory(false);
      }
    }
  }

  async function run(
    problem: Problem,
    language: Language,
    code: string,
    runHiddenTests: boolean
  ): Promise<SubmissionResult | null> {
    const runProblemId = problem.id;
    const runRequestId = runRequestRef.current + 1;
    runRequestRef.current = runRequestId;
    setIsRunning(true);
    setError(null);
    setResult(null);

    try {
      const nextResult = await runSubmission({
        problemId: runProblemId,
        language,
        code,
        runHiddenTests
      });
      if (runRequestRef.current === runRequestId && isCurrentPublishedProblem(runProblemId)) {
        setResult(nextResult);
        setResultView("current");
      }
      await refreshHistory(runProblemId);
      return nextResult;
    } catch (err) {
      if (runRequestRef.current === runRequestId && isCurrentPublishedProblem(runProblemId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
      return null;
    } finally {
      if (runRequestRef.current === runRequestId) {
        setIsRunning(false);
      }
    }
  }

  async function selectSubmission(summary: SubmissionSummary): Promise<SubmissionDetail | null> {
    selectedSubmissionRequestRef.current = summary.id;
    selectedSubmissionIdRef.current = summary.id;
    setResultView("history");
    setSelectedSubmissionId(summary.id);
    setSelectedSubmission(null);
    setIsLoadingSubmission(true);
    setError(null);

    try {
      const detail = await fetchSubmissionDetail(summary.id);
      if (selectedSubmissionRequestRef.current === summary.id) {
        setSelectedSubmission(detail);
        return detail;
      }
      return null;
    } catch (err) {
      if (selectedSubmissionRequestRef.current === summary.id) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
      return null;
    } finally {
      if (selectedSubmissionRequestRef.current === summary.id) {
        setIsLoadingSubmission(false);
      }
    }
  }

  function clearResult() {
    setResult(null);
  }

  return {
    result,
    resultView,
    setResultView,
    submissions,
    selectedSubmission,
    selectedSubmissionId,
    isRunning,
    isLoadingHistory,
    isLoadingSubmission,
    isSubmissionVisible,
    refreshHistory,
    resetHistory,
    cancelRun,
    clearResult,
    run,
    selectSubmission
  };
}
