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
  Building2,
  ShieldAlert
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
    <aside className="w-64 glass-panel border-r border-dark-border min-h-screen flex flex-col justify-between p-4 text-dark-text">
      <div>
        <div className="flex items-center gap-3 px-3 py-4 border-b border-dark-border/60 mb-6">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-white font-bold text-lg shadow-lg ${
            isClient ? 'bg-gradient-to-tr from-emerald-600 to-teal-400 shadow-emerald-500/30' : 'bg-gradient-to-tr from-brand-600 to-indigo-400 shadow-brand-500/30'
          }`}>
            <Building2 className="w-5 h-5" />
          </div>
          <div>
            <h1 className="font-bold text-sm leading-tight text-white">{user?.agencyName || 'Agency Suite'}</h1>
            <span className={`text-[10px] uppercase font-semibold tracking-wider px-2 py-0.5 rounded-full border ${
              isClient ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-brand-500/10 text-brand-500 border-brand-500/20'
            }`}>
              {user?.role === 'CLIENT' ? 'CLIENT PORTAL' : user?.role || 'PRO PLAN'}
            </span>
          </div>
        </div>

        <nav className="space-y-1.5">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                    isActive
                      ? isClient 
                        ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-md shadow-emerald-600/30' 
                        : 'bg-gradient-to-r from-brand-600 to-brand-700 text-white shadow-md shadow-brand-600/30'
                      : 'text-dark-muted hover:text-white hover:bg-dark-card/60'
                  }`
                }
              >
                <Icon className="w-4 h-4" />
                {item.name}
              </NavLink>
            );
          })}
        </nav>
      </div>

      <div className="border-t border-dark-border/60 pt-4 space-y-3">
        <div className="px-3.5 py-2 rounded-xl bg-dark-card/40 border border-dark-border/40 flex items-center gap-3">
          <div className={`w-8 h-8 rounded-full font-bold flex items-center justify-center text-xs ${
            isClient ? 'bg-emerald-500/20 text-emerald-400' : 'bg-brand-500/20 text-brand-500'
          }`}>
            {user?.name?.[0]?.toUpperCase() || 'U'}
          </div>
          <div className="overflow-hidden text-ellipsis whitespace-nowrap text-xs">
            <div className="font-medium text-white">{user?.name}</div>
            <div className="text-dark-muted text-[11px] truncate">{user?.email}</div>
          </div>
        </div>

        <button
          onClick={logout}
          className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-colors"
        >
          <LogOut className="w-4 h-4" />
          Sign Out
        </button>
      </div>
    </aside>
  );
}
