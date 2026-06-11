import { useEffect, useState } from "react";
import { clearStoredCode, storeCode } from "../lib/codeStorage";
import type { Language, Problem } from "../types";

const AUTOSAVE_DELAY_MS = 600;

/**
 * Debounced persistence of editor code to localStorage, keyed by problem and
 * language. Saving is skipped while `enabled` is false (e.g. draft previews),
 * and the stored entry is removed when the code matches the starter template.
 */
export function useAutosave(activeProblem: Problem | null, language: Language, code: string, enabled: boolean) {
  const [autosavedAt, setAutosavedAt] = useState<Date | null>(null);

  useEffect(() => {
    if (!activeProblem || !enabled) {
      return;
    }

    const problemId = activeProblem.id;
    const starter = activeProblem.starterCode[language];
    const timeout = window.setTimeout(() => {
      if (code === starter) {
        clearStoredCode(problemId, language);
        setAutosavedAt(null);
      } else {
        storeCode(problemId, language, code);
        setAutosavedAt(new Date());
      }
    }, AUTOSAVE_DELAY_MS);

    return () => window.clearTimeout(timeout);
  }, [code, activeProblem?.id, language, enabled]);

  function resetAutosavedAt() {
    setAutosavedAt(null);
  }

  return { autosavedAt, resetAutosavedAt };
}
