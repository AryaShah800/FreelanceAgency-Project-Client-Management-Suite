import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Download, Plus, Clock, CreditCard, ShieldCheck } from 'lucide-react';

export default function Invoices() {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const isOwner = user?.role === 'OWNER';

  const [invoices, setInvoices] = useState([]);
  const [projects, setProjects] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [showUnbilledModal, setShowUnbilledModal] = useState(false);
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [payingInvoiceId, setPayingInvoiceId] = useState(null);

  const [newInvoice, setNewInvoice] = useState({
    projectId: '',
    dueDate: '',
    isRecurring: false,
    lineItems: [{ description: 'Web Development Services', quantity: 1, unitPrice: 50000 }],
  });

  useEffect(() => {
    fetchInvoices();
    fetchProjects();
    loadRazorpayScript();
  }, []);

  const loadRazorpayScript = () => {
    if (!document.getElementById('razorpay-sdk')) {
      const script = document.createElement('script');
      script.id = 'razorpay-sdk';
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      document.body.appendChild(script);
    }
  };

  const fetchInvoices = async () => {
    try {
      const res = await api.get('/invoices');
      setInvoices(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchProjects = async () => {
    try {
      const res = await api.get('/projects');
      setProjects(res.data);
      if (res.data.length > 0) {
        setNewInvoice((prev) => ({ ...prev, projectId: res.data[0].id }));
        setSelectedProjectId(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleAddItem = () => {
    setNewInvoice({
      ...newInvoice,
      lineItems: [...newInvoice.lineItems, { description: '', quantity: 1, unitPrice: 0 }],
    });
  };

  const handleItemChange = (index, field, value) => {
    const updated = [...newInvoice.lineItems];
    updated[index][field] = field === 'description' ? value : Number(value);
    setNewInvoice({ ...newInvoice, lineItems: updated });
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await api.post('/invoices', newInvoice);
      setShowModal(false);
      fetchInvoices();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to generate invoice');
    }
  };

  const handleGenerateFromUnbilled = async (e) => {
    e.preventDefault();
    if (!selectedProjectId) return;
    try {
      await api.post(`/invoices/generate-from-time/${selectedProjectId}`);
      setShowUnbilledModal(false);
      fetchInvoices();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to generate invoice from unbilled time. Make sure you have unbilled logged hours!');
    }
  };

  const handleRazorpayPayment = async (invoice) => {
    setPayingInvoiceId(invoice.id);
    try {
      const orderRes = await api.post(`/payments/razorpay/create-order?invoiceId=${invoice.id}`);
      const orderData = orderRes.data;

      const options = {
        key: orderData.keyId,
        amount: orderData.amount,
        currency: orderData.currency,
        name: 'Freelance Agency Suite',
        description: `Payment for Invoice ${invoice.invoiceNumber}`,
        order_id: orderData.orderId,
        handler: async function (response) {
          try {
            await api.post('/payments/razorpay/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
              invoiceId: invoice.id,
            });
            alert('Payment Verified & Completed Successfully!');
            fetchInvoices();
          } catch (verifyErr) {
            alert('Payment signature verification failed.');
          } finally {
            setPayingInvoiceId(null);
          }
        },
        prefill: {
          name: user?.name || invoice.clientName,
          email: user?.email || '',
        },
        theme: {
          color: '#1F4B43',
        },
      };

      if (!window.Razorpay) {
        throw new Error('The secure payment checkout could not be loaded. Please refresh and try again.');
      }
      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', () => setPayingInvoiceId(null));
      rzp.open();
    } catch (err) {
      alert(err.response?.data?.message || err.message || 'Error initiating Razorpay checkout');
      setPayingInvoiceId(null);
    }
  };

  const handleDownloadPdf = async (id) => {
    try {
      const response = await api.get(`/invoices/${id}/pdf`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${id}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      alert(err.response?.data?.message || 'Unable to download this invoice PDF');
    }
  };

  const handleMarkPaid = async (id) => {
    try {
      await api.patch(`/invoices/${id}/status?status=PAID`);
      fetchInvoices();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar title={isClient ? "My Tax Invoices & Online Payments" : "Tax Invoice Ledger & Tax Engine"} />

      <main className="flex-1 p-6 space-y-6">
        {/* Action Controls Header Bar */}
        <div className="flex items-center justify-between">
          <p className="text-xs text-ink-muted">
            {isClient
              ? 'Review pending tax invoices and make secure payments online.'
              : 'Multi-State Tax Engine: Automatic Intra-state (CGST+SGST) vs Inter-state (IGST) calculation.'}
          </p>

          {!isClient && (
            <div className="flex items-center gap-3">
              <button
                onClick={() => setShowUnbilledModal(true)}
                className="bg-forest hover:bg-forest/90 text-white font-semibold text-xs px-4 py-2 rounded-full shadow-sm flex items-center gap-1.5 transition-all"
              >
                <Clock className="w-4 h-4" />
                Generate from Unbilled Time
              </button>
              <button
                onClick={() => setShowModal(true)}
                className="bg-brass hover:bg-brass-dark text-white font-semibold text-xs px-4 py-2 rounded-full shadow-sm flex items-center gap-1.5 transition-all"
              >
                <Plus className="w-4 h-4" />
                New Invoice
              </button>
            </div>
          )}
        </div>

        {/* Invoices Table */}
        <div className="bg-surface border border-border shadow-sm rounded p-6">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-border text-xs font-semibold text-ink-muted">
                  <th className="pb-3 px-2 font-sans">Invoice Number</th>
                  <th className="pb-3 font-sans">Client / Project</th>
                  <th className="pb-3 text-right font-sans">Subtotal</th>
                  <th className="pb-3 text-right font-sans">Tax Type & Amt</th>
                  <th className="pb-3 text-right font-sans">Total Amount</th>
                  <th className="pb-3 text-center font-sans">Status</th>
                  <th className="pb-3 text-right pr-2 font-sans">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60 text-xs">
                {invoices.map((inv) => {
                  const isIgst = inv.igst && inv.igst > 0;
                  const taxLabel = isIgst ? 'IGST (18%)' : 'CGST+SGST (18%)';
                  const taxValue = isIgst ? inv.igst : (inv.cgst || 0) + (inv.sgst || 0);

                  return (
                    <tr key={inv.id} className="hover:bg-paper/30">
                      <td className="py-4 px-2 font-mono font-bold text-brass-dark">{inv.invoiceNumber}</td>
                      <td className="py-4">
                        <div className="font-bold text-ink">{inv.clientName}</div>
                        <div className="text-ink-muted text-[11px] font-sans mt-0.5">{inv.projectTitle}</div>
                      </td>
                      <td className="py-4 font-mono text-right text-ink-muted tabular-nums">₹{inv.subtotal?.toLocaleString()}</td>
                      <td className="py-4 font-mono text-right text-ink-muted tabular-nums">
                        <div>₹{taxValue?.toLocaleString()}</div>
                        <div className="text-[9px] text-brass-dark font-sans">{taxLabel}</div>
                      </td>
                      <td className="py-4 font-mono text-right font-bold text-ink tabular-nums">₹{inv.totalAmount?.toLocaleString()}</td>
                      <td className="py-4 text-center">
                        <span
                          className={`font-mono text-[10px] px-2.5 py-1 rounded-full font-bold uppercase ${
                            inv.status === 'PAID'
                              ? 'bg-forest-soft text-forest border border-forest/20'
                              : inv.status === 'SENT'
                              ? 'bg-brass-soft text-brass-dark border border-brass/20'
                              : 'bg-paper text-ink-muted border border-border'
                          }`}
                        >
                          {inv.status}
                        </span>
                      </td>
                      <td className="py-4 text-right pr-2 space-x-2">
                        {isClient && inv.status !== 'PAID' ? (
                          <button
                            onClick={() => handleRazorpayPayment(inv)}
                            disabled={payingInvoiceId === inv.id}
                            className="px-3 py-1.5 bg-forest hover:bg-forest/90 text-white rounded text-[11px] font-semibold inline-flex items-center gap-1.5 shadow-sm transition-all"
                          >
                            <CreditCard className="w-3.5 h-3.5" />
                            {payingInvoiceId === inv.id ? 'Processing...' : 'Pay Online'}
                          </button>
                        ) : isOwner && inv.status !== 'PAID' ? (
                          <button
                            onClick={() => handleMarkPaid(inv.id)}
                            className="px-2.5 py-1 bg-forest-soft hover:bg-forest/20 text-forest border border-forest/20 rounded text-[11px] font-semibold transition-colors"
                          >
                            Mark Paid
                          </button>
                        ) : null}

                        <button
                          onClick={() => handleDownloadPdf(inv.id)}
                          className="px-2.5 py-1 bg-brass-soft hover:bg-brass/25 text-brass-dark border border-brass/20 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors"
                        >
                          <Download className="w-3 h-3" />
                          PDF
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* Unbilled Hours Modal */}
      {showUnbilledModal && (
        <div className="fixed inset-0 bg-ink/40 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border p-6 rounded shadow-lg w-full max-w-md">
            <h3 className="font-display font-bold text-lg text-ink mb-1">Generate Invoice from Unbilled Time</h3>
            <p className="text-xs text-ink-muted mb-4">Select a project to aggregate all logged time entries and auto-compute itemized hours × hourly rate.</p>
            
            <form onSubmit={handleGenerateFromUnbilled} className="space-y-4 text-xs">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Project *</label>
                <select
                  required
                  value={selectedProjectId}
                  onChange={(e) => setSelectedProjectId(e.target.value)}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                >
                  <option value="">Select a Project</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title} ({p.clientName})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-border mt-4">
                <button
                  type="button"
                  onClick={() => setShowUnbilledModal(false)}
                  className="px-4 py-2 rounded bg-paper text-ink-muted border border-border hover:bg-ink/5"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded bg-forest text-white font-semibold hover:bg-forest/90"
                >
                  Generate Invoice
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Manual Invoice Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-ink/40 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border p-6 rounded shadow-lg w-full max-w-lg">
            <h3 className="font-display font-bold text-lg text-ink mb-4">Generate GST Tax Invoice</h3>
            <form onSubmit={handleCreate} className="space-y-4 text-xs">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Project *</label>
                <select
                  required
                  value={newInvoice.projectId}
                  onChange={(e) => setNewInvoice({ ...newInvoice, projectId: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                >
                  <option value="">Select Project</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title} ({p.clientName})
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between border-b border-border/40 pb-1">
                  <label className="text-ink-muted font-semibold">Line Items *</label>
                  <button
                    type="button"
                    onClick={handleAddItem}
                    className="text-brass-dark hover:underline text-[11px] font-bold flex items-center gap-1"
                  >
                    + Add Item
                  </button>
                </div>

                {newInvoice.lineItems.map((item, idx) => (
                  <div key={idx} className="grid grid-cols-12 gap-2">
                    <input
                      type="text"
                      placeholder="Description"
                      required
                      value={item.description}
                      onChange={(e) => handleItemChange(idx, 'description', e.target.value)}
                      className="col-span-6 bg-paper border border-border text-ink rounded p-2 focus:outline-none"
                    />
                    <input
                      type="number"
                      placeholder="Qty"
                      required
                      value={item.quantity}
                      onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                      className="col-span-2 bg-paper border border-border text-ink rounded p-2 focus:outline-none"
                    />
                    <input
                      type="number"
                      placeholder="Unit Price"
                      required
                      value={item.unitPrice}
                      onChange={(e) => handleItemChange(idx, 'unitPrice', e.target.value)}
                      className="col-span-4 bg-paper border border-border text-ink rounded p-2 focus:outline-none"
                    />
                  </div>
                ))}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">Due Date</label>
                  <input
                    type="date"
                    value={newInvoice.dueDate}
                    onChange={(e) => setNewInvoice({ ...newInvoice, dueDate: e.target.value })}
                    className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none"
                  />
                </div>
                <div className="flex items-center gap-2 pt-6">
                  <input
                    type="checkbox"
                    id="recurring"
                    checked={newInvoice.isRecurring}
                    onChange={(e) => setNewInvoice({ ...newInvoice, isRecurring: e.target.checked })}
                    className="rounded bg-paper border-border text-brass focus:ring-brass"
                  />
                  <label htmlFor="recurring" className="text-ink font-semibold">
                    Monthly Recurring
                  </label>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-border mt-4">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded bg-paper text-ink-muted border border-border hover:bg-ink/5"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded bg-brass text-white font-semibold hover:bg-brass-dark"
                >
                  Generate Invoice
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
