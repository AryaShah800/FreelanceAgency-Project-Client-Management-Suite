import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';
import { Sparkles, Building2, Check, PenTool, ShieldCheck, CreditCard, ArrowRight, FileText, CheckCircle2 } from 'lucide-react';

export default function PublicPortal() {
  const { token } = useParams();
  const [proposal, setProposal] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [signerName, setSignerName] = useState('Sarah Jenkins');
  const [signing, setSigning] = useState(false);
  const [converting, setConverting] = useState(false);
  const [conversionResult, setConversionResult] = useState(null);

  useEffect(() => {
    fetchPublicProposal();
    loadRazorpayScript();
  }, [token]);

  const loadRazorpayScript = () => {
    if (!document.getElementById('razorpay-sdk')) {
      const script = document.createElement('script');
      script.id = 'razorpay-sdk';
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      document.body.appendChild(script);
    }
  };

  const fetchPublicProposal = async () => {
    try {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
      const res = await axios.get(`${baseURL}/public/portal/${token}`);
      setProposal(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Public proposal link is invalid or expired.');
    } finally {
      setLoading(false);
    }
  };

  const handleSign = async (e) => {
    e.preventDefault();
    setSigning(true);
    try {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
      const res = await axios.post(`${baseURL}/public/portal/${token}/sign`, { signerName });
      setProposal(res.data);
    } catch (err) {
      alert('Error signing contract proposal');
    } finally {
      setSigning(false);
    }
  };

  const handleConvertAndProvision = async () => {
    setConverting(true);
    try {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
      const res = await axios.post(`${baseURL}/public/portal/${token}/convert`);
      setConversionResult(res.data);
      fetchPublicProposal();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to convert proposal to active project');
    } finally {
      setConverting(false);
    }
  };

  const renderFormattedMarkdown = (text) => {
    if (!text) return null;
    const lines = text.split('\n');
    return lines.map((line, idx) => {
      let trimmed = line.trim();
      if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
        return <li key={idx} className="ml-4 list-disc text-ink my-1">{trimmed.substring(2)}</li>;
      }
      return <p key={idx} className="my-1.5 text-ink leading-relaxed">{line}</p>;
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-paper flex items-center justify-center p-4">
        <div className="text-center font-mono text-sm text-ink-muted animate-pulse">
          Loading Public Client Portal...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-paper flex items-center justify-center p-4">
        <div className="bg-surface border border-rust/40 p-8 rounded shadow-md text-center max-w-md space-y-3">
          <h3 className="font-display font-bold text-lg text-rust">Access Notice</h3>
          <p className="text-xs text-ink-muted">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-paper flex flex-col justify-between p-4 md:p-8">
      {/* Header */}
      <header className="max-w-4xl mx-auto w-full flex items-center justify-between py-4 border-b border-border">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-forest flex items-center justify-center text-white font-bold shadow-xs">
            <Building2 className="w-5 h-5 text-surface" />
          </div>
          <div>
            <h1 className="font-display font-bold text-base text-ink">Client Service Portal</h1>
            <p className="font-mono text-[10px] text-forest font-bold">SECURE PUBLIC SHARE TOKEN ACCESS</p>
          </div>
        </div>

        <span className="font-mono text-xs text-ink-muted bg-surface px-3 py-1 rounded border border-border">
          Client: <strong className="text-ink">{proposal.clientName}</strong>
        </span>
      </header>

      {/* Main Document Content */}
      <main className="max-w-4xl mx-auto w-full my-8 space-y-6">
        {/* Step-by-Step Interactive Workflow Banner */}
        <div className="bg-surface border border-border p-6 rounded shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2 font-mono text-[10px] font-bold text-forest uppercase">
              <Sparkles className="w-3.5 h-3.5" />
              ONE-CLICK PROPOSAL AUTOMATION LOOP
            </div>
            <h2 className="font-display font-bold text-xl text-ink">
              {proposal.isConvertedToProject ? 'Project Provisioned & Active!' : proposal.isSigned ? 'Contract Signed - Ready for One-Click Conversion' : 'Review Proposal & Execute Digital Agreement'}
            </h2>
            <p className="text-xs text-ink-muted">
              {proposal.isConvertedToProject
                ? 'Your project workspace and 50% deposit invoice have been automatically generated.'
                : 'Approve the scope below to automatically provision your active project workspace and 50% deposit invoice.'}
            </p>
          </div>

          <div>
            {!proposal.isConvertedToProject ? (
              proposal.isSigned ? (
                <button
                  onClick={handleConvertAndProvision}
                  disabled={converting}
                  className="bg-forest hover:bg-forest/90 text-white font-bold text-xs px-6 py-3 rounded-full shadow-md flex items-center gap-2 transition-all"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  {converting ? 'Provisioning Project...' : 'Approve Proposal & Create Project'}
                </button>
              ) : (
                <a
                  href="#sign-section"
                  className="bg-brass hover:bg-brass-dark text-white font-bold text-xs px-6 py-3 rounded-full shadow-md inline-flex items-center gap-2 transition-all"
                >
                  <PenTool className="w-4 h-4" />
                  Sign Contract Below
                </a>
              )
            ) : (
              <span className="font-mono text-xs font-bold text-forest bg-forest-soft px-4 py-2 rounded-full border border-forest/30 flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-forest" />
                Active Project #{proposal.createdProjectId}
              </span>
            )}
          </div>
        </div>

        {/* Conversion Result Card */}
        {conversionResult && (
          <div className="bg-forest-soft border border-forest/30 p-6 rounded shadow-sm space-y-2 animate-in fade-in">
            <h3 className="font-display font-bold text-base text-forest flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-forest" />
              {conversionResult.message}
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 font-mono text-xs pt-2">
              <div>
                <span className="text-ink-muted">Project ID:</span> <strong className="text-ink">#{conversionResult.projectId}</strong>
              </div>
              <div>
                <span className="text-ink-muted">Deposit Invoice:</span> <strong className="text-brass-dark">{conversionResult.depositInvoiceNumber}</strong>
              </div>
              <div>
                <span className="text-ink-muted">Deposit Due:</span> <strong className="text-ink">₹{conversionResult.depositTotalAmount?.toLocaleString()}</strong>
              </div>
            </div>
          </div>
        )}

        {/* Document Sheet */}
        <div className="bg-surface border border-border p-8 md:p-12 rounded shadow-md space-y-8">
          <div className="border-b border-border pb-4 flex justify-between items-start">
            <div>
              <h2 className="font-display font-bold text-2xl text-ink">SERVICES AGREEMENT PROPOSAL</h2>
              <p className="font-mono text-[10px] text-ink-muted mt-1">TOKEN: {proposal.shareToken}</p>
            </div>
            <FileText className="w-8 h-8 text-brass" />
          </div>

          <div className="space-y-4">
            <h3 className="font-display font-bold text-base text-ink">1. Project Scope</h3>
            <div className="text-xs leading-relaxed bg-paper p-4 rounded border border-border/80">
              {proposal.projectScope}
            </div>
          </div>

          <div className="space-y-4">
            <h3 className="font-display font-bold text-base text-ink">2. Itemized Deliverables</h3>
            <div className="text-xs leading-relaxed bg-paper p-4 rounded border border-border/80">
              {renderFormattedMarkdown(proposal.deliverables)}
            </div>
          </div>

          <div className="space-y-4">
            <h3 className="font-display font-bold text-base text-ink">3. Financial Terms</h3>
            <div className="text-xs leading-relaxed bg-paper p-4 rounded border border-border/80 font-mono">
              <div>Estimated Total Budget: <strong>₹{proposal.estimatedBudget?.toLocaleString()} + 18% GST</strong></div>
              <div className="mt-1 text-ink-muted">{proposal.paymentTerms}</div>
            </div>
          </div>

          {/* Signatures & Execution Section */}
          <div id="sign-section" className="pt-8 border-t border-border grid grid-cols-1 md:grid-cols-2 gap-8 text-xs font-mono">
            <div className="space-y-3">
              <h4 className="font-bold text-ink">Client Digital Sign-Off</h4>

              {proposal.isSigned ? (
                <div className="p-4 bg-forest-soft border border-forest/30 rounded space-y-1 text-forest text-[11px]">
                  <div className="font-display italic text-lg font-bold text-ink">{proposal.signatureName}</div>
                  <div>Digitally Signed: {new Date(proposal.signedAt).toLocaleString()}</div>
                  <div className="text-ink-faint">Audit Hash: VERIFIED-TOKEN-SIGNATURE</div>
                </div>
              ) : (
                <form onSubmit={handleSign} className="space-y-3 bg-paper p-4 rounded border border-border">
                  <div>
                    <label className="block text-ink-muted text-[11px] font-semibold mb-1">Full Signer Name *</label>
                    <input
                      type="text"
                      required
                      value={signerName}
                      onChange={(e) => setSignerName(e.target.value)}
                      className="w-full bg-surface border border-border text-ink rounded p-2 focus:outline-none font-display font-bold text-sm"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={signing}
                    className="w-full bg-forest hover:bg-forest/90 text-white font-semibold py-2 rounded flex items-center justify-center gap-1.5 text-xs shadow-xs"
                  >
                    <ShieldCheck className="w-4 h-4" />
                    {signing ? 'Signing...' : 'Sign & Accept Contract'}
                  </button>
                </form>
              )}
            </div>

            <div className="space-y-3">
              <h4 className="font-bold text-ink">Agency Authorized Signature</h4>
              <div className="p-4 bg-paper border border-border rounded space-y-1 text-ink text-[11px]">
                <div className="font-display italic text-lg font-bold text-brass-dark">Alex Mercer</div>
                <div>Authorized Agency Representative</div>
                <div>Apex Digital Solutions</div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="max-w-4xl mx-auto w-full text-center text-xs text-ink-muted py-4 border-t border-border">
        Freelance Agency Suite - Secure Token-Based Client Portal
      </footer>
    </div>
  );
}
