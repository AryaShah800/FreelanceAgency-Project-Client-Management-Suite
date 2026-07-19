import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { Briefcase, Calendar, IndianRupee, Trash2 } from 'lucide-react';

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
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar title="Projects & Deadlines" onQuickAction={() => setShowModal(true)} />

      <main className="flex-1 p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {projects.map((project) => (
            <div key={project.id} className="glass-panel p-5 rounded-2xl border border-dark-border space-y-4 relative group">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-[10px] uppercase font-bold text-brand-400 bg-brand-500/10 px-2 py-0.5 rounded-full border border-brand-500/20">
                    {project.clientName}
                  </span>
                  <h3 className="text-base font-bold text-white mt-1.5">{project.title}</h3>
                </div>
                <button
                  onClick={() => handleDelete(project.id)}
                  className="opacity-0 group-hover:opacity-100 text-dark-muted hover:text-red-400 transition-opacity p-1"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>

              {project.description && (
                <p className="text-xs text-dark-muted line-clamp-2">{project.description}</p>
              )}

              <div className="pt-3 border-t border-dark-border/40 flex items-center justify-between text-xs">
                <div className="flex items-center gap-1 text-emerald-400 font-bold">
                  <IndianRupee className="w-4 h-4" />
                  <span>₹{project.budget?.toLocaleString()}</span>
                </div>
                <div className="flex items-center gap-1 text-dark-muted">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>{project.deadline || 'No deadline'}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </main>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-dark-border w-full max-w-md">
            <h3 className="text-lg font-bold text-white mb-4">Create New Project</h3>
            <form onSubmit={handleCreate} className="space-y-3 text-xs">
              <div>
                <label className="block text-dark-muted font-semibold mb-1">Client *</label>
                <select
                  required
                  value={newProject.clientId}
                  onChange={(e) => setNewProject({ ...newProject, clientId: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
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
                <label className="block text-dark-muted font-semibold mb-1">Project Title *</label>
                <input
                  type="text"
                  required
                  value={newProject.title}
                  onChange={(e) => setNewProject({ ...newProject, title: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
              </div>

              <div>
                <label className="block text-dark-muted font-semibold mb-1">Description</label>
                <textarea
                  rows="3"
                  value={newProject.description}
                  onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-dark-muted font-semibold mb-1">Budget (₹) *</label>
                  <input
                    type="number"
                    required
                    value={newProject.budget}
                    onChange={(e) => setNewProject({ ...newProject, budget: e.target.value })}
                    className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                  />
                </div>
                <div>
                  <label className="block text-dark-muted font-semibold mb-1">Deadline</label>
                  <input
                    type="date"
                    value={newProject.deadline}
                    onChange={(e) => setNewProject({ ...newProject, deadline: e.target.value })}
                    className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded-xl bg-dark-card text-dark-muted hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-gradient-to-r from-brand-600 to-indigo-600 text-white font-semibold"
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
