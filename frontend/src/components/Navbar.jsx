import React from 'react';
import { Bell, Search, Plus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ title, onQuickAction }) {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';

  return (
    <header className="min-h-16 bg-surface border-b border-border px-3 md:px-6 py-3 flex items-center justify-between gap-3 text-ink sticky top-0 z-40">
      <div className="flex items-center gap-4 min-w-0">
        <h2 className="text-base md:text-lg font-display font-bold text-ink truncate">{title}</h2>
      </div>

      <div className="flex flex-row items-center gap-2 md:gap-4 shrink-0">
        <div className="max-w-[38vw] md:max-w-none truncate text-right text-[10px] md:text-[11px] font-mono text-ink-muted">
          <span className="hidden sm:inline">Active Session: </span><span className="text-ink font-semibold">{user?.email}</span>
        </div>

        {onQuickAction && (
          <button
            onClick={onQuickAction}
            className={`flex items-center gap-2 text-xs font-semibold px-4 py-2 rounded-full transition-all text-white shadow-sm ${
              isClient
                ? 'bg-forest hover:bg-forest/90'
                : 'bg-brass hover:bg-brass-dark'
            }`}
          >
            <Plus className="w-4 h-4" />
            New Entry
          </button>
        )}
      </div>
    </header>
  );
}
