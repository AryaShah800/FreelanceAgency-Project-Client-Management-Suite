import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Copy, ExternalLink, FileText, LoaderCircle, RefreshCw, ShieldAlert, X } from 'lucide-react';
import Navbar from '../components/Navbar';
import api from '../services/api';

const money = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

function proposalStatus(proposal) {
  if (proposal.revoked) return 'Revoked';
  if (proposal.signed) return proposal.convertedToProject ? 'Converted' : 'Accepted';
  if (proposal.expiresAt && new Date(proposal.expiresAt).getTime() <= Date.now()) return 'Expired';
  return 'Awaiting response';
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  });
}

function shareUrl(token) {
  return new URL(`/portal/share/${encodeURIComponent(token)}`, window.location.origin).toString();
}

function statusStyle(status) {
  if (status === 'Accepted' || status === 'Converted') return 'bg-forest/10 text-forest border-forest/20';
  if (status === 'Revoked' || status === 'Expired') return 'bg-rust/10 text-rust border-rust/20';
  return 'bg-brass/10 text-brass-dark border-brass/20';
}

export default function Proposals() {
  const [proposals, setProposals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [notice, setNotice] = useState('');

  const loadProposals = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/proposals');
      setProposals(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load proposals. Try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProposals();
  }, [loadProposals]);

  const handleCopy = async (token) => {
    try {
      await navigator.clipboard.writeText(shareUrl(token));
      setNotice('Proposal link copied.');
      window.setTimeout(() => setNotice(''), 2500);
    } catch {
      setNotice('Copy is unavailable in this browser. Open the proposal link to copy it from the address bar.');
    }
  };

  const handleRevoke = async (proposal) => {
    if (proposal.signed || proposal.revoked) return;
    if (!window.confirm(`Revoke the proposal for ${proposal.clientName}? Its public link will stop working.`)) return;
    setBusyId(proposal.id);
    setError('');
    try {
      await api.patch(`/proposals/${proposal.id}/revoke`);
      await loadProposals();
      setNotice('Proposal revoked.');
      window.setTimeout(() => setNotice(''), 2500);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not revoke the proposal.');
    } finally {
      setBusyId(null);
    }
  };

  const signedCount = proposals.filter((proposal) => proposal.signed).length;
  const waitingCount = proposals.filter((proposal) => proposalStatus(proposal) === 'Awaiting response').length;

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar title="Proposals & Agreements" />
      <main className="flex-1 p-6 max-w-6xl w-full mx-auto">
        <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
          <div>
            <h1 className="font-display text-2xl font-bold text-ink">Proposal pipeline</h1>
            <p className="mt-1 text-sm text-ink-muted">Review sent proposals, share links, and revoke proposals before acceptance.</p>
          </div>
          <Link to="/ai-generator" className="inline-flex items-center gap-2 rounded bg-brass px-4 py-2 text-sm font-semibold text-white hover:bg-brass-dark">
            <FileText className="w-4 h-4" /> Write a proposal
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-5">
          <div className="rounded border border-border bg-surface p-4">
            <div className="text-xs uppercase tracking-wider text-ink-muted">Published</div>
            <div className="mt-1 font-display text-2xl font-bold text-ink">{proposals.length}</div>
          </div>
          <div className="rounded border border-border bg-surface p-4">
            <div className="text-xs uppercase tracking-wider text-ink-muted">Awaiting response</div>
            <div className="mt-1 font-display text-2xl font-bold text-brass-dark">{waitingCount}</div>
          </div>
          <div className="rounded border border-border bg-surface p-4">
            <div className="text-xs uppercase tracking-wider text-ink-muted">Accepted</div>
            <div className="mt-1 font-display text-2xl font-bold text-forest">{signedCount}</div>
          </div>
        </div>

        {notice && <div role="status" className="mb-4 rounded border border-forest/20 bg-forest/5 px-4 py-3 text-sm text-forest">{notice}</div>}
        {error && (
          <div role="alert" className="mb-4 flex items-center justify-between gap-3 rounded border border-rust/20 bg-rust/5 px-4 py-3 text-sm text-rust">
            <span>{error}</span>
            <button onClick={loadProposals} className="inline-flex items-center gap-1 font-semibold"><RefreshCw className="w-4 h-4" /> Retry</button>
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center gap-2 py-16 text-sm text-ink-muted"><LoaderCircle className="w-5 h-5 animate-spin" /> Loading proposals…</div>
        ) : proposals.length === 0 ? (
          <div className="rounded border border-dashed border-border bg-surface px-6 py-16 text-center">
            <FileText className="mx-auto h-8 w-8 text-ink-faint" />
            <h2 className="mt-3 font-display text-lg font-semibold text-ink">No proposals yet</h2>
            <p className="mt-1 text-sm text-ink-muted">Published proposals will appear here so your team can follow up.</p>
            <Link to="/ai-generator" className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-brass-dark hover:underline">Write your first proposal <ExternalLink className="h-4 w-4" /></Link>
          </div>
        ) : (
          <div className="space-y-3">
            {proposals.map((proposal) => {
              const status = proposalStatus(proposal);
              return (
                <article key={proposal.id} className="rounded border border-border bg-surface p-4 sm:p-5 shadow-sm">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="font-display text-lg font-semibold text-ink">{proposal.clientName || 'Unnamed client'}</h2>
                        <span className={`rounded-full border px-2.5 py-1 text-[11px] font-semibold ${statusStyle(status)}`}>{status}</span>
                      </div>
                      <p className="mt-1 text-sm text-ink-muted line-clamp-2">{proposal.projectScope || 'No project scope provided'}</p>
                      <div className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-xs text-ink-muted">
                        <span>Budget <strong className="text-ink">{money.format(Number(proposal.estimatedBudget) || 0)}</strong></span>
                        <span>Sent {formatDate(proposal.createdAt)}</span>
                        <span>Expires {formatDate(proposal.expiresAt)}</span>
                        {proposal.signed && <span>Accepted by {proposal.signatureName || 'Client'} on {formatDate(proposal.signedAt)}</span>}
                        {proposal.convertedToProject && proposal.createdProjectId && <span>Project #{proposal.createdProjectId}</span>}
                      </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <a href={shareUrl(proposal.shareToken)} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1.5 rounded border border-border px-3 py-2 text-xs font-semibold text-ink hover:border-brass/60">
                        <ExternalLink className="h-4 w-4" /> Open
                      </a>
                      <button onClick={() => handleCopy(proposal.shareToken)} className="inline-flex items-center gap-1.5 rounded border border-border px-3 py-2 text-xs font-semibold text-ink hover:border-brass/60">
                        <Copy className="h-4 w-4" /> Copy link
                      </button>
                      {proposal.signed && (
                        <a href={`${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/public/portal/${encodeURIComponent(proposal.shareToken)}/acceptance-record.pdf`} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1.5 rounded border border-forest/20 bg-forest/5 px-3 py-2 text-xs font-semibold text-forest hover:bg-forest/10">
                          <Check className="h-4 w-4" /> Signed PDF
                        </a>
                      )}
                      {!proposal.signed && !proposal.revoked && (
                        <button onClick={() => handleRevoke(proposal)} disabled={busyId === proposal.id} className="inline-flex items-center gap-1.5 rounded border border-rust/20 px-3 py-2 text-xs font-semibold text-rust hover:bg-rust/5 disabled:opacity-50">
                          {busyId === proposal.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <X className="h-4 w-4" />} Revoke
                        </button>
                      )}
                      {proposal.signed && <span className="inline-flex items-center gap-1 text-xs font-medium text-forest"><ShieldAlert className="h-4 w-4" /> Acceptance record</span>}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
