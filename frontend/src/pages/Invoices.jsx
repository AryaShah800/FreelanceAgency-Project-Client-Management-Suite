import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { FileText, Download, Plus, IndianRupee, CheckCircle, Clock } from 'lucide-react';

export default function Invoices() {
  const [invoices, setInvoices] = useState([]);
  const [projects, setProjects] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [newInvoice, setNewInvoice] = useState({
    projectId: '',
    dueDate: '',
    isRecurring: false,
    lineItems: [{ description: 'Web Development Services', quantity: 1, unitPrice: 50000 }],
  });

  useEffect(() => {
    fetchInvoices();
    fetchProjects();
  }, []);

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

  const handleDownloadPdf = (id) => {
    window.open(`/api/v1/invoices/${id}/pdf`, '_blank');
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
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar title="GST Tax Invoices & PDF Generator" onQuickAction={() => setShowModal(true)} />

      <main className="flex-1 p-6">
        <div className="glass-panel p-6 rounded-2xl border border-dark-border">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-dark-border/60 text-xs font-semibold text-dark-muted">
                  <th className="pb-3 px-2">Invoice #</th>
                  <th className="pb-3">Client / Project</th>
                  <th className="pb-3">Subtotal</th>
                  <th className="pb-3">GST (CGST 9% + SGST 9%)</th>
                  <th className="pb-3">Total Amount</th>
                  <th className="pb-3">Status</th>
                  <th className="pb-3 text-right pr-2">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-dark-border/40 text-xs">
                {invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-dark-card/30">
                    <td className="py-3.5 px-2 font-mono font-bold text-brand-400">{inv.invoiceNumber}</td>
                    <td className="py-3.5">
                      <div className="font-bold text-white">{inv.clientName}</div>
                      <div className="text-dark-muted text-[11px]">{inv.projectTitle}</div>
                    </td>
                    <td className="py-3.5 text-dark-muted">₹{inv.subtotal?.toLocaleString()}</td>
                    <td className="py-3.5 text-dark-muted">₹{(inv.cgst + inv.sgst)?.toLocaleString()}</td>
                    <td className="py-3.5 font-bold text-white">₹{inv.totalAmount?.toLocaleString()}</td>
                    <td className="py-3.5">
                      <span
                        className={`text-[10px] px-2.5 py-1 rounded-full font-bold uppercase tracking-wider ${
                          inv.status === 'PAID'
                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                            : inv.status === 'SENT'
                            ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                            : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                        }`}
                      >
                        {inv.status}
                      </span>
                    </td>
                    <td className="py-3.5 text-right pr-2 space-x-2">
                      {inv.status !== 'PAID' && (
                        <button
                          onClick={() => handleMarkPaid(inv.id)}
                          className="px-2.5 py-1 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg text-[11px] font-semibold"
                        >
                          Mark Paid
                        </button>
                      )}
                      <button
                        onClick={() => handleDownloadPdf(inv.id)}
                        className="px-2.5 py-1 bg-brand-500/10 hover:bg-brand-500/20 text-brand-400 border border-brand-500/30 rounded-lg text-[11px] font-semibold inline-flex items-center gap-1"
                      >
                        <Download className="w-3 h-3" />
                        PDF
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-dark-border w-full max-w-lg">
            <h3 className="text-lg font-bold text-white mb-4">Generate GST Tax Invoice</h3>
            <form onSubmit={handleCreate} className="space-y-4 text-xs">
              <div>
                <label className="block text-dark-muted font-semibold mb-1">Project *</label>
                <select
                  required
                  value={newInvoice.projectId}
                  onChange={(e) => setNewInvoice({ ...newInvoice, projectId: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
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
                <div className="flex items-center justify-between">
                  <label className="text-dark-muted font-semibold">Line Items *</label>
                  <button
                    type="button"
                    onClick={handleAddItem}
                    className="text-brand-400 hover:underline text-[11px] font-bold flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3" /> Add Item
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
                      className="col-span-6 bg-dark-bg border border-dark-border text-white rounded-lg p-2"
                    />
                    <input
                      type="number"
                      placeholder="Qty"
                      required
                      value={item.quantity}
                      onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                      className="col-span-2 bg-dark-bg border border-dark-border text-white rounded-lg p-2"
                    />
                    <input
                      type="number"
                      placeholder="Unit Price"
                      required
                      value={item.unitPrice}
                      onChange={(e) => handleItemChange(idx, 'unitPrice', e.target.value)}
                      className="col-span-4 bg-dark-bg border border-dark-border text-white rounded-lg p-2"
                    />
                  </div>
                ))}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-dark-muted font-semibold mb-1">Due Date</label>
                  <input
                    type="date"
                    value={newInvoice.dueDate}
                    onChange={(e) => setNewInvoice({ ...newInvoice, dueDate: e.target.value })}
                    className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                  />
                </div>
                <div className="flex items-center gap-2 pt-6">
                  <input
                    type="checkbox"
                    id="recurring"
                    checked={newInvoice.isRecurring}
                    onChange={(e) => setNewInvoice({ ...newInvoice, isRecurring: e.target.checked })}
                    className="rounded bg-dark-bg border-dark-border text-brand-600"
                  />
                  <label htmlFor="recurring" className="text-dark-muted font-semibold">
                    Monthly Recurring
                  </label>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded-xl bg-dark-card text-dark-muted hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-gradient-to-r from-brand-600 to-indigo-600 text-white font-semibold"
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
