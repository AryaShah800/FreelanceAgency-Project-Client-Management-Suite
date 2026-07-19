import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { 
  Users, 
  Briefcase, 
  CheckSquare, 
  FileText, 
  TrendingUp, 
  IndianRupee,
  Clock,
  Sparkles
} from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Dashboard() {
  const [stats, setStats] = useState({
    clients: 0,
    projects: 0,
    tasks: 0,
    invoices: 0,
    revenue: 0,
  });
  const [recentProjects, setRecentProjects] = useState([]);
  const [recentInvoices, setRecentInvoices] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const [clientsRes, projectsRes, tasksRes, invoicesRes] = await Promise.all([
        api.get('/clients'),
        api.get('/projects'),
        api.get('/tasks'),
        api.get('/invoices'),
      ]);

      const totalRevenue = invoicesRes.data
        .filter(inv => inv.status === 'PAID')
        .reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);

      setStats({
        clients: clientsRes.data.length,
        projects: projectsRes.data.length,
        tasks: tasksRes.data.length,
        invoices: invoicesRes.data.length,
        revenue: totalRevenue,
      });

      setRecentProjects(projectsRes.data.slice(0, 4));
      setRecentInvoices(invoicesRes.data.slice(0, 4));
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar title="Agency Overview & Analytics" />

      <main className="flex-1 p-6 space-y-6">
        {/* Banner */}
        <div className="glass-panel p-6 rounded-2xl border border-brand-500/20 bg-gradient-to-r from-brand-900/40 via-dark-card to-dark-bg flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 text-brand-400 font-semibold text-xs mb-1">
              <Sparkles className="w-4 h-4" />
              SPRING BOOT + HIBERNATE / JPA BACKEND
            </div>
            <h1 className="text-2xl font-extrabold text-white">Welcome to your Freelance Agency Suite</h1>
            <p className="text-dark-muted text-xs mt-1">Manage client proposals, projects, real-time tasks, and GST tax invoices seamlessly.</p>
          </div>
          <Link
            to="/ai-generator"
            className="flex items-center gap-2 bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white font-semibold text-xs px-4 py-2.5 rounded-xl shadow-lg shadow-brand-500/30 transition-all"
          >
            Draft Proposal with Gemini AI
          </Link>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="glass-card p-5 rounded-2xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-dark-muted">Total Revenue (Paid)</span>
              <div className="w-9 h-9 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
                <IndianRupee className="w-5 h-5" />
              </div>
            </div>
            <div className="text-2xl font-bold text-white mt-2">₹{stats.revenue.toLocaleString()}</div>
            <div className="flex items-center gap-1 text-[11px] text-emerald-400 font-medium mt-1">
              <TrendingUp className="w-3.5 h-3.5" />
              <span>Real-time invoice calculation</span>
            </div>
          </div>

          <div className="glass-card p-5 rounded-2xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-dark-muted">Active Clients</span>
              <div className="w-9 h-9 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center">
                <Users className="w-5 h-5" />
              </div>
            </div>
            <div className="text-2xl font-bold text-white mt-2">{stats.clients}</div>
            <span className="text-[11px] text-dark-muted">Scoped to your Agency ID</span>
          </div>

          <div className="glass-card p-5 rounded-2xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-dark-muted">Ongoing Projects</span>
              <div className="w-9 h-9 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center">
                <Briefcase className="w-5 h-5" />
              </div>
            </div>
            <div className="text-2xl font-bold text-white mt-2">{stats.projects}</div>
            <span className="text-[11px] text-dark-muted">Track deadlines & budget</span>
          </div>

          <div className="glass-card p-5 rounded-2xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-dark-muted">Active Tasks</span>
              <div className="w-9 h-9 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center">
                <CheckSquare className="w-5 h-5" />
              </div>
            </div>
            <div className="text-2xl font-bold text-white mt-2">{stats.tasks}</div>
            <span className="text-[11px] text-dark-muted">STOMP WebSocket synced</span>
          </div>
        </div>

        {/* Content Section */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Recent Projects */}
          <div className="glass-panel p-5 rounded-2xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-brand-400" />
                Recent Projects
              </h3>
              <Link to="/projects" className="text-xs text-brand-400 font-semibold hover:underline">View All</Link>
            </div>

            {recentProjects.length === 0 ? (
              <p className="text-dark-muted text-xs py-8 text-center">No projects added yet.</p>
            ) : (
              <div className="space-y-3">
                {recentProjects.map((project) => (
                  <div key={project.id} className="p-3.5 rounded-xl bg-dark-bg/60 border border-dark-border/60 flex items-center justify-between">
                    <div>
                      <h4 className="text-xs font-bold text-white">{project.title}</h4>
                      <p className="text-[11px] text-dark-muted">Client: {project.clientName}</p>
                    </div>
                    <div className="text-right">
                      <span className="text-xs font-bold text-emerald-400">₹{project.budget?.toLocaleString()}</span>
                      <p className="text-[10px] text-dark-muted flex items-center justify-end gap-1 mt-0.5">
                        <Clock className="w-3 h-3" />
                        {project.deadline || 'No deadline'}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Recent Invoices */}
          <div className="glass-panel p-5 rounded-2xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <FileText className="w-4 h-4 text-brand-400" />
                GST Tax Invoices
              </h3>
              <Link to="/invoices" className="text-xs text-brand-400 font-semibold hover:underline">View All</Link>
            </div>

            {recentInvoices.length === 0 ? (
              <p className="text-dark-muted text-xs py-8 text-center">No invoices generated yet.</p>
            ) : (
              <div className="space-y-3">
                {recentInvoices.map((inv) => (
                  <div key={inv.id} className="p-3.5 rounded-xl bg-dark-bg/60 border border-dark-border/60 flex items-center justify-between">
                    <div>
                      <span className="text-xs font-mono text-brand-400 font-bold">{inv.invoiceNumber}</span>
                      <p className="text-[11px] text-dark-muted">{inv.clientName}</p>
                    </div>
                    <div className="text-right">
                      <span className="text-xs font-bold text-white">₹{inv.totalAmount?.toLocaleString()}</span>
                      <div className="mt-1">
                        <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                          inv.status === 'PAID' ? 'bg-emerald-500/20 text-emerald-400' :
                          inv.status === 'SENT' ? 'bg-blue-500/20 text-blue-400' : 'bg-amber-500/20 text-amber-400'
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
