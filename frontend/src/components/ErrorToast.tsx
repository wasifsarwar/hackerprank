import { useEffect } from "react";

const AUTO_DISMISS_MS = 7000;

interface ErrorToastProps {
  message: string | null;
  onDismiss: () => void;
}

export function ErrorToast({ message, onDismiss }: ErrorToastProps) {
  useEffect(() => {
    if (!message) {
      return;
    }
    const timeout = window.setTimeout(onDismiss, AUTO_DISMISS_MS);
    return () => window.clearTimeout(timeout);
  }, [message, onDismiss]);

  if (!message) {
    return null;
  }

  return (
    <div className="alert" role="alert">
      {message}
      <button aria-label="Dismiss error" onClick={onDismiss} type="button">
        ✕
      </button>
    </div>
  );
}
