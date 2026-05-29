import type { Difficulty, InterviewStyle } from "../types";
import { difficultyOptions, interviewStyleOptions } from "../ui";

interface GenerationComposerProps {
  constraintsNotes: string;
  difficulty: Difficulty;
  interviewStyle: InterviewStyle;
  isGenerating: boolean;
  onConstraintsNotesChange: (constraintsNotes: string) => void;
  onDifficultyChange: (difficulty: Difficulty) => void;
  onGenerate: () => void;
  onInterviewStyleChange: (interviewStyle: InterviewStyle) => void;
  onTargetConceptsChange: (targetConcepts: string) => void;
  onTopicChange: (topic: string) => void;
  targetConcepts: string;
  topic: string;
}

export function GenerationComposer({
  constraintsNotes,
  difficulty,
  interviewStyle,
  isGenerating,
  onConstraintsNotesChange,
  onDifficultyChange,
  onGenerate,
  onInterviewStyleChange,
  onTargetConceptsChange,
  onTopicChange,
  targetConcepts,
  topic
}: GenerationComposerProps) {
  return (
    <form
      className="generation-composer"
      onSubmit={(event) => {
        event.preventDefault();
        onGenerate();
      }}
    >
      <label className="composer-prompt">
        <span>Generate Prompt</span>
        <input
          onChange={(event) => onTopicChange(event.target.value)}
          placeholder="Generate a sliding window interview problem about longest valid telemetry windows..."
          value={topic}
        />
      </label>

      <div className="composer-controls">
        <label>
          <span>Difficulty</span>
          <select onChange={(event) => onDifficultyChange(event.target.value as Difficulty)} value={difficulty}>
            {difficultyOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Concepts</span>
          <input
            onChange={(event) => onTargetConceptsChange(event.target.value)}
            placeholder="sliding window, hash map"
            value={targetConcepts}
          />
        </label>
        <label>
          <span>Style</span>
          <select onChange={(event) => onInterviewStyleChange(event.target.value as InterviewStyle)} value={interviewStyle}>
            {interviewStyleOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
      </div>

      <label className="composer-notes">
        <span>Optional Notes</span>
        <textarea
          onChange={(event) => onConstraintsNotesChange(event.target.value)}
          placeholder="Company style, edge cases, input constraints, or what should make the problem tricky."
          rows={2}
          value={constraintsNotes}
        />
      </label>

      <button className="composer-submit" disabled={isGenerating} type="submit">
        {isGenerating ? "Generating Draft..." : "Generate Draft"}
      </button>
    </form>
  );
}
