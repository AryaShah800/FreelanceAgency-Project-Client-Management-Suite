import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Users, 
  Briefcase, 
  CheckSquare, 
  FileText, 
  Sparkles, 
  LogOut,
  Building2
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar() {
  const { user, logout } = useAuth();
  const isClient = user?.role === 'CLIENT';

  const ownerNavItems = [
    { name: 'Dashboard', path: '/', icon: LayoutDashboard },
    { name: 'Client CRM', path: '/clients', icon: Users },
    { name: 'Projects', path: '/projects', icon: Briefcase },
    { name: 'Task Board', path: '/tasks', icon: CheckSquare },
    { name: 'GST Invoices', path: '/invoices', icon: FileText },
    { name: 'AI Proposal Writer', path: '/ai-generator', icon: Sparkles },
  ];

  const clientNavItems = [
    { name: 'Client Portal', path: '/', icon: LayoutDashboard },
    { name: 'My Projects', path: '/projects', icon: Briefcase },
    { name: 'My Invoices & Payments', path: '/invoices', icon: FileText },
  ];

  const navItems = isClient ? clientNavItems : ownerNavItems;

  return (
    <aside className="w-64 bg-surface border-r border-border min-h-screen flex flex-col justify-between p-5 text-ink">
      <div>
        <div className="flex items-center gap-3 px-1 py-4 border-b border-border mb-6">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-white font-bold text-lg shadow-sm ${
            isClient ? 'bg-forest' : 'bg-brass'
          }`}>
            <Building2 className="w-5 h-5 text-surface" />
          </div>
          <div>
            <h1 className="font-display font-bold text-base leading-tight text-ink">{user?.agencyName || 'Agency Suite'}</h1>
            <span className={`text-[10px] tracking-wider uppercase font-mono px-2 py-0.5 rounded border inline-block mt-1 ${
              isClient ? 'bg-forest/10 text-forest border-forest/20' : 'bg-brass/10 text-brass-dark border-brass/20'
            }`}>
              {isClient ? 'CLIENT PORTAL' : user?.role || 'PRO PLAN'}
            </span>
          </div>
        </div>

        <nav className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `w-full flex items-center gap-3 px-3 py-2.5 text-sm transition-colors border-l-2 font-medium ${
                    isActive
                      ? isClient
                        ? 'border-forest text-forest bg-forest/5'
                        : 'border-brass text-brass-dark bg-brass-soft/20'
                      : 'border-transparent text-ink-muted hover:text-ink hover:bg-ink/5'
                  }`
                }
              >
                <Icon className="w-4 h-4" strokeWidth={1.5} />
                {item.name}
              </NavLink>
            );
          })}
        </nav>
      </div>

      <div className="border-t border-border pt-4 space-y-3">
        <div className="px-3 py-2 rounded bg-paper border border-border flex items-center gap-3">
          <div className={`w-8 h-8 rounded-full font-bold flex items-center justify-center text-xs ${
            isClient ? 'bg-forest/15 text-forest' : 'bg-brass/15 text-brass-dark'
          }`}>
            {user?.name?.[0]?.toUpperCase() || 'U'}
          </div>
          <div className="overflow-hidden text-ellipsis whitespace-nowrap text-xs">
            <div className="font-semibold text-ink">{user?.name}</div>
            <div className="text-ink-muted text-[11px] truncate font-mono">{user?.email}</div>
          </div>
        </div>

        <button
          onClick={logout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded text-sm font-medium text-rust hover:bg-rust-soft/30 transition-colors"
        >
          <LogOut className="w-4 h-4" strokeWidth={1.5} />
          Sign Out
        </button>
      </div>
    </aside>
  );
}
