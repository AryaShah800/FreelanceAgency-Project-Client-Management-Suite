import React from 'react';
import { Bell, Search, Plus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ title, onQuickAction }) {
  const { user } = useAuth();

  return (
    <header className="h-16 glass-panel border-b border-dark-border px-6 flex items-center justify-between text-dark-text sticky top-0 z-40">
      <div className="flex items-center gap-4">
        <h2 className="text-xl font-bold tracking-tight text-white">{title}</h2>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative">
          <Search className="w-4 h-4 text-dark-muted absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search clients, projects, tasks..."
            className="w-64 bg-dark-bg/60 border border-dark-border/60 text-xs text-white rounded-xl pl-9 pr-4 py-2 focus:outline-none focus:border-brand-500 transition-colors"
          />
        </div>

        <button className="p-2 rounded-xl bg-dark-card/60 border border-dark-border/60 text-dark-muted hover:text-white hover:bg-dark-card transition-colors relative">
          <Bell className="w-4 h-4" />
          <span className="w-2 h-2 rounded-full bg-brand-500 absolute top-2 right-2 ring-2 ring-dark-bg"></span>
        </button>

        {onQuickAction && (
          <button
            onClick={onQuickAction}
            className="flex items-center gap-2 bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white text-xs font-semibold px-4 py-2 rounded-xl shadow-lg shadow-brand-500/20 transition-all"
          >
            <Plus className="w-4 h-4" />
            New Entry
          </button>
        )}
      </div>
    </header>
  );
}
