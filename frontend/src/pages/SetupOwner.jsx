import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { Building2 } from 'lucide-react';
import { trackEvent } from '../services/analytics';

export default function SetupOwner() {
  const [form, setForm] = useState({ agencyName: '', ownerName: '', email: '', password: '', bootstrapSecret: '' });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const onSubmit = async (event) => {
    event.preventDefault(); setBusy(true); setError(''); setMessage('');
    trackEvent('Owner setup submitted');
    try {
      const { data } = await api.post('/auth/bootstrap-owner', form);
      setMessage(data.message || 'Check your email to finish setup.');
      trackEvent('Owner setup completed');
    } catch (err) { setError(err.response?.data?.message || 'First-owner setup failed.'); }
    finally { setBusy(false); }
  };
  const field = (name, label, type = 'text', minLength) => (
    <label className="block space-y-1 text-xs font-semibold text-ink-muted">
      {label}<input required type={type} minLength={minLength} value={form[name]} onChange={e => setForm({ ...form, [name]: e.target.value })}
        className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink" />
    </label>
  );
  return <main className="flex min-h-screen items-center justify-center bg-paper p-4">
    <form onSubmit={onSubmit} className="w-full max-w-md space-y-4 rounded border border-border bg-surface p-8 shadow-sm">
      <div className="text-center"><Building2 className="mx-auto mb-2 h-8 w-8 text-brass-dark"/><h1 className="font-display text-2xl font-bold text-ink">Set up the first owner</h1>
        <p className="mt-1 text-xs text-ink-muted">One-time setup for a new installation. Email verification is required before sign-in.</p></div>
      {error && <div role="alert" className="rounded border border-rust/30 bg-rust-soft/20 p-3 text-xs text-rust">{error}</div>}
      {message && <div role="status" className="rounded border border-forest/30 bg-forest-soft p-3 text-xs text-forest">{message}</div>}
      {field('agencyName', 'Agency name')}{field('ownerName', 'Your name')}{field('email', 'Work email', 'email')}{field('password', 'Password (12 characters minimum)', 'password', 12)}{field('bootstrapSecret', 'One-time setup secret', 'password', 32)}
      <button disabled={busy} className="w-full rounded bg-forest px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{busy ? 'Setting up…' : 'Create owner account'}</button>
      <p className="text-center text-xs text-ink-muted"><Link to="/login" className="font-semibold text-brass-dark">Back to sign in</Link></p>
    </form>
  </main>;
}
