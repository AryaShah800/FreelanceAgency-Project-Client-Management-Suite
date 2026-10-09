import React, { useCallback, useEffect, useState } from 'react';
import { UserPlus, Users, UserX, X } from 'lucide-react';
import api from '../services/api';

export default function TeamSettings() {
  const [team, setTeam] = useState({ members: [], invitations: [] });
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('MEMBER');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const refresh = useCallback(() => api.get('/agency/team').then(({ data }) => setTeam(data)), []);
  useEffect(() => { refresh().catch(err => setError(err.response?.data?.message || 'Could not load agency team.')); }, [refresh]);

  const invite = async (event) => {
    event.preventDefault(); setBusy(true); setError(''); setMessage('');
    try {
      const { data } = await api.post('/agency/team/invitations', { email, role });
      setMessage(data.message); setEmail(''); await refresh();
    } catch (err) { setError(err.response?.data?.message || 'Could not send team invitation.'); }
    finally { setBusy(false); }
  };
  const deactivate = async (member) => {
    if (!window.confirm(`Deactivate ${member.name}? They will lose access immediately.`)) return;
    setError(''); setMessage('');
    try { await api.post(`/agency/team/members/${member.id}/deactivate`); setMessage(`${member.name} was deactivated.`); await refresh(); }
    catch (err) { setError(err.response?.data?.message || 'Could not deactivate this account.'); }
  };
  const revoke = async (invite) => {
    setError(''); setMessage('');
    try { await api.delete(`/agency/team/invitations/${invite.id}`); setMessage(`Invitation for ${invite.email} was revoked.`); await refresh(); }
    catch (err) { setError(err.response?.data?.message || 'Could not revoke invitation.'); }
  };

  return <main className="mx-auto w-full max-w-5xl space-y-6 p-6 md:p-10">
    <header className="flex items-start gap-3 border-b border-border pb-5">
      <div className="rounded-lg bg-brass/10 p-2 text-brass-dark"><Users className="h-5 w-5" /></div>
      <div><h1 className="font-display text-2xl font-bold text-ink">Team administration</h1><p className="mt-1 text-sm text-ink-muted">Invite Admins and Staff, and deactivate accounts when access should end.</p></div>
    </header>
    {message && <p role="status" className="rounded border border-forest/30 bg-forest-soft p-3 text-sm text-forest">{message}</p>}
    {error && <p role="alert" className="rounded border border-rust/30 bg-rust-soft/20 p-3 text-sm text-rust">{error}</p>}

    <form onSubmit={invite} className="grid gap-3 rounded-lg border border-border bg-surface p-5 md:grid-cols-[1fr_180px_auto] md:items-end">
      <label className="space-y-1 text-xs font-semibold text-ink-muted">Team member email<input type="email" required maxLength={254} value={email} onChange={e => setEmail(e.target.value)} className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink" /></label>
      <label className="space-y-1 text-xs font-semibold text-ink-muted">Role<select value={role} onChange={e => setRole(e.target.value)} className="mt-1 w-full rounded border border-border bg-paper p-2.5 text-sm text-ink"><option value="ADMIN">Admin</option><option value="MEMBER">Staff</option></select></label>
      <button disabled={busy} className="inline-flex items-center justify-center gap-2 rounded bg-forest px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60"><UserPlus className="h-4 w-4" />{busy ? 'Sending…' : 'Send invitation'}</button>
      <p className="text-xs text-ink-muted md:col-span-3">Invitations expire after 7 days. Admin can manage clients and proposals. Staff can access assigned projects and tasks.</p>
    </form>

    <section className="overflow-hidden rounded-lg border border-border bg-surface">
      <div className="border-b border-border px-5 py-4"><h2 className="font-semibold text-ink">Members</h2></div>
      <div className="divide-y divide-border">
        {team.members.map(member => <div key={member.id} className="flex items-center justify-between gap-4 px-5 py-4">
          <div className="min-w-0"><p className="truncate text-sm font-semibold text-ink">{member.name}</p><p className="truncate text-xs text-ink-muted">{member.email} · {member.role === 'MEMBER' ? 'Staff' : member.role}</p></div>
          <div className="flex shrink-0 items-center gap-3">
            <span className={`rounded px-2 py-1 text-[10px] font-semibold ${member.enabled ? 'bg-forest-soft text-forest' : 'bg-rust-soft/30 text-rust'}`}>{member.enabled ? 'Active' : 'Deactivated'}</span>
            {member.role !== 'OWNER' && member.enabled && <button onClick={() => deactivate(member)} aria-label={`Deactivate ${member.name}`} className="rounded p-2 text-rust hover:bg-rust-soft/30"><UserX className="h-4 w-4" /></button>}
          </div>
        </div>)}
        {!team.members.length && <p className="px-5 py-8 text-center text-sm text-ink-muted">No team accounts yet.</p>}
      </div>
    </section>

    {team.invitations.length > 0 && <section className="overflow-hidden rounded-lg border border-border bg-surface">
      <div className="border-b border-border px-5 py-4"><h2 className="font-semibold text-ink">Pending invitations</h2></div>
      <div className="divide-y divide-border">{team.invitations.map(invite => <div key={invite.id} className="flex items-center justify-between gap-4 px-5 py-4">
        <div><p className="text-sm font-semibold text-ink">{invite.email}</p><p className="text-xs text-ink-muted">{invite.role === 'MEMBER' ? 'Staff' : invite.role} · expires {new Date(invite.expiresAt).toLocaleString()}</p></div>
        <button onClick={() => revoke(invite)} className="inline-flex items-center gap-1 rounded border border-border px-3 py-2 text-xs text-ink-muted hover:text-rust"><X className="h-3.5 w-3.5" />Revoke</button>
      </div>)}</div>
    </section>}
  </main>;
}
