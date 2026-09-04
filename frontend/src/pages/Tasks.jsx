import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import TaskCommentDrawer from '../components/TaskCommentDrawer';
import { useAuth } from '../context/AuthContext';
import { wsService } from '../services/websocket';
import { Clock, Plus, Trash2, Eye, EyeOff, MessageSquare, ChevronDown, Play, Square, WifiOff, RefreshCw } from 'lucide-react';

const TASK_STATUSES = [
  { id: 'TODO', label: 'To Do', color: 'border-t-ink-faint bg-surface' },
  { id: 'IN_PROGRESS', label: 'In Progress', color: 'border-t-brass bg-surface' },
  { id: 'REVIEW', label: 'In Review', color: 'border-t-indigo-400 bg-surface' },
  { id: 'DONE', label: 'Done', color: 'border-t-forest bg-surface' },
];

export default function Tasks() {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const [tasks, setTasks] = useState([]);
  const [projects, setProjects] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [activeTaskForComments, setActiveTaskForComments] = useState(null);
  const [openDropdownId, setOpenDropdownId] = useState(null);

  // Time tracking & offline states
  const [activeTimer, setActiveTimer] = useState(null);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [isOffline, setIsOffline] = useState(!navigator.onLine);
  const [pendingOfflineQueue, setPendingOfflineQueue] = useState([]);

  const [newTask, setNewTask] = useState({
    projectId: '',
    title: '',
    description: '',
    estimatedHours: 1,
    status: 'TODO',
    isClientVisible: true,
  });

  useEffect(() => {
    fetchTasks();
    fetchProjects();
    fetchActiveTimer();
    setupNetworkAndWebSocket();

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const setupNetworkAndWebSocket = () => {
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Sync offline queue if present
    const savedQueue = JSON.parse(localStorage.getItem('offline_time_queue') || '[]');
    setPendingOfflineQueue(savedQueue);

    // STOMP WebSocket Live Updates Subscription
    wsService.connect(() => {
      wsService.subscribe('/topic/tasks', (updatedTask) => {
        fetchTasks();
      });
    });
  };

  const handleOnline = async () => {
    setIsOffline(false);
    await processOfflineQueue();
  };

  const handleOffline = () => {
    setIsOffline(true);
  };

  const processOfflineQueue = async () => {
    const queue = JSON.parse(localStorage.getItem('offline_time_queue') || '[]');
    if (queue.length === 0) return;

    for (const item of queue) {
      try {
        if (item.type === 'START') {
          await api.post(`/time-entries/start?taskId=${item.taskId}`);
        } else if (item.type === 'STOP') {
          await api.post(`/time-entries/${item.entryId}/stop`);
        }
      } catch (err) {
        console.error('Failed to sync offline item', err);
      }
    }

    localStorage.removeItem('offline_time_queue');
    setPendingOfflineQueue([]);
    fetchActiveTimer();
    fetchTasks();
  };

  // Timer ticker interval
  useEffect(() => {
    let interval = null;
    if (activeTimer && activeTimer.startTime) {
      interval = setInterval(() => {
        const start = new Date(activeTimer.startTime).getTime();
        const now = new Date().getTime();
        setElapsedSeconds(Math.floor((now - start) / 1000));
      }, 1000);
    } else {
      setElapsedSeconds(0);
    }
    return () => clearInterval(interval);
  }, [activeTimer]);

  const fetchActiveTimer = async () => {
    if (!navigator.onLine) return;
    try {
      const res = await api.get('/time-entries/active');
      if (res.data) {
        setActiveTimer(res.data);
      } else {
        setActiveTimer(null);
      }
    } catch (err) {
      console.error(err);
    }
  };

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

  const handleStartTimer = async (taskId) => {
    if (!navigator.onLine) {
      // Offline PWA Queue Fallback
      const queuedItem = { type: 'START', taskId, timestamp: new Date().toISOString() };
      const newQueue = [...pendingOfflineQueue, queuedItem];
      localStorage.setItem('offline_time_queue', JSON.stringify(newQueue));
      setPendingOfflineQueue(newQueue);
      setActiveTimer({ id: 'temp', taskId, taskTitle: 'Offline Task', startTime: new Date().toISOString() });
      return;
    }

    try {
      const res = await api.post(`/time-entries/start?taskId=${taskId}`);
      setActiveTimer(res.data);
      fetchTasks();
    } catch (err) {
      alert('Failed to start timer');
    }
  };

  const handleStopTimer = async () => {
    if (!activeTimer) return;

    if (!navigator.onLine) {
      // Offline PWA Queue Fallback
      const queuedItem = { type: 'STOP', entryId: activeTimer.id, timestamp: new Date().toISOString() };
      const newQueue = [...pendingOfflineQueue, queuedItem];
      localStorage.setItem('offline_time_queue', JSON.stringify(newQueue));
      setPendingOfflineQueue(newQueue);
      setActiveTimer(null);
      return;
    }

    try {
      await api.post(`/time-entries/${activeTimer.id}/stop`);
      setActiveTimer(null);
      fetchTasks();
    } catch (err) {
      alert('Failed to stop timer');
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await api.post('/tasks', newTask);
      setShowModal(false);
      setNewTask({
        projectId: projects[0]?.id || '',
        title: '',
        description: '',
        estimatedHours: 1,
        status: 'TODO',
        isClientVisible: true,
      });
      fetchTasks();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create task');
    }
  };

  const handleStatusChange = async (task, newStatus) => {
    try {
      await api.put(`/tasks/${task.id}`, { ...task, status: newStatus });
      setOpenDropdownId(null);
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

  const formatTimer = (totalSec) => {
    const hrs = Math.floor(totalSec / 3600);
    const mins = Math.floor((totalSec % 3600) / 60);
    const secs = totalSec % 60;
    return `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar
        title={isClient ? "Project Milestones & Deliverables" : "Task Board & PWA Offline Sync"}
        onQuickAction={isClient ? null : () => setShowModal(true)}
      />

      <main className="flex-1 p-6 space-y-6">
        {/* Offline Banner Indicator */}
        {isOffline && (
          <div className="bg-rust-soft border border-rust text-rust p-3 rounded text-xs flex items-center justify-between font-semibold">
            <span className="flex items-center gap-2">
              <WifiOff className="w-4 h-4" />
              Offline Mode Active: Time tracking actions will be queued locally and synced automatically upon reconnection.
            </span>
            {pendingOfflineQueue.length > 0 && (
              <span className="font-mono bg-rust/20 px-2 py-0.5 rounded">
                {pendingOfflineQueue.length} queued action(s)
              </span>
            )}
          </div>
        )}

        {/* Active Time Tracker Header Bar */}
        {activeTimer && (
          <div className="bg-surface border border-brass p-4 rounded shadow-sm flex items-center justify-between animate-in fade-in">
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 rounded-full bg-rust animate-ping"></div>
              <div>
                <span className="font-mono text-[10px] uppercase font-bold text-brass-dark tracking-wider">
                  TIMETRACKER RUNNING {isOffline ? '(OFFLINE QUEUED)' : '(STOMP SYNCED)'}
                </span>
                <h4 className="font-display font-bold text-sm text-ink">{activeTimer.taskTitle}</h4>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <span className="font-mono text-xl font-bold text-ink">{formatTimer(elapsedSeconds)}</span>
              <button
                onClick={handleStopTimer}
                className="bg-rust hover:bg-rust/90 text-white font-semibold text-xs px-4 py-2 rounded-full flex items-center gap-1.5 shadow-sm transition-all"
              >
                <Square className="w-3.5 h-3.5 fill-current" />
                Stop & Log Hours
              </button>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {TASK_STATUSES.map((status) => {
            const statusTasks = tasks.filter((t) => t.status === status.id);
            return (
              <div key={status.id} className={`border border-border border-t-4 rounded-b shadow-sm p-4 min-w-[240px] ${status.color}`}>
                <div className="flex items-center justify-between mb-4 border-b border-border/40 pb-2">
                  <h3 className="font-sans text-xs font-bold uppercase tracking-wider text-ink-muted">{status.label}</h3>
                  <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-paper border border-border text-ink-muted">
                    {statusTasks.length}
                  </span>
                </div>

                <div className="space-y-3">
                  {statusTasks.map((task) => {
                    const isCurrentTimer = activeTimer && activeTimer.taskId === task.id;
                    return (
                      <div key={task.id} className={`bg-surface border p-4 rounded transition-all relative group ${isCurrentTimer ? 'border-brass shadow-sm' : 'border-border hover:border-brass/40'}`}>
                        <div className="flex items-start justify-between gap-1">
                          <div className="space-y-1">
                            {!isClient && (
                              <div className="flex items-center gap-1.5 text-[10px] font-mono">
                                {task.isClientVisible ? (
                                  <span className="text-forest bg-forest-soft px-1.5 py-0.5 rounded flex items-center gap-1 font-semibold">
                                    <Eye className="w-3 h-3" /> Client
                                  </span>
                                ) : (
                                  <span className="text-ink-muted bg-paper border border-border px-1.5 py-0.5 rounded flex items-center gap-1">
                                    <EyeOff className="w-3 h-3" /> Internal
                                  </span>
                                )}
                              </div>
                            )}
                            <h4 className="font-display font-semibold text-sm text-ink leading-tight mt-1">{task.title}</h4>
                          </div>
                          {!isClient && (
                            <button
                              onClick={() => handleDelete(task.id)}
                              className="opacity-0 group-hover:opacity-100 text-ink-faint hover:text-rust transition-opacity p-0.5"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>

                        {task.description && (
                          <p className="text-xs text-ink-muted line-clamp-2 leading-relaxed mt-2">{task.description}</p>
                        )}

                        {/* Hours Logged Badge */}
                        <div className="mt-3 flex items-center justify-between text-[11px] font-mono text-ink-muted bg-paper p-1.5 rounded border border-border/60">
                          <span className="flex items-center gap-1">
                            <Clock className="w-3 h-3 text-brass" /> Logged: {task.actualHours ? task.actualHours.toFixed(1) : '0.0'} hrs
                          </span>
                          {!isClient && (
                            isCurrentTimer ? (
                              <button
                                onClick={handleStopTimer}
                                className="text-rust font-bold hover:underline flex items-center gap-1"
                              >
                                <Square className="w-3 h-3 fill-current" /> Stop
                              </button>
                            ) : (
                              <button
                                onClick={() => handleStartTimer(task.id)}
                                className="text-brass-dark font-bold hover:underline flex items-center gap-1"
                              >
                                <Play className="w-3 h-3 fill-current" /> Start Timer
                              </button>
                            )
                          )}
                        </div>

                        <div className="flex items-center justify-between text-xs text-ink-muted pt-3 mt-3 border-t border-border/40 relative">
                          <button
                            onClick={() => setActiveTaskForComments(task)}
                            className="flex items-center gap-1 text-brass-dark hover:underline text-[11px] font-semibold"
                          >
                            <MessageSquare className="w-3.5 h-3.5" />
                            Discussion
                          </button>

                          {!isClient ? (
                            <div className="relative">
                              <button
                                onClick={() => setOpenDropdownId(openDropdownId === task.id ? null : task.id)}
                                className="bg-paper border border-border text-[10px] text-ink rounded px-2.5 py-1 flex items-center gap-1 font-semibold focus:outline-none hover:bg-ink/5"
                              >
                                <span>{task.status}</span>
                                <ChevronDown className="w-3 h-3 text-ink-faint" />
                              </button>

                              {openDropdownId === task.id && (
                                <div className="absolute right-0 bottom-full mb-1.5 w-32 bg-surface border border-border rounded shadow-lg z-50 py-1">
                                  {TASK_STATUSES.map((s) => (
                                    <button
                                      key={s.id}
                                      type="button"
                                      onClick={() => handleStatusChange(task, s.id)}
                                      className={`w-full text-left px-3 py-1.5 text-[10px] font-medium hover:bg-paper ${
                                        task.status === s.id ? 'text-brass-dark font-bold bg-brass-soft/20' : 'text-ink-muted'
                                      }`}
                                    >
                                      {s.label}
                                    </button>
                                  ))}
                                </div>
                              )}
                            </div>
                          ) : (
                            <span className="font-mono text-[9px] font-bold text-forest uppercase tracking-wider bg-forest-soft px-2 py-0.5 rounded border border-forest/20">
                              {task.status}
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </main>

      {/* Task Comments Slide-over Drawer */}
      {activeTaskForComments && (
        <TaskCommentDrawer
          task={activeTaskForComments}
          onClose={() => setActiveTaskForComments(null)}
        />
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-ink/40 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border p-6 rounded shadow-lg w-full max-w-md">
            <h3 className="font-display font-bold text-lg text-ink mb-4">Create Task</h3>
            <form onSubmit={handleCreate} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Project *</label>
                <select
                  required
                  value={newTask.projectId}
                  onChange={(e) => setNewTask({ ...newTask, projectId: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
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
                <label className="block text-ink-muted font-semibold mb-1">Task Title *</label>
                <input
                  type="text"
                  required
                  value={newTask.title}
                  onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-ink-muted font-semibold mb-1">Description</label>
                <textarea
                  rows="3"
                  value={newTask.description}
                  onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-ink-muted font-semibold mb-1">Estimated Hours</label>
                <input
                  type="number"
                  step="0.5"
                  value={newTask.estimatedHours}
                  onChange={(e) => setNewTask({ ...newTask, estimatedHours: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="clientVisible"
                  checked={newTask.isClientVisible}
                  onChange={(e) => setNewTask({ ...newTask, isClientVisible: e.target.checked })}
                  className="rounded bg-paper border-border text-brass focus:ring-brass"
                />
                <label htmlFor="clientVisible" className="text-ink font-medium">
                  Visible to Client in Portal
                </label>
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
