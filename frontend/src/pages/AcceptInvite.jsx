import React, { useState } from 'react';
import { trackEvent } from '../services/analytics';
import { Link, useSearchParams } from 'react-router-dom';
import api from '../services/api';

export default function AcceptInvite() {
  const [params] = useSearchParams();
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const submit = async (event) => {
    event.preventDefault(); setError(''); setMessage('');
    if (password !== confirm) { setError('Passwords do not match.'); return; }
    trackEvent('Invitation submitted');
    setBusy(true);
    try { const { data } = await api.post('/auth/invitations/accept', { token: params.get('token'), name, password }); setMessage(data.message); trackEvent('Invitation accepted'); }
    catch (err) { setError(err.response?.data?.message || 'This invitation is invalid, revoked, or expired.'); }
    finally { setBusy(false); }
  };
  return <main className="flex min-h-screen items-center justify-center bg-paper p-4"><form onSubmit={submit} className="w-full max-w-md space-y-4 rounded border border-border bg-surface p-8">
    <h1 className="font-display text-2xl font-bold text-ink">Join the agency team</h1>
    {message && <p role="status" className="rounded bg-forest-soft p-3 text-sm text-forest">{message}</p>}
    {error && <p role="alert" className="rounded bg-rust-soft/30 p-3 text-sm text-rust">{error}</p>}
    <label className="block text-xs font-semibold text-ink-muted">Your name<input required maxLength={150} value={name} onChange={e => setName(e.target.value)} className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink"/></label>
    <label className="block text-xs font-semibold text-ink-muted">Password (12 characters minimum)<input type="password" required minLength={12} maxLength={72} autoComplete="new-password" value={password} onChange={e => setPassword(e.target.value)} className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink"/></label>
    <label className="block text-xs font-semibold text-ink-muted">Confirm password<input type="password" required minLength={12} maxLength={72} autoComplete="new-password" value={confirm} onChange={e => setConfirm(e.target.value)} className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink"/></label>
    <button disabled={busy || !params.get('token')} className="w-full rounded bg-forest px-4 py-2.5 text-sm font-semibold text-white">{busy ? 'Joining…' : 'Accept invitation'}</button>
    <p className="text-sm text-ink-muted"><Link to="/login" className="font-semibold text-brass-dark">Return to sign in</Link></p>
  </form></main>;
}
