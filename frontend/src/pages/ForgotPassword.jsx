import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { trackEvent } from '../services/analytics';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setMessage(''); setError('');
    try { const { data } = await api.post('/auth/forgot-password', { email }); setMessage(data.message); trackEvent('Password reset requested'); }
    catch (err) { setError(err.response?.data?.message || 'Could not send a reset email right now.'); }
    finally { setBusy(false); }
  };
  return <main className="flex min-h-screen items-center justify-center bg-paper p-4"><form onSubmit={submit} className="w-full max-w-md space-y-4 rounded border border-border bg-surface p-8">
    <h1 className="font-display text-2xl font-bold text-ink">Reset your password</h1><p className="text-sm text-ink-muted">We’ll send a one-hour password reset link if this account is eligible.</p>
    {message && <p role="status" className="rounded bg-forest-soft p-3 text-sm text-forest">{message}</p>}{error && <p role="alert" className="rounded bg-rust-soft/30 p-3 text-sm text-rust">{error}</p>}
    <label className="block text-xs font-semibold text-ink-muted">Work email<input type="email" required value={email} onChange={e => setEmail(e.target.value)} className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink"/></label>
    <button disabled={busy} className="w-full rounded bg-forest px-4 py-2.5 text-sm font-semibold text-white">{busy ? 'Sending…' : 'Send reset link'}</button>
    <p className="text-sm text-ink-muted"><Link to="/login" className="font-semibold text-brass-dark">Return to sign in</Link></p>
  </form></main>;
}
