import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api from '../services/api';

export default function VerifyEmail() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!token) return;
    api.post('/auth/verify-email', { token }).then(({ data }) => setMessage(data.message))
      .catch(err => setError(err.response?.data?.message || 'This verification link is invalid or expired.'));
  }, [token]);
  const resend = async (event) => {
    event.preventDefault(); setBusy(true); setError(''); setMessage('');
    try { const { data } = await api.post('/auth/resend-verification', { email }); setMessage(data.message); }
    catch (err) { setError(err.response?.data?.message || 'Could not request another verification email.'); }
    finally { setBusy(false); }
  };
  return <main className="flex min-h-screen items-center justify-center bg-paper p-4"><section className="w-full max-w-md space-y-4 rounded border border-border bg-surface p-8">
    <h1 className="font-display text-2xl font-bold text-ink">Email verification</h1>
    {message && <p role="status" className="rounded bg-forest-soft p-3 text-sm text-forest">{message}</p>}
    {error && <p role="alert" className="rounded bg-rust-soft/30 p-3 text-sm text-rust">{error}</p>}
    {!token && <form onSubmit={resend} className="space-y-3"><p className="text-sm text-ink-muted">Request a fresh verification link for your account.</p>
      <label className="block text-xs font-semibold text-ink-muted">Email<input type="email" required value={email} onChange={e => setEmail(e.target.value)} className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink"/></label>
      <button disabled={busy} className="w-full rounded bg-forest px-4 py-2.5 text-sm font-semibold text-white">{busy ? 'Sending…' : 'Send verification email'}</button></form>}
    <p className="text-sm text-ink-muted"><Link to="/login" className="font-semibold text-brass-dark">Return to sign in</Link></p>
  </section></main>;
}
