import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { Briefcase, Calendar, IndianRupee, Trash2, Plus } from 'lucide-react';

export default function Projects() {
  const [projects, setProjects] = useState([]);
  const [clients, setClients] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [newProject, setNewProject] = useState({
    clientId: '',
    title: '',
    description: '',
    budget: '',
    deadline: '',
  });

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
      <Navbar title="Project Ledger & Contracts" onQuickAction={() => setShowModal(true)} />

      <main className="flex-1 p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-6xl">
          {projects.map((project) => (
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

              <div className="pt-3 border-t border-border flex items-center justify-between text-xs">
                <div className="flex items-center gap-1 text-ink font-bold font-mono">
                  <IndianRupee className="w-3.5 h-3.5 text-ink-muted" />
                  <span>₹{project.budget?.toLocaleString()}</span>
                </div>
                <div className="flex items-center gap-1 text-ink-muted font-mono text-[11px]">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>{project.deadline || 'No deadline'}</span>
                </div>
              </div>
            </div>
          ))}

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
