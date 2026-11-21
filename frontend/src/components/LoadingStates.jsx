import React, { useEffect, useState } from 'react';
import { RefreshCw } from 'lucide-react';

export const SLOW_LOAD_MS = 5000;

/** True once `loading` has stayed true for `delay` ms (e.g. free-tier API cold start). */
export function useSlowLoading(loading, delay = SLOW_LOAD_MS) {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    if (!loading) {
      setSlow(false);
      return undefined;
    }
    const id = setTimeout(() => setSlow(true), delay);
    return () => clearTimeout(id);
  }, [loading, delay]);
  return slow;
}

export function Skeleton({ className = '' }) {
  return <div aria-hidden="true" className={`animate-pulse rounded bg-border/60 ${className}`} />;
}

export function WakingNote({ show }) {
  if (!show) return null;
  return (
    <div role="status" className="rounded border border-brass/30 bg-brass-soft/40 px-4 py-3 text-xs text-ink-muted">
      Still loading. The free-tier API may be waking up, which can take up to a minute. Your data will appear as soon as it responds.
    </div>
  );
}

export function LoadError({ message = 'Could not load this data.', onRetry }) {
  return (
    <div role="alert" className="flex items-center justify-between gap-3 rounded border border-rust/20 bg-rust/5 px-4 py-3 text-sm text-rust">
      <span>{message}</span>
      {onRetry && (
        <button onClick={onRetry} className="inline-flex items-center gap-1 font-semibold">
          <RefreshCw className="h-4 w-4" /> Retry
        </button>
      )}
    </div>
  );
}

export function CardListSkeleton({ rows = 3, label = 'Loading' }) {
  return (
    <div role="status" aria-label={label} className="space-y-3">
      {Array.from({ length: rows }, (_, i) => (
        <Skeleton key={i} className="h-16 w-full" />
      ))}
    </div>
  );
}
