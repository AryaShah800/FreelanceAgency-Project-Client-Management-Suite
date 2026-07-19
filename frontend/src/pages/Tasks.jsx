import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { CheckSquare, Clock, Plus, Trash2, Zap } from 'lucide-react';

const TASK_STATUSES = [
  { id: 'TODO', label: 'To Do', color: 'border-slate-500/40 bg-slate-500/5' },
  { id: 'IN_PROGRESS', label: 'In Progress', color: 'border-blue-500/40 bg-blue-500/5' },
  { id: 'REVIEW', label: 'In Review', color: 'border-purple-500/40 bg-purple-500/5' },
  { id: 'DONE', label: 'Done', color: 'border-emerald-500/40 bg-emerald-500/5' },
];

export default function Tasks() {
  const [tasks, setTasks] = useState([]);
  const [projects, setProjects] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [newTask, setNewTask] = useState({
    projectId: '',
    title: '',
    description: '',
    estimatedHours: 1,
    status: 'TODO',
  });

  useEffect(() => {
    fetchTasks();
    fetchProjects();
  }, []);

  const fetchTasks = async () => {
    try {
      const res = await api.get('/tasks');
      setTasks(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchProjects = async () => {
    try {
      const res = await api.get('/projects');
      setProjects(res.data);
      if (res.data.length > 0) {
        setNewTask((prev) => ({ ...prev, projectId: res.data[0].id }));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await api.post('/tasks', newTask);
      setShowModal(false);
      setNewTask({ projectId: projects[0]?.id || '', title: '', description: '', estimatedHours: 1, status: 'TODO' });
      fetchTasks();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create task');
    }
  };

  const handleStatusChange = async (task, newStatus) => {
    try {
      await api.put(`/tasks/${task.id}`, { ...task, status: newStatus });
      fetchTasks();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete task?')) return;
    try {
      await api.delete(`/tasks/${id}`);
      fetchTasks();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar title="STOMP WebSocket Real-time Task Board" onQuickAction={() => setShowModal(true)} />

      <main className="flex-1 p-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {TASK_STATUSES.map((status) => {
            const statusTasks = tasks.filter((t) => t.status === status.id);
            return (
              <div key={status.id} className={`glass-panel p-4 rounded-2xl border ${status.color}`}>
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-xs font-bold text-white uppercase tracking-wider">{status.label}</h3>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-dark-card text-dark-muted">
                    {statusTasks.length}
                  </span>
                </div>

                <div className="space-y-3">
                  {statusTasks.map((task) => (
                    <div key={task.id} className="glass-card p-4 rounded-xl space-y-2 relative group">
                      <div className="flex items-start justify-between">
                        <h4 className="text-sm font-bold text-white">{task.title}</h4>
                        <button
                          onClick={() => handleDelete(task.id)}
                          className="opacity-0 group-hover:opacity-100 text-dark-muted hover:text-red-400 transition-opacity p-1"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {task.description && (
                        <p className="text-xs text-dark-muted line-clamp-2">{task.description}</p>
                      )}

                      <div className="flex items-center justify-between text-xs text-dark-muted pt-2 border-t border-dark-border/40">
                        <div className="flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5 text-brand-400" />
                          <span>{task.estimatedHours} hrs est.</span>
                        </div>

                        <select
                          value={task.status}
                          onChange={(e) => handleStatusChange(task, e.target.value)}
                          className="bg-dark-bg border border-dark-border text-[11px] text-dark-muted rounded-lg px-2 py-0.5 focus:outline-none"
                        >
                          {TASK_STATUSES.map((s) => (
                            <option key={s.id} value={s.id}>
                              {s.label}
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </main>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-dark-border w-full max-w-md">
            <h3 className="text-lg font-bold text-white mb-4">Create New Task</h3>
            <form onSubmit={handleCreate} className="space-y-3 text-xs">
              <div>
                <label className="block text-dark-muted font-semibold mb-1">Project *</label>
                <select
                  required
                  value={newTask.projectId}
                  onChange={(e) => setNewTask({ ...newTask, projectId: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                >
                  <option value="">Select a Project</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title} ({p.clientName})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-dark-muted font-semibold mb-1">Task Title *</label>
                <input
                  type="text"
                  required
                  value={newTask.title}
                  onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
              </div>

              <div>
                <label className="block text-dark-muted font-semibold mb-1">Description</label>
                <textarea
                  rows="3"
                  value={newTask.description}
                  onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
              </div>

              <div>
                <label className="block text-dark-muted font-semibold mb-1">Estimated Hours</label>
                <input
                  type="number"
                  step="0.5"
                  value={newTask.estimatedHours}
                  onChange={(e) => setNewTask({ ...newTask, estimatedHours: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
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
                  Create Task
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
