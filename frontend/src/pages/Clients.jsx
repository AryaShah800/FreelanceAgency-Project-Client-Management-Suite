import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { Mail, Phone, Building, Plus, Trash2, ShieldAlert } from 'lucide-react';

const STAGES = [
  { id: 'LEAD', label: 'Leads', color: 'border-t-ink-faint bg-surface' },
  { id: 'CONTACTED', label: 'Contacted', color: 'border-t-ink-muted bg-surface' },
  { id: 'PROPOSAL_SENT', label: 'Proposal Sent', color: 'border-t-brass bg-surface' },
  { id: 'WON', label: 'Won / Active', color: 'border-t-forest bg-surface' },
  { id: 'LOST', label: 'Lost', color: 'border-t-rust bg-surface' },
];

export default function Clients() {
  const [clients, setClients] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [newClient, setNewClient] = useState({
    companyName: '',
    contactPerson: '',
    email: '',
    phone: '',
    gstin: '',
    dealStage: 'LEAD',
  });

  useEffect(() => {
    fetchClients();
  }, []);

  const fetchClients = async () => {
    try {
      const res = await api.get('/clients');
      setClients(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await api.post('/clients', newClient);
      setShowModal(false);
      setNewClient({ companyName: '', contactPerson: '', email: '', phone: '', gstin: '', dealStage: 'LEAD' });
      fetchClients();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create client');
    }
  };

  const handleStageChange = async (id, stage) => {
    try {
      await api.patch(`/clients/${id}/stage?stage=${stage}`);
      fetchClients();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete client?')) return;
    try {
      await api.delete(`/clients/${id}`);
      fetchClients();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-paper">
      <Navbar title="Client CRM Pipeline" onQuickAction={() => setShowModal(true)} />

      <main className="flex-1 p-6">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 overflow-x-auto pb-4">
          {STAGES.map((stage) => {
            const stageClients = clients.filter((c) => c.dealStage === stage.id);
            return (
              <div key={stage.id} className={`border border-border border-t-4 rounded-b shadow-sm p-4 min-w-[260px] flex flex-col justify-between ${stage.color}`}>
                <div>
                  <div className="flex items-center justify-between mb-4 border-b border-border/40 pb-2">
                    <h3 className="font-sans text-xs font-bold uppercase tracking-wider text-ink-muted">{stage.label}</h3>
                    <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-paper border border-border text-ink-muted">
                      {stageClients.length}
                    </span>
                  </div>

                  <div className="space-y-3">
                    {stageClients.map((client) => (
                      <div key={client.id} className="bg-surface border border-border p-4 rounded hover:border-brass/40 transition-all relative group">
                        <div className="flex items-start justify-between gap-1">
                          <h4 className="font-display font-semibold text-sm text-ink">{client.companyName}</h4>
                          <button
                            onClick={() => handleDelete(client.id)}
                            className="opacity-0 group-hover:opacity-100 text-ink-faint hover:text-rust transition-opacity p-0.5"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>

                        {client.contactPerson && (
                          <p className="text-xs text-ink-muted mt-1.5 flex items-center gap-1.5">
                            <Building className="w-3 h-3 text-brass" />
                            {client.contactPerson}
                          </p>
                        )}

                        <p className="text-xs text-ink-muted flex items-center gap-1.5 truncate mt-1">
                          <Mail className="w-3 h-3 text-brass" />
                          {client.email}
                        </p>

                        {client.phone && (
                          <p className="text-xs text-ink-muted flex items-center gap-1.5 mt-1">
                            <Phone className="w-3 h-3 text-brass" />
                            {client.phone}
                          </p>
                        )}

                        <div className="pt-2 mt-3 border-t border-border/40">
                          <select
                            value={client.dealStage}
                            onChange={(e) => handleStageChange(client.id, e.target.value)}
                            className="w-full bg-paper border border-border text-[10px] text-ink-muted rounded px-2 py-1 focus:outline-none"
                          >
                            {STAGES.map((s) => (
                              <option key={s.id} value={s.id}>
                                Move to: {s.label}
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </main>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-ink/40 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border p-6 rounded shadow-lg w-full max-w-md">
            <h3 className="font-display font-bold text-lg text-ink mb-4">Add Client</h3>
            <form onSubmit={handleCreate} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Company Name *</label>
                <input
                  type="text"
                  required
                  value={newClient.companyName}
                  onChange={(e) => setNewClient({ ...newClient, companyName: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                />
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Contact Person</label>
                <input
                  type="text"
                  value={newClient.contactPerson}
                  onChange={(e) => setNewClient({ ...newClient, contactPerson: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                />
              </div>
              <div>
                <label className="block text-ink-muted font-semibold mb-1">Email *</label>
                <input
                  type="email"
                  required
                  value={newClient.email}
                  onChange={(e) => setNewClient({ ...newClient, email: e.target.value })}
                  className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">Phone</label>
                  <input
                    type="text"
                    value={newClient.phone}
                    onChange={(e) => setNewClient({ ...newClient, phone: e.target.value })}
                    className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                  />
                </div>
                <div>
                  <label className="block text-ink-muted font-semibold mb-1">GSTIN</label>
                  <input
                    type="text"
                    value={newClient.gstin}
                    onChange={(e) => setNewClient({ ...newClient, gstin: e.target.value })}
                    className="w-full bg-paper border border-border text-ink rounded p-2.5 focus:outline-none focus:border-brass"
                  />
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
                  Create Client
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
