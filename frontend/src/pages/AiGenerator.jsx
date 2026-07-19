import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { Sparkles, Copy, Check, FileText } from 'lucide-react';

export default function AiGenerator() {
  const [formData, setFormData] = useState({
    clientName: 'Apex Innovations',
    projectScope: 'Custom Spring Boot Microservices Backend & React Glassmorphic Dashboard with Real-time WebSockets',
    deliverables: '1. RESTful APIs with JWT Auth\n2. OpenPDF Invoice Streamer\n3. Responsive React Dashboard\n4. Deployment Configuration',
    paymentTerms: '50% advance upon project kickoff, 50% upon final acceptance',
  });
  const [contract, setContract] = useState('');
  const [generating, setGenerating] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGenerating(true);
    try {
      const res = await api.post('/ai/generate-contract', formData);
      setContract(res.data.contract);
    } catch (err) {
      alert('Error generating contract');
    } finally {
      setGenerating(false);
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(contract);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar title="Gemini AI Proposal & Agreement Generator" />

      <main className="flex-1 p-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Input Form */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-border space-y-4">
          <div className="flex items-center gap-2 text-brand-400 font-bold text-xs">
            <Sparkles className="w-4 h-4" />
            SPRING WEBCLIENT + GEMINI AI API
          </div>
          <h3 className="text-lg font-bold text-white">Project Scope & Proposal Parameters</h3>

          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            <div>
              <label className="block text-dark-muted font-semibold mb-1">Client Name *</label>
              <input
                type="text"
                required
                value={formData.clientName}
                onChange={(e) => setFormData({ ...formData, clientName: e.target.value })}
                className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
              />
            </div>

            <div>
              <label className="block text-dark-muted font-semibold mb-1">Project Scope *</label>
              <textarea
                rows="3"
                required
                value={formData.projectScope}
                onChange={(e) => setFormData({ ...formData, projectScope: e.target.value })}
                className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
              />
            </div>

            <div>
              <label className="block text-dark-muted font-semibold mb-1">Deliverables</label>
              <textarea
                rows="3"
                value={formData.deliverables}
                onChange={(e) => setFormData({ ...formData, deliverables: e.target.value })}
                className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
              />
            </div>

            <div>
              <label className="block text-dark-muted font-semibold mb-1">Payment Terms</label>
              <input
                type="text"
                value={formData.paymentTerms}
                onChange={(e) => setFormData({ ...formData, paymentTerms: e.target.value })}
                className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
              />
            </div>

            <button
              type="submit"
              disabled={generating}
              className="w-full bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white font-semibold py-3 rounded-xl shadow-lg shadow-brand-500/25 flex items-center justify-center gap-2 transition-all"
            >
              {generating ? (
                <span>Generating Proposal with Gemini AI...</span>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  Generate Proposal & Contract
                </>
              )}
            </button>
          </form>
        </div>

        {/* Generated Contract Output */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-border flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <FileText className="w-4 h-4 text-brand-400" />
                Generated Agreement Document
              </h3>

              {contract && (
                <button
                  onClick={handleCopy}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-dark-card border border-dark-border text-xs text-brand-400 hover:text-white transition-colors"
                >
                  {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                  {copied ? 'Copied' : 'Copy Text'}
                </button>
              )}
            </div>

            {contract ? (
              <pre className="bg-dark-bg/80 border border-dark-border/60 p-4 rounded-xl text-xs text-slate-300 font-sans whitespace-pre-wrap overflow-y-auto max-h-[500px]">
                {contract}
              </pre>
            ) : (
              <div className="text-center py-24 text-dark-muted text-xs">
                <Sparkles className="w-8 h-8 text-brand-500/40 mx-auto mb-2" />
                Fill out the project details on the left and click "Generate Proposal & Contract" to trigger Spring's WebClient & Gemini API.
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
