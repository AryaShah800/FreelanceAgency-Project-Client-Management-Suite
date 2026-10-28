import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import {
  Sparkles,
  Copy,
  Check,
  Download,
  Save,
  CircleCheck,
  AlertTriangle,
  AlertCircle,
  FileText,
  Printer,
  Layout,
  Plus,
  Loader2,
  Upload,
  Link as LinkIcon
} from 'lucide-react';
import ProposalSectionEditor from '../components/ProposalSectionEditor';
import { formatMarkdownToHtml, sanitizeHtml } from '../utils/formatUtils';
import { calculateCommercials } from '../utils/taxMath';

const defaultAgencySettings = {
  state: 'Maharashtra',
  isGstRegistered: true,
  lutDetails: 'LUT/12345/2026-27',
  legalTemplate: `Intellectual Property: All bespoke source code, designs, and assets created under this agreement will be fully transferred to the client upon receipt of final payment.
Revisions & Acceptance: Each milestone includes up to 2 rounds of minor revisions. Additional structural changes will be scoped via a formal Change Request at standard hourly rates.
Payment Terms: Invoices are due within 7 days of issuance. Late payments may accrue a penalty of 1.5% per month.
Warranty: A 30-day bug-fix warranty period commences upon final production deployment for all delivered features.
Confidentiality: Both parties agree to a mutual Non-Disclosure Agreement (NDA) regarding all proprietary business data and trade secrets.
Governing Law & Jurisdiction: This agreement shall be governed by the laws of India, and the courts of Maharashtra shall have exclusive jurisdiction.
Termination: Either party may terminate this agreement with 15 days written notice.
Limitation of Liability: The agency's liability is capped at the total amount paid under this agreement.`
};

export default function AiGenerator() {
  const { user } = useAuth();

  const [formData, setFormData] = useState({
    clientName: '',
    clientEmail: '',
    clientCountry: 'IN',
    clientState: '',
    clientGstin: '',
    pricingType: 'Fixed Price',
    currency: 'INR',
    startDate: '',
    totalBudget: '',
    projectScope: '',
    deliverables: '',
    timeline: '',
    paymentTerms: '',
    exclusions: {
      hosting: true,
      thirdPartyLicenses: true,
      dataMigration: true,
      training: true,
      postLaunchSupport: false,
    },
  });

  const [formErrors, setFormErrors] = useState({});
  const [apiError, setApiError] = useState('');

  const [blocks, setBlocks] = useState([]);
  const [proposalTitle, setProposalTitle] = useState('Proposal draft');
  const [reviewNotes, setReviewNotes] = useState([]);
  const [activeTab, setActiveTab] = useState('editor'); // 'editor' | 'document'
  const [showSettings, setShowSettings] = useState(false);
  const [agencySettings, setAgencySettings] = useState(defaultAgencySettings);
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [settingsLoaded, setSettingsLoaded] = useState(false);

  const [generating, setGenerating] = useState(false);
  const [copied, setCopied] = useState(false);
  const [saved, setSaved] = useState(false);
  const [paymentMilestones, setPaymentMilestones] = useState([
    { label: 'Project initiation', percentage: '40', daysFromStart: 0 },
    { label: 'Core development', percentage: '30', daysFromStart: 30 },
    { label: 'Final delivery & handover', percentage: '30', daysFromStart: 60 },
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

  useEffect(() => {
    let active = true;
    api.get('/agency/proposal-defaults').then(async ({ data }) => {
      let settings = {
        state: data.state || defaultAgencySettings.state,
        isGstRegistered: data.isGstRegistered ?? defaultAgencySettings.isGstRegistered,
        lutDetails: data.lutDetails ?? defaultAgencySettings.lutDetails,
        legalTemplate: data.legalTemplate || defaultAgencySettings.legalTemplate,
      };
      if (user?.role === 'OWNER' && !data.initialized) {
        const legacy = localStorage.getItem('agency_settings');
        if (legacy) {
          try { settings = { ...settings, ...JSON.parse(legacy) }; } catch { /* use agency defaults */ }
        }
        const saved = await api.patch('/agency/proposal-defaults', settings);
        settings = { ...settings, ...saved.data };
        localStorage.removeItem('agency_settings');
      }
      if (active) { setAgencySettings(settings); setSettingsLoaded(true); }
    }).catch((err) => {
      if (active) {
        setApiError(err.response?.data?.message || 'Could not load agency proposal defaults.');
        setSettingsLoaded(true);
      }
    });
    return () => { active = false; };
  }, [user?.role]);

  const saveAgencySettings = async () => {
    setSettingsSaving(true);
    setApiError('');
    try {
      const { data } = await api.patch('/agency/proposal-defaults', agencySettings);
      setAgencySettings({
        state: data.state, isGstRegistered: data.isGstRegistered,
        lutDetails: data.lutDetails, legalTemplate: data.legalTemplate,
      });
      localStorage.removeItem('agency_settings');
      setShowSettings(false);
    } catch (err) {
      setApiError(err.response?.data?.message || 'Could not save agency proposal defaults.');
    } finally { setSettingsSaving(false); }
  };

  const totalPercentage = paymentMilestones.reduce((sum, milestone) => sum + (Number(milestone.percentage) || 0), 0);

  const validateForm = () => {
    const errors = {};
    if (!formData.clientName.trim()) {
      errors.clientName = 'Client or company name is required.';
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.clientEmail.trim())) {
      errors.clientEmail = 'Enter a valid client contact email.';
    }
    if (!formData.projectScope.trim()) {
      errors.projectScope = 'Project scope is required.';
    } else if (formData.projectScope.trim().length < 30) {
      errors.projectScope = `Project scope must be at least 30 characters (currently ${formData.projectScope.trim().length} chars).`;
    }
    if (totalPercentage !== 100) {
      errors.paymentMilestones = `Milestone percentages must total exactly 100% (currently ${totalPercentage}%).`;
    }
    return errors;
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (formErrors[field]) {
      setFormErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const handleFillSample = () => {
    setFormData((prev) => ({
      ...prev,
      clientName: 'FinTech Innovations Pvt Ltd',
      clientEmail: 'client@example.com',
      clientCountry: 'IN',
      clientState: 'Karnataka',
      clientGstin: '29ABCDE1234F1Z5',
      pricingType: 'Fixed Price',
      currency: 'INR',
      startDate: '2026-11-01',
      totalBudget: '450000',
      projectScope:
        'Custom Spring Boot microservices backend and high-performance React dashboard with real-time WebSocket notifications',
      deliverables:
        '• Production Spring Boot REST APIs\n• Real-time STOMP WebSocket feeds\n• Razorpay payment webhook integration\n• OpenPDF GST invoice generator',
      timeline: '6 weeks across 3 agile milestones',
      paymentTerms:
        '40% advance upon contract signing; 30% midway milestone sign-off; 30% final acceptance',
      exclusions: {
        hosting: true,
        thirdPartyLicenses: true,
        dataMigration: true,
        training: true,
        postLaunchSupport: false,
      },
    }));
    setFormErrors({});
    setApiError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');

    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }
    setFormErrors({});
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
      setBlocks(
        (res.data.sections || []).map((section, index) => ({
          id: `${index}-${section.heading}`,
          title: section.heading,
          content: section.content,
        }))
      );
      setReviewNotes(res.data.reviewNotes || []);
    } catch (err) {
      const validationMap = err.response?.data?.validationErrors;
      const validationMsg = validationMap ? Object.values(validationMap).join(' • ') : null;
      const errorMsg =
        validationMsg || err.response?.data?.message || err.message || 'Unable to generate a proposal draft right now.';
      setApiError(errorMsg);
    } finally {
      setGenerating(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: formData.currency || 'INR',
      minimumFractionDigits: 0
    }).format(amount);
  };

  const generateCommercialsHtml = () => {
    if (!formData.totalBudget) return '<p><i>Commercial details not fully specified.</i></p>';

    const results = calculateCommercials({
      budget: formData.totalBudget,
      clientCountry: formData.clientCountry,
      clientState: formData.clientState,
      agencyState: agencySettings.state,
      isGstRegistered: agencySettings.isGstRegistered,
      startDate: formData.startDate,
      paymentMilestones
    });

    let taxDesc = '';
    if (results.taxType === 'None') {
      taxDesc = 'No Tax Applicable';
    } else if (results.taxType === 'Export') {
      taxDesc = `Export of Services (Zero-rated under ${agencySettings.lutDetails || 'LUT'})`;
    } else {
      taxDesc = `${results.taxType} @${results.gstRate * 100}%`;
    }

    let html = `
      <table style="width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 14px;">
        <tr style="background: #f4ecdc;"><th style="padding: 8px; border: 1px solid #e4dfd3; text-align: left;">Description</th><th style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">Amount</th></tr>
        <tr><td style="padding: 8px; border: 1px solid #e4dfd3;">Total Project Cost (${formData.pricingType})</td><td style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">${formatCurrency(results.baseBudget)}</td></tr>
        ${results.taxType !== 'None' ? `<tr><td style="padding: 8px; border: 1px solid #e4dfd3;">Taxes (${taxDesc})</td><td style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">${formatCurrency(results.gstAmount)}</td></tr>` : ''}
        <tr style="font-weight: bold;"><td style="padding: 8px; border: 1px solid #e4dfd3;">Grand Total</td><td style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">${formatCurrency(results.grandTotal)}</td></tr>
      </table>
      <p style="margin-top: 8px; font-size: 12px; color: #666;"><em>All figures above are ${results.taxType === 'None' ? 'exclusive' : 'inclusive'} of applicable taxes.</em></p>

      <h4 style="margin-top: 24px; font-family: 'Cinzel', Georgia, serif; font-size: 16px;">Payment Schedule</h4>
      <table style="width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 14px;">
        <tr style="background: #f4ecdc;"><th style="padding: 8px; border: 1px solid #e4dfd3; text-align: left;">Milestone</th><th style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">%</th><th style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">Due Date</th><th style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">Invoice Total</th></tr>`;

    results.invoices.forEach(inv => {
      html += `<tr><td style="padding: 8px; border: 1px solid #e4dfd3;">${inv.label}</td><td style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">${inv.percentage}%</td><td style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">${inv.dueDate}</td><td style="padding: 8px; border: 1px solid #e4dfd3; text-align: right;">${formatCurrency(inv.invoiceTotal)}</td></tr>`;
    });
    html += `</table>`;

    if (results.taxType === 'Export') {
      html += `<p style="margin-top: 16px; font-size: 13px; color: #666;">Note: Invoices will be generated in foreign currency via SWIFT/Wire Transfer with no local tax applied under ${agencySettings.lutDetails || 'LUT'}.</p>`;
    } else {
      html += `<p style="margin-top: 16px; font-size: 13px; color: #666;">Note: TDS (Tax Deducted at Source) under section 194J may be applicable. Please issue Form 16A accordingly.</p>`;
    }

    return html;
  };

  const generateLegalTermsHtml = () => {
    // Convert newlines in the setting to bullet points or paragraphs
    const lines = (agencySettings.legalTemplate || '').split('\n').filter(l => l.trim() !== '');
    let listHtml = lines.map(line => `<li style="margin-bottom: 12px;">${line}</li>`).join('');

    // Add computed validity
    const validityDate = new Date();
    validityDate.setDate(validityDate.getDate() + 14);
    const dateStr = validityDate.toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' });

    return `
      <ul style="margin: 0; padding-left: 20px; font-size: 14px; line-height: 1.6;">
        ${listHtml}
        <li><strong>Proposal Validity:</strong> The terms and pricing in this proposal remain valid until ${dateStr}.</li>
      </ul>
    `;
  };

  const handleSave = () => {
    localStorage.setItem('proposal_draft', JSON.stringify({ proposalTitle, blocks, reviewNotes }));
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const buildDocumentHtml = () => {
    const content = blocks
      .map(
        (block, index) =>
          `<div class="section"><h2 class="section-title"><span class="section-num">${String(index + 1).padStart(
            2,
            '0'
          )}</span> ${block.title}</h2><div class="section-body">${formatMarkdownToHtml(block.content)}</div></div>`
      )
      .join('');

    const commercialsHtml = `<div class="section"><h2 class="section-title"><span class="section-num">${String(blocks.length + 1).padStart(2, '0')}</span> Commercial Terms & Payment Schedule</h2><div class="section-body">${generateCommercialsHtml()}</div></div>`;
    const legalHtml = `<div class="section"><h2 class="section-title"><span class="section-num">${String(blocks.length + 2).padStart(2, '0')}</span> Standard Legal Terms</h2><div class="section-body">${generateLegalTermsHtml()}</div></div>`;

    return `<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>${proposalTitle}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
  <style>
    @media print {
      body { margin: 20mm 15mm; }
      .no-print { display: none; }
    }
    body {
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      max-width: 820px;
      margin: 40px auto;
      padding: 0 24px;
      color: #1c1b19;
      background: #faf8f5;
      line-height: 1.7;
    }
    .badge {
      display: inline-block;
      font-family: monospace;
      font-size: 11px;
      letter-spacing: 0.15em;
      color: #8c6508;
      background: #f4ecdc;
      padding: 4px 8px;
      border-radius: 4px;
      font-weight: 700;
    }
    .header {
      border-bottom: 2px solid #e4dfd3;
      padding-bottom: 24px;
      margin-bottom: 32px;
      margin-top: 16px;
    }
    h1 {
      font-family: 'Cinzel', Georgia, serif;
      font-size: 32px;
      margin: 12px 0 6px 0;
      color: #1c1b19;
      font-weight: 700;
    }
    .subtitle {
      color: #6b6862;
      font-size: 14px;
      margin: 0;
    }
    .agency-tag {
      font-size: 12px;
      color: #8c6508;
      font-weight: 600;
      margin-top: 8px;
    }
    .section {
      background: #ffffff;
      border: 1px solid #e4dfd3;
      border-radius: 8px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.03);
    }
    .section-title {
      font-family: 'Cinzel', Georgia, serif;
      font-size: 18px;
      color: #1c1b19;
      margin: 0 0 16px 0;
      display: flex;
      align-items: center;
      gap: 8px;
      border-bottom: 1px solid #f0ece3;
      padding-bottom: 10px;
    }
    .section-num {
      font-family: monospace;
      font-size: 13px;
      color: #8c6508;
      background: #f9f5ed;
      padding: 2px 6px;
      border-radius: 4px;
    }
    .section-body {
      font-size: 14px;
      color: #2b2926;
    }
    ul, ol { padding-left: 24px; margin: 8px 0; }
    li { margin-bottom: 6px; }
    blockquote {
      border-left: 3px solid #b8860b;
      margin: 12px 0;
      padding: 6px 14px;
      background: #faf6ed;
      font-style: italic;
      color: #555;
    }
    .footer {
      text-align: center;
      font-size: 12px;
      color: #8c8983;
      margin-top: 48px;
      border-top: 1px solid #e4dfd3;
      padding-top: 20px;
    }
  </style>
</head>
<body>
  <div class="header">
    <span class="badge">CONFIDENTIAL PROPOSAL DRAFT</span>
    <h1>${proposalTitle}</h1>
    <p class="subtitle">A customized technical scope and commercial specification.</p>
    <div class="agency-tag">Prepared by ${user?.agencyName || 'Your Agency'}</div>
  </div>
  ${content}
  ${commercialsHtml}
  ${legalHtml}
  <div class="footer">
    <p>© ${new Date().getFullYear()} ${user?.agencyName || 'Your Agency'}. All Rights Reserved. Commercial In Confidence.</p>
  </div>
</body>
</html>`;
  };

  const [publishLoading, setPublishLoading] = useState(false);
  const [publishLink, setPublishLink] = useState('');

  const handlePublish = async () => {
    try {
      setPublishLoading(true);
      const documentHtml = sanitizeHtml(buildDocumentHtml());
      // Ensure milestones have proper defaults for processing
      const milestonesWithDefaults = paymentMilestones.map(m => ({
        ...m,
        daysFromStart: parseInt(m.daysFromStart || '30', 10)
      }));
      const results = calculateCommercials({
        budget: formData.totalBudget,
        clientCountry: formData.clientCountry,
        clientState: formData.clientState,
        agencyState: agencySettings.state,
        isGstRegistered: agencySettings.isGstRegistered,
        startDate: formData.startDate,
        paymentMilestones: milestonesWithDefaults
      });
      // Merge results invoice dates back to milestones for saving
      const finalMilestones = milestonesWithDefaults.map((m, i) => ({
        ...m,
        ...results.invoices[i]
      }));

      const res = await api.post('/proposals/publish', {
        clientName: formData.clientName,
        clientEmail: formData.clientEmail.trim(),
        projectScope: formData.projectScope,
        deliverables: formData.deliverables,
        paymentTerms: formData.paymentTerms,
        totalBudget: results.grandTotal,
        documentHtml,
        paymentMilestonesJson: JSON.stringify(finalMilestones)
      });
      setPublishLink(`${window.location.origin}/portal/share/${res.data.shareToken}`);
    } catch (err) {
      alert("Failed to publish proposal: " + (err.response?.data?.error || err.message));
    } finally {
      setPublishLoading(false);
    }
  };

  const handleDownload = () => {
    const documentHtml = sanitizeHtml(buildDocumentHtml());

    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([documentHtml], { type: 'text/html' }));
    link.download = `${
      proposalTitle
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)/g, '') || 'proposal-draft'
    }.html`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const handlePrint = () => {
    window.print();
  };

  const updateMilestone = (index, field, value) => {
    setPaymentMilestones((current) =>
      current.map((milestone, milestoneIndex) =>
        milestoneIndex === index ? { ...milestone, [field]: value } : milestone
      )
    );
    if (formErrors.paymentMilestones) {
      setFormErrors((prev) => {
        const next = { ...prev };
        delete next.paymentMilestones;
        return next;
      });
    }
  };

  const addMilestone = () => {
    setPaymentMilestones((current) => [...current, { label: '', percentage: '', daysFromStart: 30 }]);
  };

  const removeMilestone = (index) => {
    setPaymentMilestones((current) => current.filter((_, i) => i !== index));
  };

  const handleCopy = () => {
    const temp = document.createElement('div');
    const fullText = blocks
      .map((b) => {
        temp.innerHTML = b.content || '';
        return `${b.title}\n${temp.innerText.trim()}`;
      })
      .join('\n\n');
    navigator.clipboard.writeText(fullText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleBlockContentChange = (index, newContent) => {
    setBlocks((current) =>
      current.map((item, itemIndex) => (itemIndex === index ? { ...item, content: newContent } : item))
    );
  };

  const handleAddNewSection = () => {
    const newIndex = blocks.length + 1;
    setBlocks((current) => [
      ...current,
      {
        id: `${newIndex}-custom-section`,
        title: 'Additional Terms & Scope',
        content: '<p>Enter custom stipulations, milestone details, or assumptions for this section.</p>',
      },
    ]);
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar title="Modular AI Proposal & Scoping Builder" />

      <main className="flex-1 p-4 md:p-6 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Rail Input Form */}
        <div className="lg:col-span-4 bg-surface border border-border p-5 rounded-lg shadow-xs space-y-4">
          <div className="flex items-center gap-1.5 text-brass-dark font-mono text-[10px] font-bold tracking-wider">
            <Sparkles className="w-3.5 h-3.5" />
            MODULAR BLOCK-BASED SCOPING ENGINE
          </div>

          <div className="flex items-center justify-between">
            <h3 className="font-display font-semibold text-base text-ink">Contract Parameters</h3>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setShowSettings(!showSettings)}
                className="text-[11px] font-mono text-ink-muted hover:text-ink flex items-center gap-1 cursor-pointer font-medium px-2 py-1 bg-surface border border-border rounded"
              >
                {showSettings ? 'Hide Config' : 'Agency Config'}
              </button>
              <button
                type="button"
                onClick={handleFillSample}
                className="text-[11px] font-mono text-brass-dark hover:underline flex items-center gap-1 cursor-pointer font-medium"
              >
                Fill Sample
              </button>
            </div>
          </div>

          {showSettings && (
            <div className="p-4 bg-paper/60 border border-border rounded-lg space-y-3 text-xs mb-4">
              <div className="flex items-center justify-between pb-2 border-b border-border/50">
                <span className="font-semibold text-ink">Agency Default Profile</span>
                {user?.role === 'OWNER' ? (
                  <button type="button" disabled={settingsSaving || !settingsLoaded} className="text-brass-dark disabled:opacity-50" onClick={saveAgencySettings}>
                    {settingsSaving ? 'Saving…' : 'Save & Close'}
                  </button>
                ) : (
                  <button type="button" className="text-ink-muted" onClick={() => setShowSettings(false)}>Close</button>
                )}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">Base State</label>
                  <input disabled={user?.role !== 'OWNER'}
                    type="text"
                    value={agencySettings.state}
                    onChange={(e) => setAgencySettings(s => ({...s, state: e.target.value}))}
                    className="w-full bg-paper border border-border rounded p-2"
                  />
                </div>
                <div className="flex flex-col justify-end">
                  <label className="flex items-center gap-2 cursor-pointer p-2 border border-border rounded bg-paper">
                    <input disabled={user?.role !== 'OWNER'}
                      type="checkbox"
                      checked={agencySettings.isGstRegistered}
                      onChange={(e) => setAgencySettings(s => ({...s, isGstRegistered: e.target.checked}))}
                      className="accent-brass"
                    />
                    <span className="font-medium text-ink/90">GST Registered</span>
                  </label>
                </div>
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">LUT Ref (For Exports)</label>
                <input disabled={user?.role !== 'OWNER'}
                  type="text"
                  value={agencySettings.lutDetails}
                  onChange={(e) => setAgencySettings(s => ({...s, lutDetails: e.target.value}))}
                  className="w-full bg-paper border border-border rounded p-2"
                />
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Legal Clauses Template (One-time Setup)</label>
                <textarea disabled={user?.role !== 'OWNER'}
                  value={agencySettings.legalTemplate}
                  onChange={(e) => setAgencySettings(s => ({...s, legalTemplate: e.target.value}))}
                  rows={6}
                  className="w-full bg-paper border border-border rounded p-2 font-mono text-[10px]"
                />
              </div>
            </div>
          )}

          {/* API Error Notification */}
          {apiError && (
            <div className="p-3 bg-rust-soft/30 border border-rust/40 rounded-lg flex items-start justify-between gap-2 text-rust text-xs font-medium">
              <div className="flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{apiError}</span>
              </div>
              <button
                type="button"
                onClick={() => setApiError('')}
                className="text-rust/70 hover:text-rust font-bold text-sm leading-none cursor-pointer"
              >
                ×
              </button>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-4 text-xs">
            {/* Client contact inputs */}
            <div>
              <label className="block text-ink-muted font-semibold mb-1">Client Name *</label>
              <input
                type="text"
                placeholder="e.g. Acme Corporation"
                value={formData.clientName}
                onChange={(e) => handleInputChange('clientName', e.target.value)}
                className={`w-full bg-paper border ${
                  formErrors.clientName
                    ? 'border-rust ring-1 ring-rust/30 bg-rust-soft/10'
                    : 'border-border focus:border-brass'
                } text-ink rounded p-2.5 focus:outline-none transition-all`}
              />
              {formErrors.clientName && (
                <div className="flex items-center gap-1.5 text-rust text-[11px] font-medium mt-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.clientName}</span>
                </div>
              )}
            </div>
            <div>
              <label className="block text-ink-muted font-semibold mb-1">Client Contact Email *</label>
              <input
                type="email"
                autoComplete="email"
                maxLength={254}
                placeholder="e.g. contact@acme.com"
                value={formData.clientEmail}
                onChange={(e) => handleInputChange('clientEmail', e.target.value)}
                className={`w-full bg-paper border ${formErrors.clientEmail ? 'border-rust ring-1 ring-rust/30' : 'border-border focus:border-brass'} text-ink rounded p-2.5 focus:outline-none transition-all`}
              />
              {formErrors.clientEmail && <p className="mt-1 text-[11px] text-rust">{formErrors.clientEmail}</p>}
              <p className="mt-1 text-[10px] text-ink-muted">The signer must use this address. A typed signature does not verify identity.</p>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Country</label>
                <select
                  value={formData.clientCountry}
                  onChange={(e) => handleInputChange('clientCountry', e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                >
                  <option value="IN">India (IN)</option>
                  <option value="US">United States (US)</option>
                  <option value="UK">United Kingdom (UK)</option>
                  <option value="AE">United Arab Emirates (AE)</option>
                  <option value="SG">Singapore (SG)</option>
                  <option value="AU">Australia (AU)</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">State</label>
                <input
                  type="text"
                  placeholder="e.g. Maharashtra"
                  value={formData.clientState}
                  onChange={(e) => handleInputChange('clientState', e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                  disabled={formData.clientCountry !== 'IN'}
                />
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">GSTIN / VAT ID</label>
                <input
                  type="text"
                  placeholder="e.g. 29ABCDE1234F1Z5"
                  value={formData.clientGstin}
                  onChange={(e) => handleInputChange('clientGstin', e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                />
              </div>
            </div>

            {/* Project Scope Textarea */}
            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="block text-ink-muted font-semibold">Project Scope *</label>
                <span
                  className={`text-[10px] font-mono ${
                    formData.projectScope.length < 30 ? 'text-rust font-semibold' : 'text-forest'
                  }`}
                >
                  {formData.projectScope.length < 30
                    ? `${formData.projectScope.length}/30 min chars`
                    : `${formData.projectScope.length} chars`}
                </span>
              </div>
              <textarea
                rows="3"
                placeholder="Describe project objectives and core technical scope (minimum 30 characters)..."
                value={formData.projectScope}
                onChange={(e) => handleInputChange('projectScope', e.target.value)}
                className={`w-full bg-paper border ${
                  formErrors.projectScope
                    ? 'border-rust ring-1 ring-rust/30 bg-rust-soft/10'
                    : 'border-border focus:border-brass'
                } text-ink rounded p-2.5 focus:outline-none transition-all`}
              />
              {formErrors.projectScope && (
                <div className="flex items-center gap-1.5 text-rust text-[11px] font-medium mt-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.projectScope}</span>
                </div>
              )}
            </div>

            {/* Deliverables */}
            <div>
              <label className="block text-ink-muted font-semibold mb-1">Deliverables</label>
              <textarea
                rows="3"
                placeholder="• Deliverable 1&#10;• Deliverable 2"
                value={formData.deliverables}
                onChange={(e) => handleInputChange('deliverables', e.target.value)}
                className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
              />
            </div>

            {/* Commercial Configuration */}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Pricing Model</label>
                <select
                  value={formData.pricingType}
                  onChange={(e) => handleInputChange('pricingType', e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                >
                  <option value="Fixed Price">Fixed Price</option>
                  <option value="Time & Material">Time & Material</option>
                  <option value="Retainer">Retainer</option>
                </select>
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Currency</label>
                <select
                  value={formData.currency}
                  onChange={(e) => handleInputChange('currency', e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                >
                  <option value="INR">INR (₹)</option>
                  <option value="USD">USD ($)</option>
                  <option value="EUR">EUR (€)</option>
                  <option value="GBP">GBP (£)</option>
                  <option value="AED">AED</option>
                </select>
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Total Budget (Excl. Tax)</label>
                <input
                  type="number"
                  placeholder="e.g. 500000"
                  value={formData.totalBudget}
                  onChange={(e) => handleInputChange('totalBudget', e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                />
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Est. Start Date</label>
                <input
                  type="date"
                  value={formData.startDate}
                  onChange={(e) => handleInputChange('startDate', e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                />
              </div>
            </div>

            {/* Payment Schedule */}
            <div className="border-t border-border pt-4">
              <div className="flex items-center justify-between mb-2">
                <label className="block text-ink-muted font-semibold">Payment Milestones</label>
                <span
                  className={totalPercentage === 100 ? 'text-forest font-semibold' : 'text-rust font-semibold'}
                >
                  {totalPercentage}% total
                </span>
              </div>
              <div className="space-y-2">
                {paymentMilestones.map((milestone, index) => (
                  <div key={index} className="grid grid-cols-[1fr_58px_20px] gap-2 items-center">
                    <input
                      type="text"
                      value={milestone.label}
                      onChange={(e) => updateMilestone(index, 'label', e.target.value)}
                      placeholder="Milestone name"
                      className="min-w-0 bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                    />
                    <div className="relative" style={{gridColumn: 'span 2'}}>
                      <input
                        type="number"
                        min="0"
                        max="100"
                        value={milestone.percentage}
                        onChange={(e) => updateMilestone(index, 'percentage', e.target.value)}
                        className="w-full bg-paper border border-border text-ink rounded p-2.5 pr-4 focus:outline-none focus:border-brass"
                      />
                      <span className="absolute right-1.5 top-2.5 text-ink-faint text-[10px]">%</span>
                    </div>
                    <div className="relative" style={{gridColumn: 'span 3'}}>
                      <input
                        type="number"
                        min="0"
                        value={milestone.daysFromStart}
                        onChange={(e) => updateMilestone(index, 'daysFromStart', e.target.value)}
                        className="w-full bg-paper border border-border text-ink rounded p-2.5 pr-10 focus:outline-none focus:border-brass"
                      />
                      <span className="absolute right-1.5 top-2.5 text-ink-faint text-[10px]">Days</span>
                    </div>
                    {paymentMilestones.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeMilestone(index)}
                        className="text-rust hover:text-rust-dark font-bold text-xs cursor-pointer ml-1"
                        title="Remove milestone"
                      >
                        ×
                      </button>
                    )}
                  </div>
                ))}
              </div>
              {formErrors.paymentMilestones && (
                <div className="flex items-center gap-1.5 text-rust text-[11px] font-medium mt-1.5">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.paymentMilestones}</span>
                </div>
              )}
              <button
                type="button"
                onClick={addMilestone}
                className="mt-2 text-brass-dark font-semibold text-[11px] hover:underline cursor-pointer"
              >
                + Add milestone
              </button>
            </div>

            {/* Exclusions Checklist */}
            <div className="border-t border-border pt-4">
              <label className="block text-ink-muted font-semibold mb-2">Explicitly Exclude (Out of Scope)</label>
              <div className="grid grid-cols-2 gap-2 text-[11px] font-medium text-ink/80">
                {Object.entries(formData.exclusions || {}).map(([key, value]) => (
                  <label key={key} className="flex items-center gap-2 cursor-pointer p-2 border border-border rounded-md hover:bg-paper transition-colors">
                    <input
                      type="checkbox"
                      checked={!!value}
                      onChange={(e) => {
                        setFormData((prev) => ({
                          ...prev,
                          exclusions: { ...(prev.exclusions || {}), [key]: e.target.checked },
                        }));
                      }}
                      className="accent-brass"
                    />
                    <span>{key.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase())}</span>
                  </label>
                ))}
              </div>
            </div>

            {/* Submit Button with Active Loading Feedback */}
            <button
              type="submit"
              disabled={generating}
              className={`w-full font-semibold py-2.5 rounded-full shadow-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
                generating
                  ? 'bg-brass-dark text-white opacity-90 cursor-not-allowed'
                  : 'bg-brass hover:bg-brass-dark text-white'
              }`}
            >
              {generating ? (
                <>
                  <Loader2 className="w-4 h-4 text-white animate-spin" />
                  <span>Synthesizing Proposal with AI...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Generate Proposal Draft</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Proposal Document & Editor Section */}
        <div className="lg:col-span-8 flex flex-col items-center gap-4">
          <div className="w-full max-w-3xl bg-surface border border-border rounded-lg shadow-sm p-6 md:p-8 relative space-y-6">
            {/* Document Header & Global Actions */}
            <div className="flex flex-col gap-4 border-b border-border pb-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                {/* View Switcher: Interactive Blocks vs Full Document */}
                <div className="flex items-center bg-paper border border-border rounded p-0.5 text-xs">
                  <button
                    type="button"
                    onClick={() => setActiveTab('editor')}
                    className={`px-3 py-1 rounded flex items-center gap-1.5 font-medium transition-colors cursor-pointer ${
                      activeTab === 'editor'
                        ? 'bg-surface text-ink shadow-2xs font-semibold'
                        : 'text-ink-muted hover:text-ink'
                    }`}
                  >
                    <Layout className="w-3.5 h-3.5" />
                    <span>Interactive Sections ({blocks.length})</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveTab('document')}
                    className={`px-3 py-1 rounded flex items-center gap-1.5 font-medium transition-colors cursor-pointer ${
                      activeTab === 'document'
                        ? 'bg-surface text-ink shadow-2xs font-semibold'
                        : 'text-ink-muted hover:text-ink'
                    }`}
                  >
                    <FileText className="w-3.5 h-3.5" />
                    <span>Executive Document View</span>
                  </button>
                </div>

                {/* Document Actions */}
                <div className="flex flex-wrap items-center gap-2">
                  <button
                    onClick={handleSave}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded bg-brass text-white text-xs font-semibold hover:bg-brass-dark transition-colors cursor-pointer"
                  >
                    <Save className="w-3.5 h-3.5" />
                    {saved ? 'Saved' : 'Save draft'}
                  </button>

                  <button
                    onClick={handleCopy}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded bg-paper border border-border text-xs text-brass-dark font-semibold hover:bg-ink/5 transition-colors cursor-pointer"
                  >
                    {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                    {copied ? 'Copied' : 'Copy text'}
                  </button>

                  {publishLink ? (
                    <div className="flex items-center gap-1">
                      <input
                        type="text"
                        readOnly
                        value={publishLink}
                        className="bg-paper border border-border text-ink text-xs px-2 py-1 rounded w-48 outline-none"
                      />
                      <button
                        onClick={() => { navigator.clipboard.writeText(publishLink); alert('Copied!'); }}
                        className="px-2 py-1 bg-brass text-white text-xs rounded hover:bg-brass-dark"
                      >
                        Copy
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={handlePublish}
                      disabled={publishLoading || !blocks.length}
                      className="flex items-center gap-1 px-2.5 py-1.5 rounded bg-brass text-white text-xs font-semibold hover:bg-brass-dark disabled:opacity-40 transition-colors cursor-pointer"
                      title="Publish and get shareable link"
                    >
                      {publishLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
                      Publish
                    </button>
                  )}

                  <button
                    onClick={handleDownload}
                    disabled={!blocks.length}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded bg-paper border border-border text-xs text-brass-dark font-semibold hover:bg-ink/5 disabled:opacity-40 transition-colors cursor-pointer"
                    title="Export formatted HTML document"
                  >
                    <Download className="w-3.5 h-3.5" />
                    Download
                  </button>

                  <button
                    onClick={handlePrint}
                    disabled={!blocks.length}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded bg-paper border border-border text-xs text-brass-dark font-semibold hover:bg-ink/5 disabled:opacity-40 transition-colors cursor-pointer"
                    title="Print or Save as PDF"
                  >
                    <Printer className="w-3.5 h-3.5" />
                    Print / PDF
                  </button>
                </div>
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-[10px] tracking-widest text-brass-dark font-bold">
                    PROPOSAL / SCOPE OF WORK
                  </span>
                  <span className="text-[10px] text-ink-faint">• {user?.agencyName || 'Your Agency'}</span>
                </div>
                <input
                  type="text"
                  value={proposalTitle}
                  onChange={(e) => setProposalTitle(e.target.value)}
                  className="font-display font-bold text-2xl md:text-3xl text-ink bg-transparent border-b border-transparent hover:border-border focus:border-brass focus:outline-none w-full mt-1.5 pb-0.5"
                  placeholder="Enter proposal title..."
                />
                <p className="text-xs text-ink-muted mt-1">A structured working proposal ready for human review and client approval.</p>
              </div>
            </div>

            {/* High-Fidelity Loading State with Shimmer Skeleton */}
            {generating && (
              <div className="space-y-5 py-4">
                <div className="bg-brass-soft/30 border border-brass/40 rounded-lg p-5 flex items-center gap-3.5 shadow-xs">
                  <div className="w-9 h-9 rounded-full bg-brass/20 flex items-center justify-center shrink-0">
                    <Sparkles className="w-5 h-5 text-brass-dark animate-spin" />
                  </div>
                  <div>
                    <h4 className="font-display font-bold text-sm text-ink flex items-center gap-2">
                      <span>Gemini AI Engine is Drafting Proposal</span>
                      <span className="flex h-2 w-2 relative">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-brass opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-brass-dark"></span>
                      </span>
                    </h4>
                    <p className="text-xs text-ink-muted mt-0.5">
                      Synthesizing requirements, structured deliverables, payment milestones, and statutory GST stipulations...
                    </p>
                  </div>
                </div>

                <div className="space-y-4">
                  {[
                    { title: 'Objectives and scope' },
                    { title: 'Deliverables' },
                    { title: 'Timeline and milestones' },
                    { title: 'Commercial terms' },
                  ].map((item, i) => (
                    <div key={i} className="border border-border rounded-lg p-4 bg-paper/40 space-y-2.5 animate-pulse">
                      <div className="flex items-center gap-2 border-b border-border/50 pb-2">
                        <span className="font-mono text-xs text-brass-dark">0{i + 1}</span>
                        <span className="font-display font-semibold text-xs text-ink/70">{item.title}</span>
                      </div>
                      <div className="space-y-2 pt-1">
                        <div className="h-3 bg-border/70 rounded w-full" />
                        <div className="h-3 bg-border/60 rounded w-11/12" />
                        <div className="h-3 bg-border/40 rounded w-3/4" />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 1: Modular Interactive Section Editors */}
            {!generating && activeTab === 'editor' && (
              <div className="space-y-5">
                {blocks.map((block, idx) => (
                  <ProposalSectionEditor
                    key={block.id || idx}
                    block={block}
                    index={idx}
                    onChange={(newContent) => handleBlockContentChange(idx, newContent)}
                  />
                ))}

                {blocks.length > 0 && (
                  <>
                    <button
                      type="button"
                      onClick={handleAddNewSection}
                      className="w-full py-2.5 border border-dashed border-border hover:border-brass/70 rounded-lg text-xs font-semibold text-brass-dark hover:bg-brass-soft/20 flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>Add Custom Section Block</span>
                    </button>

                    {/* Computed Commercial Terms Block (Read Only) */}
                    <div className="bg-paper border border-border rounded-lg p-5 shadow-xs">
                      <div className="flex items-center justify-between mb-4 pb-2 border-b border-border/50">
                        <div className="flex items-center gap-2">
                          <span className="w-6 h-6 rounded-md bg-brass/20 text-brass-dark font-mono text-[10px] font-bold flex items-center justify-center shrink-0">
                            {String(blocks.length + 1).padStart(2, '0')}
                          </span>
                          <h3 className="font-display font-bold text-sm text-ink">Commercial Terms & Payment Schedule</h3>
                        </div>
                        <span className="text-[10px] font-mono text-ink-muted bg-ink/5 px-2 py-0.5 rounded">AUTO-COMPUTED</span>
                      </div>
                      <div
                        className="text-sm text-ink/90 leading-relaxed wysiwyg-editor"
                        dangerouslySetInnerHTML={{ __html: sanitizeHtml(generateCommercialsHtml()) }}
                      />
                    </div>

                    {/* Standard Legal Terms Block (Read Only) */}
                    <div className="bg-paper border border-border rounded-lg p-5 shadow-xs">
                      <div className="flex items-center justify-between mb-4 pb-2 border-b border-border/50">
                        <div className="flex items-center gap-2">
                          <span className="w-6 h-6 rounded-md bg-brass/20 text-brass-dark font-mono text-[10px] font-bold flex items-center justify-center shrink-0">
                            {String(blocks.length + 2).padStart(2, '0')}
                          </span>
                          <h3 className="font-display font-bold text-sm text-ink">Standard Legal Terms</h3>
                        </div>
                        <span className="text-[10px] font-mono text-ink-muted bg-ink/5 px-2 py-0.5 rounded">TEMPLATE</span>
                      </div>
                      <div
                        className="text-sm text-ink/90 leading-relaxed wysiwyg-editor"
                        dangerouslySetInnerHTML={{ __html: sanitizeHtml(generateLegalTermsHtml()) }}
                      />
                    </div>
                  </>
                )}

                {!blocks.length && (
                  <div className="p-10 rounded-lg bg-paper border border-dashed border-border text-center text-sm text-ink-muted space-y-2">
                    <p className="font-semibold text-ink">No proposal sections generated yet.</p>
                    <p className="text-xs">
                      Fill out the parameters on the left and click <strong>Generate Proposal Draft</strong> to create an editable scope.
                    </p>
                  </div>
                )}
              </div>
            )}

            {/* TAB 2: Full Document Formatted View */}
            {!generating && activeTab === 'document' && (
              <div className="bg-paper/40 border border-border rounded-lg p-6 md:p-8 space-y-8 font-sans">
                <div className="border-b border-border pb-4">
                  <span className="font-mono text-[10px] uppercase tracking-wider text-brass-dark font-bold">
                    Official Document Preview
                  </span>
                  <h2 className="font-display font-bold text-2xl text-ink mt-1">{proposalTitle}</h2>
                  <p className="text-xs text-ink-muted">Prepared by {user?.agencyName || 'Your Agency'} for {formData.clientName || 'Client'}</p>
                </div>

                <div className="space-y-6">
                  {blocks.map((block, idx) => (
                    <div key={block.id || idx} className="space-y-2">
                      <h3 className="font-display font-bold text-base text-ink flex items-center gap-2 border-b border-border/50 pb-1">
                        <span className="font-mono text-xs text-brass-dark">{String(idx + 1).padStart(2, '0')}</span>
                        {block.title}
                      </h3>
                      <div
                        className="text-sm text-ink/90 leading-relaxed wysiwyg-editor"
                        dangerouslySetInnerHTML={{ __html: sanitizeHtml(formatMarkdownToHtml(block.content)) }}
                      />
                    </div>
                  ))}

                  {blocks.length > 0 && (
                    <>
                      <div className="space-y-2">
                        <h3 className="font-display font-bold text-base text-ink flex items-center gap-2 border-b border-border/50 pb-1">
                          <span className="font-mono text-xs text-brass-dark">{String(blocks.length + 1).padStart(2, '0')}</span>
                          Commercial Terms & Payment Schedule
                        </h3>
                        <div
                          className="text-sm text-ink/90 leading-relaxed wysiwyg-editor"
                          dangerouslySetInnerHTML={{ __html: sanitizeHtml(generateCommercialsHtml()) }}
                        />
                      </div>
                      <div className="space-y-2">
                        <h3 className="font-display font-bold text-base text-ink flex items-center gap-2 border-b border-border/50 pb-1">
                          <span className="font-mono text-xs text-brass-dark">{String(blocks.length + 2).padStart(2, '0')}</span>
                          Standard Legal Terms
                        </h3>
                        <div
                          className="text-sm text-ink/90 leading-relaxed wysiwyg-editor"
                          dangerouslySetInnerHTML={{ __html: sanitizeHtml(generateLegalTermsHtml()) }}
                        />
                      </div>
                    </>
                  )}
                  {!blocks.length && (
                    <p className="text-xs text-ink-faint italic">No sections to display in document preview.</p>
                  )}
                </div>
              </div>
            )}

            <div className="pt-4 border-t border-border flex flex-wrap items-center justify-between text-[11px] text-ink-muted">
              <span>Full WYSIWYG rich text editor with real bold, italic, headings, lists, undo/redo, and revert.</span>
              <span className="font-mono text-[10px] text-brass-dark">Auto-saves to browser storage</span>
            </div>
          </div>

          {/* Internal Review Checklist */}
          <div className="w-full max-w-3xl bg-surface border border-border rounded-lg p-5 flex flex-col md:flex-row md:items-start gap-5 shadow-xs">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <AlertTriangle className="w-4 h-4 text-brass-dark" />
                <h3 className="font-display font-bold text-base text-ink">Proposal Readiness Checklist</h3>
              </div>
              <p className="text-xs text-ink-muted">
                Internal checks and compliance items to confirm prior to dispatching to client.
              </p>
            </div>
            <ul className="flex-1 space-y-2 text-xs text-ink-muted">
              {(reviewNotes.length
                ? reviewNotes
                : [
                    'Confirm scope, timeline, milestones, and deliverables.',
                    'Verify statutory GST rates and state Place of Supply code.',
                    'Ensure payment milestone percentages total exactly 100%.',
                  ]
              ).map((note, index) => (
                <li key={index} className="flex gap-2 items-start">
                  <CircleCheck className="w-3.5 h-3.5 text-forest shrink-0 mt-0.5" />
                  <span>{note}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </main>
    </div>
  );
}
