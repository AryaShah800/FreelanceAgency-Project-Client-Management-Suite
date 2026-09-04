import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Sparkles, Copy, Check, RefreshCw, Download, Save, CircleCheck, AlertTriangle } from 'lucide-react';

export default function AiGenerator() {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';

  const [formData, setFormData] = useState({
    clientName: '',
    projectScope: '',
    deliverables: '',
    timeline: '',
    budgetGuidance: '',
    paymentTerms: '',
  });

  const [blocks, setBlocks] = useState([]);
  const [proposalTitle, setProposalTitle] = useState('Proposal draft');
  const [reviewNotes, setReviewNotes] = useState([]);

  const [generating, setGenerating] = useState(false);
  const [copied, setCopied] = useState(false);
  const [saved, setSaved] = useState(false);
  const [paymentMilestones, setPaymentMilestones] = useState([
    { label: 'Project initiation', percentage: '40' },
    { label: 'Core development', percentage: '30' },
    { label: 'Final delivery & handover', percentage: '30' },
  ]);

  useEffect(() => {
    const draft = localStorage.getItem('proposal_draft');
    if (!draft) return;
    try {
      const parsed = JSON.parse(draft);
      setProposalTitle(parsed.proposalTitle || 'Proposal draft');
      setBlocks(parsed.blocks || []);
      setReviewNotes(parsed.reviewNotes || []);
    } catch {
      localStorage.removeItem('proposal_draft');
    }
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGenerating(true);
    try {
      const paymentTerms = paymentMilestones
        .filter((milestone) => milestone.label.trim())
        .map((milestone) => `${milestone.percentage}% - ${milestone.label}`)
        .join('; ');
      const res = await api.post('/ai/ai-generator', { ...formData, paymentTerms });
      if (!Array.isArray(res.data.sections) || !res.data.sections.length) {
        throw new Error('The backend is running an older AI endpoint. Restart the Spring Boot backend, then try again.');
      }
      setProposalTitle(res.data.title || 'Proposal draft');
      setBlocks((res.data.sections || []).map((section, index) => ({
        id: `${index}-${section.heading}`,
        title: section.heading,
        content: section.content,
      })));
      setReviewNotes(res.data.reviewNotes || []);
    } catch (err) {
      alert(err.response?.data?.message || 'Unable to generate a proposal draft right now.');
    } finally {
      setGenerating(false);
    }
  };

  const handleSave = () => {
    localStorage.setItem('proposal_draft', JSON.stringify({ proposalTitle, blocks, reviewNotes }));
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const handleDownload = () => {
    const content = blocks.map((block, index) => `<h2>${String(index + 1).padStart(2, '0')} ${block.title}</h2><p>${block.content.replace(/\n/g, '<br />')}</p>`).join('');
    const documentHtml = `<!doctype html><html><head><meta charset="utf-8"><title>${proposalTitle}</title><style>body{font-family:Georgia,serif;max-width:760px;margin:60px auto;color:#1c1b19;line-height:1.65}h1{font-size:34px;margin-bottom:4px}h2{font-size:18px;border-top:1px solid #e4dfd3;padding-top:24px;margin-top:34px}p{font-family:Arial,sans-serif;white-space:normal}</style></head><body><small>PROPOSAL DRAFT</small><h1>${proposalTitle}</h1><p>Prepared by Apex Digital Solutions</p>${content}</body></html>`;
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([documentHtml], { type: 'text/html' }));
    link.download = `${proposalTitle.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '') || 'proposal-draft'}.html`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const updateMilestone = (index, field, value) => {
    setPaymentMilestones((current) => current.map((milestone, milestoneIndex) => milestoneIndex === index ? { ...milestone, [field]: value } : milestone));
  };

  const addMilestone = () => setPaymentMilestones((current) => [...current, { label: '', percentage: '' }]);

  const totalPercentage = paymentMilestones.reduce((sum, milestone) => sum + (Number(milestone.percentage) || 0), 0);

  const handleCopy = () => {
    const fullText = blocks.map((b) => `${b.title}\n${b.content}`).join('\n\n');
    navigator.clipboard.writeText(fullText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar title="Modular AI Proposal & Scoping Builder" />

      <main className="flex-1 p-6 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Rail Input Form */}
        <div className="lg:col-span-4 bg-surface border border-border p-5 rounded shadow-sm space-y-4">
          <div className="flex items-center gap-1.5 text-brass-dark font-mono text-[10px] font-bold tracking-wider">
            <Sparkles className="w-3.5 h-3.5" />
            MODULAR BLOCK-BASED SCOPING ENGINE
          </div>
          <h3 className="font-display font-semibold text-base text-ink">Contract Parameters</h3>

          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            <div>
              <label className="block text-ink-muted font-semibold mb-1">Client Name *</label>
              <input
                type="text"
                required
                value={formData.clientName}
                onChange={(e) => setFormData({ ...formData, clientName: e.target.value })}
                className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
              />
            </div>

            <div>
              <label className="block text-ink-muted font-semibold mb-1">Project Scope *</label>
              <textarea
                rows="3"
                required
                minLength="30"
                value={formData.projectScope}
                onChange={(e) => setFormData({ ...formData, projectScope: e.target.value })}
                className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
              />
            </div>

            <div>
              <label className="block text-ink-muted font-semibold mb-1">Deliverables</label>
              <textarea
                rows="3"
                value={formData.deliverables}
                onChange={(e) => setFormData({ ...formData, deliverables: e.target.value })}
                className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
              />
            </div>

            <div className="border-t border-border pt-4">
              <div className="flex items-center justify-between mb-2">
                <label className="block text-ink-muted font-semibold">Payment schedule</label>
                <span className={totalPercentage === 100 ? 'text-forest font-semibold' : 'text-rust font-semibold'}>{totalPercentage}% total</span>
              </div>
              <div className="space-y-2">
                {paymentMilestones.map((milestone, index) => (
                  <div key={index} className="grid grid-cols-[1fr_58px] gap-2">
                    <input
                      type="text"
                      value={milestone.label}
                      onChange={(e) => updateMilestone(index, 'label', e.target.value)}
                      placeholder="Milestone name"
                      className="min-w-0 bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                    />
                    <div className="relative">
                      <input
                        type="number"
                        min="0"
                        max="100"
                        value={milestone.percentage}
                        onChange={(e) => updateMilestone(index, 'percentage', e.target.value)}
                        className="w-full bg-paper border border-border text-ink rounded p-2.5 pr-5 focus:outline-none focus:border-brass"
                      />
                      <span className="absolute right-2 top-2.5 text-ink-faint">%</span>
                    </div>
                  </div>
                ))}
              </div>
              <button type="button" onClick={addMilestone} className="mt-2 text-brass-dark font-semibold text-[11px] hover:underline">+ Add milestone</button>
            </div>

            <div>
              <label className="block text-ink-muted font-semibold mb-1">Timeline / Milestones</label>
              <input
                type="text"
                value={formData.timeline}
                onChange={(e) => setFormData({ ...formData, timeline: e.target.value })}
                placeholder="e.g. 8 weeks, with two review milestones"
                className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
              />
            </div>

            <div>
              <label className="block text-ink-muted font-semibold mb-1">Budget Guidance</label>
              <input
                type="text"
                value={formData.budgetGuidance}
                onChange={(e) => setFormData({ ...formData, budgetGuidance: e.target.value })}
                placeholder="e.g. ₹250,000 excluding applicable taxes"
                className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
              />
            </div>

            <button
              type="submit"
              disabled={generating}
              className="w-full bg-brass hover:bg-brass-dark text-white font-semibold py-2.5 rounded-full shadow-sm flex items-center justify-center gap-2 transition-all"
            >
              {generating ? (
                <span className="animate-pulse">Generating Proposal...</span>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  Generate Proposal Draft
                </>
              )}
            </button>
          </form>
        </div>

        {/* Proposal document and internal review rail */}
        <div className="lg:col-span-8 flex flex-col items-center gap-4">
          <div className="w-full max-w-3xl bg-surface border border-border rounded shadow-md p-6 md:p-10 relative space-y-8">
            <div className="flex flex-col gap-5 border-b border-border pb-7">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span className="font-mono text-[10px] tracking-widest text-brass-dark font-bold">PROPOSAL / DRAFT</span>
                <div className="flex flex-wrap gap-2">
                  <button onClick={handleSave} className="flex items-center gap-1.5 px-3 py-1.5 rounded bg-brass text-white text-xs font-semibold hover:bg-brass-dark"><Save className="w-3.5 h-3.5" />{saved ? 'Saved' : 'Save draft'}</button>
                  <button onClick={handleCopy} className="flex items-center gap-1.5 px-3 py-1.5 rounded bg-paper border border-border text-xs text-brass-dark font-semibold hover:bg-ink/5">{copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}{copied ? 'Copied' : 'Copy'}</button>
                  <button onClick={handleDownload} disabled={!blocks.length} className="flex items-center gap-1.5 px-3 py-1.5 rounded bg-paper border border-border text-xs text-brass-dark font-semibold hover:bg-ink/5 disabled:opacity-40"><Download className="w-3.5 h-3.5" />Download</button>
                </div>
              </div>
              <div>
                <p className="font-mono text-[10px] text-ink-muted mb-2">PREPARED BY APEX DIGITAL SOLUTIONS</p>
                <h2 className="font-display font-bold text-3xl leading-tight text-ink">{proposalTitle}</h2>
                <p className="text-sm text-ink-muted mt-2">A working proposal for client review and approval.</p>
              </div>
            </div>

            <div className="space-y-7">
              {blocks.map((block, idx) => (
                <section key={block.id} className="space-y-3">
                  <div className="flex items-center justify-between gap-3">
                    <h3 className="font-display font-bold text-lg text-ink"><span className="font-mono text-[10px] text-brass-dark mr-2">{String(idx + 1).padStart(2, '0')}</span>{block.title}</h3>
                    <button type="button" title="Regenerate section" className="flex items-center gap-1 text-[11px] text-ink-muted hover:text-brass-dark"><RefreshCw className="w-3.5 h-3.5" />Regenerate</button>
                  </div>
                  <textarea
                    rows="6"
                    value={block.content}
                    aria-label={`Edit ${block.title}`}
                    onChange={(e) => setBlocks((current) => current.map((item, itemIndex) => itemIndex === idx ? { ...item, content: e.target.value } : item))}
                    className="w-full min-h-[150px] resize-y bg-paper border border-border text-ink text-sm rounded p-4 font-sans leading-relaxed focus:outline-none focus:border-brass"
                  />
                </section>
              ))}
              {!blocks.length && <div className="p-10 rounded bg-paper border border-dashed border-border text-center text-sm text-ink-muted">Complete the brief and generate a proposal draft. Your editable document will appear here.</div>}
            </div>
            <p className="pt-5 border-t border-border text-[11px] text-ink-muted">Editable working draft. Confirm scope, pricing, tax treatment, and legal language before sending.</p>
          </div>

          <div className="w-full max-w-3xl bg-surface border border-border rounded p-5 flex flex-col md:flex-row md:items-start gap-5">
            <div className="flex-1"><div className="flex items-center gap-2 mb-1"><AlertTriangle className="w-4 h-4 text-brass-dark" /><h3 className="font-display font-bold text-base text-ink">Proposal readiness</h3></div><p className="text-xs text-ink-muted">Internal checks to complete before this draft leaves the agency.</p></div>
            <ul className="flex-1 space-y-2 text-xs text-ink-muted">{(reviewNotes.length ? reviewNotes : ['Confirm scope, timeline, pricing, and tax treatment before sending.']).map((note, index) => <li key={index} className="flex gap-2"><CircleCheck className="w-3.5 h-3.5 text-forest shrink-0 mt-0.5" />{note}</li>)}</ul>
          </div>
        </div>
      </main>

    </div>
  );
}
