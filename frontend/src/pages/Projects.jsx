import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { Briefcase, Calendar, IndianRupee, Trash2, Plus, TrendingUp, DollarSign, Receipt } from 'lucide-react';

export default function Projects() {
  const [projects, setProjects] = useState([]);
  const [clients, setClients] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [showExpenseModal, setShowExpenseModal] = useState(false);
  const [selectedProjectId, setSelectedProjectId] = useState(null);

  const [newProject, setNewProject] = useState({
    clientId: '',
    title: '',
    description: '',
    budget: '',
    deadline: '',
  });

  const [newExpense, setNewExpense] = useState({
    description: 'Contractor Payment / Hosting Fee',
    amount: 15000,
    category: 'Contractor',
  });

  const [projectProfitability, setProjectProfitability] = useState({});

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [projRes, clientRes] = await Promise.all([
        api.get('/projects'),
        api.get('/clients'),
      ]);
      setProjects(projRes.data);
      setClients(clientRes.data);

      // Fetch profitability for each project
      projRes.data.forEach(async (p) => {
        try {
          const profRes = await api.get(`/projects/${p.id}/expenses/profitability`);
          setProjectProfitability((prev) => ({ ...prev, [p.id]: profRes.data }));
        } catch (e) {
          console.error(e);
        }
      });
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await api.post('/projects', newProject);
      setShowModal(false);
      setNewProject({ clientId: '', title: '', description: '', budget: '', deadline: '' });
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create project');
    }
  };

  const handleCreateExpense = async (e) => {
    e.preventDefault();
    if (!selectedProjectId) return;
    try {
      await api.post(`/projects/${selectedProjectId}/expenses`, newExpense);
      setShowExpenseModal(false);
      fetchData();
    } catch (err) {
      alert('Failed to log expense');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete project?')) return;
    try {
      await api.delete(`/projects/${id}`);
      fetchData();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar title="Project Ledger & Net Profitability" onQuickAction={() => setShowModal(true)} />

      <main className="flex-1 p-6 space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-6xl">
          {projects.map((project) => {
            const metrics = projectProfitability[project.id] || {
              revenue: project.budget || 0,
              totalExpenses: 0,
              netProfit: project.budget || 0,
              marginPercent: 100,
            };

            const isHighProfit = metrics.marginPercent >= 50;

            return (
              <div key={project.id} className="bg-surface p-5 rounded border border-border space-y-4 relative group hover:border-brass/40 transition-all shadow-sm">
                <div className="flex items-start justify-between">
                  <div>
                    <span className="font-mono text-[9px] uppercase font-bold text-brass-dark bg-brass-soft/40 px-2 py-0.5 rounded border border-brass/20">
                      {project.clientName}
                    </span>
                    <h3 className="font-display font-semibold text-base text-ink mt-2">{project.title}</h3>
                  </div>
                  <button
                    onClick={() => handleDelete(project.id)}
                    className="opacity-0 group-hover:opacity-100 text-ink-faint hover:text-rust transition-opacity p-0.5"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                {project.description && (
                  <p className="text-xs text-ink-muted line-clamp-2 leading-relaxed">{project.description}</p>
                )}

                {/* Net Profitability Calculator Box */}
                <div className="p-3 bg-paper rounded border border-border/80 text-xs space-y-2">
                  <div className="flex items-center justify-between font-mono">
                    <span className="text-ink-muted">Net Profit Margin:</span>
                    <span className={`font-bold px-2 py-0.5 rounded text-[11px] ${
                      isHighProfit ? 'bg-forest-soft text-forest border border-forest/20' : 'bg-brass-soft text-brass-dark border border-brass/20'
                    }`}>
                      {metrics.marginPercent}% Margin
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[10px] font-mono text-ink-muted pt-1 border-t border-border/60">
                    <div>Rev: <strong className="text-ink">₹{metrics.revenue?.toLocaleString()}</strong></div>
                    <div>Exp: <strong className="text-rust">₹{metrics.totalExpenses?.toLocaleString()}</strong></div>
                  </div>
                </div>

                <div className="pt-3 border-t border-border flex items-center justify-between text-xs">
                  <button
                    onClick={() => {
                      setSelectedProjectId(project.id);
                      setShowExpenseModal(true);
                    }}
                    className="text-brass-dark hover:underline font-semibold flex items-center gap-1 text-[11px]"
                  >
                    <Receipt className="w-3.5 h-3.5" /> Log Expense
                  </button>

                  <div className="flex items-center gap-1 text-ink-muted font-mono text-[11px]">
                    <Calendar className="w-3.5 h-3.5" />
                    <span>{project.deadline || 'No deadline'}</span>
                  </div>
                </div>
              </div>
            );
          })}

          {/* New Project Ghost CTA Card */}
          <button
            onClick={() => setShowModal(true)}
            className="border-2 border-dashed border-border hover:border-brass/40 transition-all rounded bg-surface/40 p-6 flex flex-col items-center justify-center text-center space-y-2 h-full min-h-[160px]"
          >
            <Plus className="w-6 h-6 text-ink-faint" />
            <span className="font-display text-sm font-semibold text-ink">New Project Contract</span>
            <span className="text-[11px] text-ink-muted">Add another client assignment</span>
          </button>
        </div>
      </main>

      {/* Expense Modal */}
      {showExpenseModal && (
        <div className="fixed inset-0 bg-ink/40 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border p-6 rounded shadow-lg w-full max-w-md">
            <h3 className="font-display font-bold text-lg text-ink mb-1">Log Project Expense</h3>
            <p className="text-xs text-ink-muted mb-4">Record vendor costs or overhead to recalculate real-time Net Profit Margin.</p>

            <form onSubmit={handleCreateExpense} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Description *</label>
                <input
                  type="text"
                  required
                  value={newExpense.description}
                  onChange={(e) => setNewExpense({ ...newExpense, description: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">Amount (₹) *</label>
                  <input
                    type="number"
                    required
                    value={newExpense.amount}
                    onChange={(e) => setNewExpense({ ...newExpense, amount: e.target.value })}
                    className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">Category</label>
                  <select
                    value={newExpense.category}
                    onChange={(e) => setNewExpense({ ...newExpense, category: e.target.value })}
                    className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                  >
                    <option value="Contractor">Contractor</option>
                    <option value="Software">Software</option>
                    <option value="Hosting">Hosting</option>
                    <option value="Overhead">Overhead</option>
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-border mt-4">
                <button
                  type="button"
                  onClick={() => setShowExpenseModal(false)}
                  className="px-4 py-2 rounded bg-paper text-ink-muted border border-border hover:bg-ink/5"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded bg-brass text-white font-semibold hover:bg-brass-dark"
                >
                  Log Expense
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-ink/40 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border p-6 rounded shadow-lg w-full max-w-md">
            <h3 className="font-display font-bold text-lg text-ink mb-4">Create Project</h3>
            <form onSubmit={handleCreate} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Client *</label>
                <select
                  required
                  value={newProject.clientId}
                  onChange={(e) => setNewProject({ ...newProject, clientId: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                >
                  <option value="">Select a Client</option>
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.companyName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-ink-muted font-semibold mb-1">Project Title *</label>
                <input
                  type="text"
                  required
                  value={newProject.title}
                  onChange={(e) => setNewProject({ ...newProject, title: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-ink-muted font-semibold mb-1">Description</label>
                <textarea
                  rows="3"
                  value={newProject.description}
                  onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">Budget (₹) *</label>
                  <input
                    type="number"
                    required
                    value={newProject.budget}
                    onChange={(e) => setNewProject({ ...newProject, budget: e.target.value })}
                    className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">Deadline</label>
                  <input
                    type="date"
                    value={newProject.deadline}
                    onChange={(e) => setNewProject({ ...newProject, deadline: e.target.value })}
                    className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-border mt-4">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded bg-paper text-ink-muted border border-border hover:bg-ink/5"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded bg-brass text-white font-semibold hover:bg-brass-dark"
                >
                  Create Project
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
