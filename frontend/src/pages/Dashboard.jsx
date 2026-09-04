import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { 
  Users, 
  Briefcase, 
  CheckSquare, 
  FileText, 
  IndianRupee,
  Clock,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  Flame,
  Activity,
  History,
  ExternalLink
} from 'lucide-react';
import { Link } from 'react-router-dom';

function StatCard({ label, value, sublabel, accent = "brass" }) {
  const accentMap = {
    brass: "text-brass-dark bg-brass-soft/40 border-brass/30",
    forest: "text-forest bg-forest-soft border-forest/30",
    neutral: "text-ink-muted bg-paper border-border",
    rust: "text-rust bg-rust-soft/50 border-rust/30",
  };
  return (
    <div className="border-l-4 border-ink/20 pl-5 py-4 bg-surface rounded-r shadow-xs">
      <p className="font-sans text-xs uppercase tracking-wider text-ink-muted font-medium">{label}</p>
      <p className="font-display text-3xl font-bold text-ink mt-1.5">{value}</p>
      {sublabel && (
        <span className={`inline-block mt-2 text-[10px] font-mono px-2.5 py-0.5 rounded border ${accentMap[accent]}`}>
          {sublabel}
        </span>
      )}
    </div>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';

  const [stats, setStats] = useState({
    clients: 0,
    projects: 0,
    tasks: 0,
    invoices: 0,
    revenue: 0,
  });
  const [recentProjects, setRecentProjects] = useState([]);
  const [recentInvoices, setRecentInvoices] = useState([]);
  const [clientTasks, setClientTasks] = useState([]);
  const [auditEvents, setAuditEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const [projectsRes, tasksRes, invoicesRes] = await Promise.all([
        api.get('/projects'),
        api.get('/tasks'),
        api.get('/invoices'),
      ]);

      let clientsCount = 0;
      if (!isClient) {
        const [clientsRes, auditRes] = await Promise.all([
          api.get('/clients'),
          api.get('/audit-events'),
        ]);
        clientsCount = clientsRes.data.length;
        setAuditEvents(auditRes.data.slice(0, 6));
      }

      const totalRevenue = invoicesRes.data
        .filter((inv) => inv.status === 'PAID')
        .reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);

      setStats({
        clients: clientsCount,
        projects: projectsRes.data.length,
        tasks: tasksRes.data.length,
        invoices: invoicesRes.data.length,
        revenue: totalRevenue,
      });

      setRecentProjects(projectsRes.data);
      setRecentInvoices(invoicesRes.data.slice(0, 4));
      setClientTasks(tasksRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const completedTasks = clientTasks.filter((t) => t.status === 'DONE').length;
  const progressPercentage = clientTasks.length > 0 ? Math.round((completedTasks / clientTasks.length) * 100) : 0;
  const blockedTasks = clientTasks.filter((t) => t.status === 'REVIEW');

  const totalBudget = recentProjects.reduce((sum, p) => sum + (p.budget || 0), 0);
  const totalLoggedHours = clientTasks.reduce((sum, t) => sum + (t.actualHours || 0), 0);
  const estConsumedValue = totalLoggedHours * 150;
  const budgetConsumedPercent = totalBudget > 0 ? Math.min(100, Math.round((estConsumedValue / totalBudget) * 100)) : 0;

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar title={isClient ? 'Client Super Dashboard & Project Health' : 'Agency Command Center'} />

      <main className="flex-1 p-6 space-y-6">
        {/* Banner Sheet */}
        <div className="bg-surface p-6 rounded border border-border shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className={`flex items-center gap-2 font-mono text-[10px] font-bold tracking-wider mb-1.5 ${isClient ? 'text-forest' : 'text-brass-dark'}`}>
              <Sparkles className="w-3.5 h-3.5" />
              {isClient ? 'SUPER DASHBOARD - CLIENT HEALTH & TRANSPARENCY' : 'AGENCY COMMAND CENTER'}
            </div>
            <h1 className="font-display text-2xl font-bold text-ink">
              {isClient ? `Welcome back, ${user?.name}` : `Welcome to ${user?.agencyName || 'Agency Suite'}`}
            </h1>
            <p className="text-ink-muted text-xs mt-1">
              {isClient
                ? 'Real-time budget burn-down, milestone delivery velocity, and active blocker alerts.'
                : 'Traffic-light project health scores, resource allocation, and live activity audit feed.'}
            </p>
          </div>

          <div className="flex items-center gap-3">
            {!isClient && (
              <a
                href="/portal/share/demo-proposal-token-2026"
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 bg-paper hover:bg-ink/5 border border-border text-ink text-xs font-semibold px-4 py-2.5 rounded-full transition-all"
              >
                <ExternalLink className="w-3.5 h-3.5 text-brass-dark" />
                Demo Public Portal Share
              </a>
            )}
            {!isClient ? (
              <Link
                to="/ai-generator"
                className="inline-block bg-brass hover:bg-brass-dark text-white text-xs font-semibold px-5 py-2.5 rounded-full shadow-sm transition-all"
              >
                Draft Project Proposal
              </Link>
            ) : (
              <Link
                to="/tasks"
                className="inline-block bg-forest hover:bg-forest/90 text-white text-xs font-semibold px-5 py-2.5 rounded-full shadow-sm transition-all"
              >
                View Milestone Tasks
              </Link>
            )}
          </div>
        </div>

        {/* Actionable Client Blocker Alert Box */}
        {isClient && blockedTasks.length > 0 && (
          <div className="bg-rust-soft border border-rust/40 p-4 rounded shadow-xs flex items-center justify-between">
            <div className="flex items-center gap-3">
              <AlertTriangle className="w-5 h-5 text-rust shrink-0" />
              <div>
                <h4 className="font-display font-bold text-sm text-rust">Action Required: {blockedTasks.length} Pending Review / Approvals</h4>
                <p className="text-xs text-ink-muted mt-0.5">
                  The agency has submitted deliverables awaiting your sign-off: <span className="font-semibold text-ink">{blockedTasks.map(t => t.title).join(', ')}</span>.
                </p>
              </div>
            </div>
            <Link to="/tasks" className="bg-rust hover:bg-rust/90 text-white text-xs font-semibold px-4 py-2 rounded-full shrink-0 shadow-xs">
              Review Now
            </Link>
          </div>
        )}

        {/* Client Budget Burn-Down & Delivery Velocity Section */}
        {isClient ? (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            <div className="lg:col-span-7 bg-surface p-6 rounded border border-border shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-border">
                <h3 className="font-display font-semibold text-base text-ink flex items-center gap-2">
                  <Flame className="w-4 h-4 text-brass-dark" />
                  Budget Burn-Down & Financial Velocity
                </h3>
                <span className="font-mono text-xs font-bold text-forest bg-forest-soft px-2.5 py-0.5 rounded border border-forest/20">
                  {budgetConsumedPercent}% Consumed
                </span>
              </div>

              <div className="space-y-3">
                <div className="flex justify-between text-xs font-mono">
                  <span className="text-ink-muted">Logged Work Value: <strong className="text-ink">₹{estConsumedValue.toLocaleString()}</strong></span>
                  <span className="text-ink-muted">Total Contract Budget: <strong className="text-ink">₹{totalBudget.toLocaleString()}</strong></span>
                </div>

                <div className="w-full bg-paper h-4 rounded border border-border overflow-hidden p-0.5 flex">
                  <div
                    className="bg-gradient-to-r from-brass to-brass-dark h-full rounded transition-all duration-500"
                    style={{ width: `${budgetConsumedPercent}%` }}
                  ></div>
                </div>

                <div className="flex justify-between text-[11px] text-ink-muted pt-1">
                  <span>Project Start</span>
                  <span className="font-semibold text-forest">Estimated Completion: On Schedule</span>
                  <span>Target Budget Limit</span>
                </div>
              </div>
            </div>

            <div className="lg:col-span-5 bg-surface p-6 rounded border border-border shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-border">
                <h3 className="font-display font-semibold text-base text-ink flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-forest" />
                  Delivery Velocity Nodes
                </h3>
                <span className="font-mono text-lg font-bold text-forest">{progressPercentage}%</span>
              </div>

              <div className="space-y-3">
                <p className="text-xs text-ink-muted">
                  {completedTasks} of {clientTasks.length} milestone tasks completed cleanly.
                </p>

                <div className="flex items-center gap-2 py-2">
                  {clientTasks.map((t) => (
                    <div
                      key={t.id}
                      className={`h-3 flex-1 rounded transition-all duration-300 ${
                        t.status === 'DONE'
                          ? 'bg-forest'
                          : t.status === 'IN_PROGRESS'
                          ? 'bg-brass-soft border border-brass'
                          : 'bg-paper border border-border'
                      }`}
                      title={`${t.title} (${t.status})`}
                    ></div>
                  ))}
                </div>

                <div className="flex justify-between text-[10px] font-mono text-ink-muted">
                  <span>Scope Definition</span>
                  <span>UAT & Launch</span>
                </div>
              </div>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Health Scores */}
            <div className="lg:col-span-7 bg-surface p-6 rounded border border-border shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-border">
                <h3 className="font-display font-semibold text-base text-ink flex items-center gap-2">
                  <Activity className="w-4 h-4 text-brass-dark" />
                  Traffic-Light Project Health Scores
                </h3>
                <span className="text-xs font-mono text-ink-muted">Global Operations Scope</span>
              </div>

              <div className="space-y-3">
                {recentProjects.map((proj) => {
                  const projTasks = clientTasks.filter((t) => t.projectId === proj.id);
                  const projDone = projTasks.filter((t) => t.status === 'DONE').length;
                  const healthStatus = projTasks.length > 0 && projDone / projTasks.length >= 0.5 ? 'GREEN' : 'YELLOW';

                  return (
                    <div key={proj.id} className="p-4 rounded border border-border bg-paper flex items-center justify-between">
                      <div>
                        <h4 className="font-display font-bold text-sm text-ink">{proj.title}</h4>
                        <span className="text-xs text-ink-muted font-mono">Client: {proj.clientName}</span>
                      </div>
                      <div className="text-right space-y-1">
                        <span className={`font-mono text-[9px] px-2 py-0.5 rounded font-bold uppercase inline-block ${
                          healthStatus === 'GREEN' ? 'bg-forest-soft text-forest border border-forest/20' : 'bg-brass-soft text-brass-dark border border-brass/20'
                        }`}>
                          {healthStatus === 'GREEN' ? '● ON TRACK' : '▲ MONITORING'}
                        </span>
                        <div className="font-mono text-xs font-bold text-ink">₹{proj.budget?.toLocaleString()}</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Audit & Activity Log Feed Timeline */}
            <div className="lg:col-span-5 bg-surface p-6 rounded border border-border shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-border">
                <h3 className="font-display font-semibold text-base text-ink flex items-center gap-2">
                  <History className="w-4 h-4 text-brass-dark" />
                  Client & Audit Activity Feed
                </h3>
                <span className="font-mono text-[10px] text-ink-muted">REAL-TIME LOG</span>
              </div>

              {auditEvents.length === 0 ? (
                <p className="text-xs text-ink-muted py-6 text-center">No activity logged yet.</p>
              ) : (
                <div className="space-y-3 font-mono text-xs">
                  {auditEvents.map((evt) => (
                    <div key={evt.id} className="p-3 rounded border border-border/80 bg-paper space-y-1">
                      <div className="flex items-center justify-between text-[11px] font-bold text-brass-dark">
                        <span>{evt.actor}</span>
                        <span className="text-[10px] text-ink-muted">
                          {new Date(evt.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className="text-ink text-[11px] font-sans">{evt.details}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Hierarchical Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="md:col-span-2">
            <StatCard
              label={isClient ? 'Total Invoiced Amount' : 'Total Revenue (Paid)'}
              value={`₹${stats.revenue.toLocaleString()}`}
              sublabel="Active ledger balance"
              accent="forest"
            />
          </div>

          <StatCard
            label={isClient ? 'Active Projects' : 'Ongoing Projects'}
            value={stats.projects.toString()}
            sublabel="Tracked contract limits"
            accent="brass"
          />

          <StatCard
            label={isClient ? 'Milestone Tasks' : 'Active Tasks'}
            value={stats.tasks.toString()}
            sublabel="Assigned deliverables"
            accent="neutral"
          />
        </div>
      </main>
    </div>
  );
}
