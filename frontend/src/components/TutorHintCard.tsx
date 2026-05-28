import type { SubmissionResult, TutorHint } from "../types";

interface TutorHintCardProps {
  hint: TutorHint | null;
  isLoading: boolean;
  onRequest: () => void;
  status?: SubmissionResult["status"];
  submissionId?: string;
}

export function TutorHintCard({ hint, isLoading, onRequest, status, submissionId }: TutorHintCardProps) {
  if (!submissionId || !status || status === "ACCEPTED") {
    return null;
  }

  const label = hint ? `${hint.provider} ${hint.level}` : "nudge";

  return (
    <div className="tutor-hint">
      <div className="tutor-hint-header">
        <div>
          <span>{label}</span>
          <strong>Tutor Hint</strong>
        </div>
        <button disabled={isLoading} onClick={onRequest} type="button">
          {isLoading ? "Loading..." : hint ? "Another Hint" : "Hint"}
        </button>
      </div>

      {isLoading ? (
        <p>Thinking through the failing run...</p>
      ) : hint ? (
        <>
          <p>{hint.summary}</p>
          <ul>
            {hint.hints.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          <p className="tutor-next-step">{hint.nextStep}</p>
        </>
      ) : (
        <p>Ask for one focused nudge on this failed submission.</p>
      )}
    </div>
  );
}
