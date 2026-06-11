import { useRef, useState } from "react";
import { fetchSubmissionHint, fetchTutorMessages, sendTutorMessage } from "../api";
import type { TutorHint, TutorMessage } from "../types";

interface UseTutorOptions {
  isSubmissionVisible: (submissionId: string) => boolean;
  setError: (message: string | null) => void;
}

/**
 * Owns tutor hints and the submission-scoped tutor conversation. Responses
 * are dropped when the submission they belong to is no longer on screen.
 */
export function useTutor({ isSubmissionVisible, setError }: UseTutorOptions) {
  const [hint, setHint] = useState<TutorHint | null>(null);
  const [hintSubmissionId, setHintSubmissionId] = useState("");
  const [messages, setMessages] = useState<TutorMessage[]>([]);
  const [messagesSubmissionId, setMessagesSubmissionId] = useState("");
  const [isLoadingHint, setIsLoadingHint] = useState(false);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [isSendingMessage, setIsSendingMessage] = useState(false);
  const hintRequestRef = useRef(0);
  const chatRequestRef = useRef(0);

  function isCurrentHintRequest(submissionId: string, requestId: number) {
    return hintRequestRef.current === requestId && isSubmissionVisible(submissionId);
  }

  function isCurrentChatRequest(submissionId: string, requestId: number) {
    return chatRequestRef.current === requestId && isSubmissionVisible(submissionId);
  }

  function reset() {
    hintRequestRef.current += 1;
    chatRequestRef.current += 1;
    setHint(null);
    setHintSubmissionId("");
    setIsLoadingHint(false);
    setMessages([]);
    setMessagesSubmissionId("");
    setIsLoadingMessages(false);
    setIsSendingMessage(false);
  }

  async function requestHint(submissionId: string) {
    if (!submissionId) {
      return;
    }

    const requestId = hintRequestRef.current + 1;
    hintRequestRef.current = requestId;
    setHint(null);
    setHintSubmissionId(submissionId);
    setIsLoadingHint(true);
    setError(null);

    try {
      const nextHint = await fetchSubmissionHint(submissionId);
      if (isCurrentHintRequest(submissionId, requestId)) {
        setHint(nextHint);
      }
    } catch (err) {
      if (isCurrentHintRequest(submissionId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentHintRequest(submissionId, requestId)) {
        setIsLoadingHint(false);
      }
    }
  }

  async function loadMessages(submissionId: string) {
    const requestId = chatRequestRef.current + 1;
    chatRequestRef.current = requestId;
    setMessages([]);
    setMessagesSubmissionId(submissionId);
    setIsLoadingMessages(true);

    try {
      const nextMessages = await fetchTutorMessages(submissionId);
      if (isCurrentChatRequest(submissionId, requestId)) {
        setMessages(nextMessages);
      }
    } catch (err) {
      if (isCurrentChatRequest(submissionId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentChatRequest(submissionId, requestId)) {
        setIsLoadingMessages(false);
      }
    }
  }

  async function sendMessage(submissionId: string, message: string) {
    if (!submissionId || !message.trim()) {
      return;
    }

    const requestId = chatRequestRef.current + 1;
    chatRequestRef.current = requestId;
    setMessagesSubmissionId(submissionId);
    setIsSendingMessage(true);
    setError(null);

    try {
      const response = await sendTutorMessage(submissionId, message);
      if (isCurrentChatRequest(submissionId, requestId)) {
        setMessages(response.messages);
      }
    } catch (err) {
      if (isCurrentChatRequest(submissionId, requestId)) {
        setError(err instanceof Error ? err.message : "Something went wrong");
      }
    } finally {
      if (isCurrentChatRequest(submissionId, requestId)) {
        setIsSendingMessage(false);
      }
    }
  }

  return {
    hint,
    hintSubmissionId,
    messages,
    messagesSubmissionId,
    isLoadingHint,
    isLoadingMessages,
    isSendingMessage,
    reset,
    requestHint,
    loadMessages,
    sendMessage
  };
}
