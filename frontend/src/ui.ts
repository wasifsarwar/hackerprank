import type { Difficulty, Language } from "./types";

export type ResultView = "current" | "history";

export const languageLabels: Record<Language, string> = {
  python: "Python",
  java: "Java"
};

export const editorLanguages: Record<Language, string> = {
  python: "python",
  java: "java"
};

export const difficultyOptions: Difficulty[] = ["Easy", "Medium", "Hard"];
