import { useEffect, useRef } from "react";

const AUTO_DISMISS_MS = 7000;

interface ErrorToastProps {
  message: string | null;
  onDismiss: () => void;
}

export function ErrorToast({ message, onDismiss }: ErrorToastProps) {
  // Keep the latest callback in a ref so the dismiss timer is keyed only on
  // the message; parent re-renders must not restart the countdown.
  const onDismissRef = useRef(onDismiss);
  onDismissRef.current = onDismiss;

  useEffect(() => {
    if (!message) {
      return;
    }
    const timeout = window.setTimeout(() => onDismissRef.current(), AUTO_DISMISS_MS);
    return () => window.clearTimeout(timeout);
  }, [message]);

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
