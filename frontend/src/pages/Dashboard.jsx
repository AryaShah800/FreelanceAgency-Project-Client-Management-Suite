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
  ShieldAlert,
  TrendingUp,
  Activity
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
        const clientsRes = await api.get('/clients');
        clientsCount = clientsRes.data.length;
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

  // Budget calculations
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
                : 'Traffic-light project health scores, resource allocation, and incoming revenue forecasts.'}
            </p>
          </div>

          <div>
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
            {/* Budget Burn-Down Chart Component */}
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

                {/* Progress bar container */}
                <div className="w-full bg-paper h-4 rounded border border-border overflow-hidden p-0.5 flex">
                  <div
                    className="bg-gradient-to-r from-brass to-brass-dark h-full rounded transition-all duration-500"
                    style={{ width: `${budgetConsumedPercent}%` }}
                    title={`Budget Burned: ${budgetConsumedPercent}%`}
                  ></div>
                </div>

                <div className="flex justify-between text-[11px] text-ink-muted pt-1">
                  <span>Project Start</span>
                  <span className="font-semibold text-forest">Estimated Completion: On Schedule</span>
                  <span>Target Budget Limit</span>
                </div>
              </div>
            </div>

            {/* Segmented Milestone Progress Tracker */}
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
          /* Agency Command Center Health Scores */
          <div className="bg-surface p-6 rounded border border-border shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-border">
              <h3 className="font-display font-semibold text-base text-ink flex items-center gap-2">
                <Activity className="w-4 h-4 text-brass-dark" />
                Traffic-Light Project Health Scores
              </h3>
              <span className="text-xs font-mono text-ink-muted">Global Operations Scope</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {recentProjects.map((proj) => {
                const projTasks = clientTasks.filter((t) => t.projectId === proj.id);
                const projDone = projTasks.filter((t) => t.status === 'DONE').length;
                const healthStatus = projTasks.length > 0 && projDone / projTasks.length >= 0.5 ? 'GREEN' : 'YELLOW';

                return (
                  <div key={proj.id} className="p-4 rounded border border-border bg-paper space-y-2">
                    <div className="flex items-center justify-between">
                      <h4 className="font-display font-bold text-sm text-ink">{proj.title}</h4>
                      <span className={`font-mono text-[9px] px-2 py-0.5 rounded font-bold uppercase ${
                        healthStatus === 'GREEN' ? 'bg-forest-soft text-forest border border-forest/20' : 'bg-brass-soft text-brass-dark border border-brass/20'
                      }`}>
                        {healthStatus === 'GREEN' ? '● ON TRACK' : '▲ MONITORING'}
                      </span>
                    </div>
                    <div className="text-xs text-ink-muted font-mono flex justify-between">
                      <span>Client: {proj.clientName}</span>
                      <span>Budget: ₹{proj.budget?.toLocaleString()}</span>
                    </div>
                  </div>
                );
              })}
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

        {/* Ledger Activity & Feeds */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Projects Ledger */}
          <div className="bg-surface p-5 rounded border border-border shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-border">
              <h3 className="font-display font-semibold text-sm text-ink flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-brass-dark" />
                Project Ledger
              </h3>
              <Link to="/projects" className="text-xs text-brass-dark font-semibold hover:underline">
                View Ledger
              </Link>
            </div>

            {recentProjects.length === 0 ? (
              <p className="text-ink-muted text-xs py-8 text-center">No projects in current ledger.</p>
            ) : (
              <div className="divide-y divide-border/60">
                {recentProjects.map((project) => (
                  <div key={project.id} className="py-3 flex items-center justify-between text-xs">
                    <div>
                      <h4 className="font-semibold text-ink">{project.title}</h4>
                      <p className="text-ink-muted text-[11px] mt-0.5">Client: {project.clientName}</p>
                    </div>
                    <div className="text-right space-y-1">
                      <div className="font-mono font-bold text-ink">₹{project.budget?.toLocaleString()}</div>
                      <div className="text-[10px] text-ink-muted font-mono flex items-center justify-end gap-1">
                        <Clock className="w-3 h-3" />
                        {project.deadline || 'No deadline'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Invoices Feed */}
          <div className="bg-surface p-5 rounded border border-border shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-border">
              <h3 className="font-display font-semibold text-sm text-ink flex items-center gap-2">
                <FileText className="w-4 h-4 text-brass-dark" />
                GST Invoices Feed
              </h3>
              <Link to="/invoices" className="text-xs text-brass-dark font-semibold hover:underline">
                View Invoices
              </Link>
            </div>

            {recentInvoices.length === 0 ? (
              <p className="text-ink-muted text-xs py-8 text-center">No invoice records generated yet.</p>
            ) : (
              <div className="divide-y divide-border/60">
                {recentInvoices.map((inv) => (
                  <div key={inv.id} className="py-3 flex items-center justify-between text-xs">
                    <div>
                      <span className="font-mono text-brass-dark font-bold">{inv.invoiceNumber}</span>
                      <p className="text-ink-muted text-[11px] mt-0.5">{inv.clientName}</p>
                    </div>
                    <div className="text-right space-y-1">
                      <div className="font-mono font-bold text-ink">₹{inv.totalAmount?.toLocaleString()}</div>
                      <div>
                        <span className={`font-mono text-[9px] px-2 py-0.5 rounded-full font-bold uppercase ${
                          inv.status === 'PAID' ? 'bg-forest-soft text-forest' :
                          inv.status === 'SENT' ? 'bg-brass-soft text-brass-dark' : 'bg-rust-soft text-rust'
                        }`}>
                          {inv.status}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
