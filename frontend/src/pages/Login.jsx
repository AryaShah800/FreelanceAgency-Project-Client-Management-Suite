import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Building2, Lock, Mail, ArrowRight } from 'lucide-react';
import { trackEvent } from '../services/analytics';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      trackEvent('Login completed');
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials');
    }
  };

  return (
    <div className="min-h-screen bg-paper flex items-center justify-center p-4 relative overflow-hidden">
      {/* Subtle warm ambient details */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brass-soft/20 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-forest-soft/30 rounded-full blur-3xl pointer-events-none"></div>

      <div className="w-full max-w-md bg-surface p-8 rounded border border-border shadow-md relative z-10">
        <div className="flex flex-col items-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-brass flex items-center justify-center text-white mb-4 shadow-sm">
            <Building2 className="w-7 h-7 text-surface" />
          </div>
          <h2 className="font-display text-2xl font-bold text-ink tracking-tight">Ledger Portal Sign-In</h2>
          <p className="text-ink-muted text-xs mt-1.5 font-sans">Manage projects, clients, proposals, and GST invoicing</p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded bg-rust-soft/50 border border-rust/20 text-rust text-xs font-semibold">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-ink-muted mb-1.5">Work Email</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-ink-faint absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@agency.com"
                className="w-full bg-paper border border-border text-ink text-sm rounded pl-10 pr-4 py-2.5 focus:outline-none focus:border-brass transition-colors"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-ink-muted mb-1.5">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-ink-faint absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-paper border border-border text-ink text-sm rounded pl-10 pr-4 py-2.5 focus:outline-none focus:border-brass transition-colors"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-4 bg-brass hover:bg-brass-dark text-white font-semibold py-2.5 rounded-full shadow-sm flex items-center justify-center gap-2 transition-all text-sm"
          >
            {loading ? 'Authenticating...' : 'Sign In to Ledger'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="mt-6 flex flex-col gap-2 text-center text-xs text-ink-muted">
          <Link to="/forgot-password" className="text-brass-dark font-semibold hover:underline">Forgot password?</Link>
          <Link to="/verify-email" className="text-brass-dark font-semibold hover:underline">Need an email verification link?</Link>
          <Link to="/setup" className="mt-2 text-ink-faint hover:text-ink">First time setting up this installation?</Link>
          <span>Agency accounts are invitation-only.</span>
        </div>
      </div>
      <footer className="absolute bottom-4 left-0 right-0 z-10 flex justify-center gap-5 px-4 text-xs text-ink-muted">
        <Link to="/privacy" className="underline">Privacy</Link>
        <Link to="/terms" className="underline">Terms</Link>
        <Link to="/contact" className="underline">Contact</Link>
      </footer>
    </div>
  );
}
