import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Building2, Lock, Mail, User, ShieldCheck, ArrowRight } from 'lucide-react';

export default function Register() {
  const [formData, setFormData] = useState({
    agencyName: '',
    gstin: '',
    ownerName: '',
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const { register, loading } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await register(formData);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    }
  };

  return (
    <div className="min-h-screen bg-dark-bg flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-600/20 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none"></div>

      <div className="w-full max-w-lg glass-panel p-8 rounded-2xl border border-dark-border shadow-2xl relative z-10">
        <div className="flex flex-col items-center mb-6">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center text-white mb-2 shadow-xl shadow-brand-500/30">
            <Building2 className="w-6 h-6" />
          </div>
          <h2 className="text-2xl font-bold text-white tracking-tight">Create Agency Account</h2>
          <p className="text-dark-muted text-xs">Spring Boot + React Powered Suite</p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-dark-muted mb-1">Agency Name</label>
              <div className="relative">
                <Building2 className="w-4 h-4 text-dark-muted absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  name="agencyName"
                  required
                  value={formData.agencyName}
                  onChange={handleChange}
                  placeholder="Acme Digital"
                  className="w-full bg-dark-bg/80 border border-dark-border text-white text-xs rounded-xl pl-9 pr-3 py-2.5 focus:outline-none focus:border-brand-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-dark-muted mb-1">GSTIN (Optional)</label>
              <div className="relative">
                <ShieldCheck className="w-4 h-4 text-dark-muted absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  name="gstin"
                  value={formData.gstin}
                  onChange={handleChange}
                  placeholder="27AAAAA0000A1Z5"
                  className="w-full bg-dark-bg/80 border border-dark-border text-white text-xs rounded-xl pl-9 pr-3 py-2.5 focus:outline-none focus:border-brand-500"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-dark-muted mb-1">Owner Full Name</label>
            <div className="relative">
              <User className="w-4 h-4 text-dark-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                name="ownerName"
                required
                value={formData.ownerName}
                onChange={handleChange}
                placeholder="Alex Mercer"
                className="w-full bg-dark-bg/80 border border-dark-border text-white text-xs rounded-xl pl-9 pr-3 py-2.5 focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-dark-muted mb-1">Email Address</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-dark-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                name="email"
                required
                value={formData.email}
                onChange={handleChange}
                placeholder="alex@acme.com"
                className="w-full bg-dark-bg/80 border border-dark-border text-white text-xs rounded-xl pl-9 pr-3 py-2.5 focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-dark-muted mb-1">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-dark-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                name="password"
                required
                value={formData.password}
                onChange={handleChange}
                placeholder="••••••••"
                className="w-full bg-dark-bg/80 border border-dark-border text-white text-xs rounded-xl pl-9 pr-3 py-2.5 focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white font-semibold py-2.5 rounded-xl shadow-lg shadow-brand-500/25 flex items-center justify-center gap-2 transition-all text-sm"
          >
            {loading ? 'Creating Account...' : 'Initialize Agency Suite'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="mt-6 text-center text-xs text-dark-muted">
          Already registered?{' '}
          <Link to="/login" className="text-brand-500 font-semibold hover:underline">
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
}
