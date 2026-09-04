import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { wsService } from '../services/websocket';
import { X, Send, MessageSquare, Eye, EyeOff } from 'lucide-react';

export default function TaskCommentDrawer({ task, onClose }) {
  const { user } = useAuth();
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (task) {
      fetchComments();
      subscribeToLiveComments();
    }

    return () => {
      if (task && task.projectId) {
        wsService.unsubscribe(`/topic/project/${task.projectId}/comments`);
      }
    };
  }, [task]);

  const subscribeToLiveComments = () => {
    if (!task || !task.projectId) return;
    const topic = `/topic/project/${task.projectId}/comments`;
    wsService.connect(() => {
      wsService.subscribe(topic, (incomingComment) => {
        setComments((prev) => {
          if (prev.some((c) => c.id === incomingComment.id)) return prev;
          return [...prev, incomingComment];
        });
      });
    });
  };

  const fetchComments = async () => {
    try {
      const res = await api.get(`/tasks/${task.id}/comments`);
      setComments(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSend = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setSending(true);
    try {
      const res = await api.post(`/tasks/${task.id}/comments`, { content: newComment });
      setComments((prev) => {
        if (prev.some((c) => c.id === res.data.id)) return prev;
        return [...prev, res.data];
      });
      setNewComment('');
    } catch (err) {
      alert('Failed to post comment');
    } finally {
      setSending(false);
    }
  };

  if (!task) return null;

  return (
    <div className="fixed inset-0 z-50 bg-ink/40 backdrop-blur-xs flex justify-end">
      <div className="w-full max-w-md bg-surface h-full border-l border-border flex flex-col justify-between p-6 shadow-2xl animate-in slide-in-from-right duration-200">
        <div>
          {/* Header */}
          <div className="flex items-start justify-between pb-4 border-b border-border">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono text-[9px] font-bold px-2 py-0.5 rounded bg-paper border border-border text-ink-muted uppercase">
                  {task.status}
                </span>
                {task.isClientVisible ? (
                  <span className="text-[10px] font-mono font-semibold text-forest flex items-center gap-1">
                    <Eye className="w-3 h-3" /> Client Visible
                  </span>
                ) : (
                  <span className="text-[10px] font-mono font-semibold text-brass-dark flex items-center gap-1">
                    <EyeOff className="w-3 h-3" /> Internal Only
                  </span>
                )}
              </div>
              <h3 className="font-display font-bold text-base text-ink mt-1.5">{task.title}</h3>
            </div>
            <button
              onClick={onClose}
              className="p-1 rounded text-ink-muted hover:text-ink hover:bg-paper transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Comments List */}
          <div className="mt-4 space-y-3 overflow-y-auto max-h-[calc(100vh-250px)] pr-1">
            <h4 className="font-sans text-xs font-semibold text-ink-muted flex items-center gap-1.5 mb-2">
              <MessageSquare className="w-3.5 h-3.5 text-brass-dark" />
              Live In-App Discussion ({comments.length})
            </h4>

            {loading ? (
              <p className="text-xs text-ink-muted py-6 text-center">Loading discussion thread...</p>
            ) : comments.length === 0 ? (
              <div className="text-center py-8 text-ink-muted text-xs bg-paper rounded border border-border p-4">
                No comments yet. Start the conversation below!
              </div>
            ) : (
              comments.map((comment) => {
                const isClientRole = comment.authorRole === 'CLIENT';
                return (
                  <div
                    key={comment.id}
                    className={`p-3 rounded border text-xs space-y-1 ${
                      isClientRole
                        ? 'bg-forest-soft border-forest/30 text-forest ml-4'
                        : 'bg-paper border-border text-ink mr-4'
                    }`}
                  >
                    <div className="flex items-center justify-between font-semibold text-[11px]">
                      <span className={isClientRole ? 'text-forest font-bold' : 'text-brass-dark font-bold'}>
                        {comment.authorName} ({comment.authorRole})
                      </span>
                      <span className="text-[10px] text-ink-muted font-mono">
                        {new Date(comment.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="whitespace-pre-wrap leading-relaxed">{comment.content}</p>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Input Footer */}
        <form onSubmit={handleSend} className="pt-4 border-t border-border">
          <div className="flex items-center gap-2">
            <input
              type="text"
              required
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Write a message to the client..."
              className="flex-1 bg-paper border border-border text-ink text-xs rounded px-3.5 py-2.5 focus:outline-none focus:border-brass"
            />
            <button
              type="submit"
              disabled={sending}
              className="p-2.5 rounded bg-brass hover:bg-brass-dark text-white font-semibold shadow-sm transition-all"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
