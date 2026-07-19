import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Building2, Lock, Mail, ArrowRight } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('owner@agency.com');
  const [password, setPassword] = useState('password123');
  const [error, setError] = useState('');
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials');
    }
  };

  return (
    <div className="min-h-screen bg-dark-bg flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-600/20 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none"></div>

      <div className="w-full max-w-md glass-panel p-8 rounded-2xl border border-dark-border shadow-2xl relative z-10">
        <div className="flex flex-col items-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center text-white mb-3 shadow-xl shadow-brand-500/30">
            <Building2 className="w-7 h-7" />
          </div>
          <h2 className="text-2xl font-bold text-white tracking-tight">Agency Portal Login</h2>
          <p className="text-dark-muted text-xs mt-1">Manage your clients, projects & GST invoices</p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-dark-muted mb-1.5">Work Email</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-dark-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@agency.com"
                className="w-full bg-dark-bg/80 border border-dark-border text-white text-sm rounded-xl pl-10 pr-4 py-2.5 focus:outline-none focus:border-brand-500 transition-colors"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-dark-muted mb-1.5">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-dark-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-dark-bg/80 border border-dark-border text-white text-sm rounded-xl pl-10 pr-4 py-2.5 focus:outline-none focus:border-brand-500 transition-colors"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white font-semibold py-2.5 rounded-xl shadow-lg shadow-brand-500/25 flex items-center justify-center gap-2 transition-all text-sm"
          >
            {loading ? 'Authenticating...' : 'Sign In to Dashboard'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="mt-6 text-center text-xs text-dark-muted">
          Don't have an agency account?{' '}
          <Link to="/register" className="text-brand-500 font-semibold hover:underline">
            Register Agency
          </Link>
        </div>
      </div>
    </div>
  );
}
