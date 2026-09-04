import React from 'react';
import { Bell, Search, Plus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ title, onQuickAction }) {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';

  return (
    <header className="h-16 bg-surface border-b border-border px-6 flex items-center justify-between text-ink sticky top-0 z-40">
      <div className="flex items-center gap-4">
        <h2 className="text-lg font-display font-bold text-ink">{title}</h2>
      </div>

      <div className="flex flex-row items-center gap-4">
        <div className="text-[11px] font-mono text-ink-muted">
          Active Session: <span className="text-ink font-semibold">{user?.email}</span>
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
