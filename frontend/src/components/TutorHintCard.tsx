import { useState } from "react";
import type { FormEvent } from "react";
import type { SubmissionResult, TutorHint, TutorMessage } from "../types";

interface TutorHintCardProps {
  hint: TutorHint | null;
  isLoading: boolean;
  isLoadingMessages: boolean;
  isSendingMessage: boolean;
  messages: TutorMessage[];
  onRequest: () => void;
  onSendMessage: (message: string) => Promise<void>;
  status?: SubmissionResult["status"];
  submissionId?: string;
}

export function TutorHintCard({
  hint,
  isLoading,
  isLoadingMessages,
  isSendingMessage,
  messages,
  onRequest,
  onSendMessage,
  status,
  submissionId
}: TutorHintCardProps) {
  const [draft, setDraft] = useState("");

  if (!submissionId || !status || status === "ACCEPTED") {
    return null;
  }

  const label = hint ? `${hint.provider} ${hint.level}` : "nudge";

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = draft.trim();
    if (!message || isSendingMessage) {
      return;
    }

    await onSendMessage(message);
    setDraft("");
  }

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

      {(isLoadingMessages || messages.length > 0) && (
        <div className="tutor-thread">
          {isLoadingMessages ? (
            <p>Loading conversation...</p>
          ) : (
            messages.map((message) => (
              <div className={`tutor-message ${message.role}`} key={message.id}>
                <span>{message.role === "assistant" ? "Tutor" : "You"}</span>
                <p>{message.content}</p>
              </div>
            ))
          )}
        </div>
      )}

      <form className="tutor-follow-up" onSubmit={handleSubmit}>
        <input
          aria-label="Tutor follow-up"
          disabled={isSendingMessage}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="Ask a follow-up"
          type="text"
          value={draft}
        />
        <button disabled={isSendingMessage || !draft.trim()} type="submit">
          {isSendingMessage ? "Sending..." : "Send"}
        </button>
      </form>
    </div>
  );
}
