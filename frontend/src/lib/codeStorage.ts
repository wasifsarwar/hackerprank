import type { Language } from "../types";

function codeStorageKey(problemId: string, language: Language) {
  return `hackerprank-code:${problemId}:${language}`;
}

export function loadStoredCode(problemId: string, language: Language): string | null {
  try {
    return window.localStorage.getItem(codeStorageKey(problemId, language));
  } catch {
    return null;
  }
}

export function storeCode(problemId: string, language: Language, code: string) {
  try {
    window.localStorage.setItem(codeStorageKey(problemId, language), code);
  } catch {
    // Storage may be full or unavailable; autosave is best-effort.
  }
}

export function clearStoredCode(problemId: string, language: Language) {
  try {
    window.localStorage.removeItem(codeStorageKey(problemId, language));
  } catch {
    // Ignore storage failures.
  }
}
