import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { Users, Mail, Phone, Building, Plus, Trash2 } from 'lucide-react';

const STAGES = [
  { id: 'LEAD', label: 'Leads', color: 'border-amber-500/50 bg-amber-500/5' },
  { id: 'CONTACTED', label: 'Contacted', color: 'border-blue-500/50 bg-blue-500/5' },
  { id: 'PROPOSAL_SENT', label: 'Proposal Sent', color: 'border-purple-500/50 bg-purple-500/5' },
  { id: 'WON', label: 'Won / Active', color: 'border-emerald-500/50 bg-emerald-500/5' },
  { id: 'LOST', label: 'Lost', color: 'border-red-500/50 bg-red-500/5' },
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
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar title="Client CRM & Pipeline" onQuickAction={() => setShowModal(true)} />

      <main className="flex-1 p-6">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 overflow-x-auto pb-4">
          {STAGES.map((stage) => {
            const stageClients = clients.filter((c) => c.dealStage === stage.id);
            return (
              <div key={stage.id} className={`glass-panel p-4 rounded-2xl border ${stage.color} min-w-[260px]`}>
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-xs font-bold text-white uppercase tracking-wider">{stage.label}</h3>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-dark-card text-dark-muted">
                    {stageClients.length}
                  </span>
                </div>

                <div className="space-y-3">
                  {stageClients.map((client) => (
                    <div key={client.id} className="glass-card p-4 rounded-xl space-y-2 relative group">
                      <div className="flex items-start justify-between">
                        <h4 className="text-sm font-bold text-white">{client.companyName}</h4>
                        <button
                          onClick={() => handleDelete(client.id)}
                          className="opacity-0 group-hover:opacity-100 text-dark-muted hover:text-red-400 transition-opacity p-1"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {client.contactPerson && (
                        <p className="text-xs text-dark-muted flex items-center gap-1.5">
                          <Building className="w-3 h-3 text-brand-400" />
                          {client.contactPerson}
                        </p>
                      )}

                      <p className="text-xs text-dark-muted flex items-center gap-1.5 truncate">
                        <Mail className="w-3 h-3 text-brand-400" />
                        {client.email}
                      </p>

                      {client.phone && (
                        <p className="text-xs text-dark-muted flex items-center gap-1.5">
                          <Phone className="w-3 h-3 text-brand-400" />
                          {client.phone}
                        </p>
                      )}

                      <div className="pt-2 border-t border-dark-border/40">
                        <select
                          value={client.dealStage}
                          onChange={(e) => handleStageChange(client.id, e.target.value)}
                          className="w-full bg-dark-bg/80 border border-dark-border text-[11px] text-dark-muted rounded-lg px-2 py-1 focus:outline-none"
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
            );
          })}
        </div>
      </main>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-dark-border w-full max-w-md">
            <h3 className="text-lg font-bold text-white mb-4">Add New Client</h3>
            <form onSubmit={handleCreate} className="space-y-3 text-xs">
              <div>
                <label className="block text-dark-muted font-semibold mb-1">Company Name *</label>
                <input
                  type="text"
                  required
                  value={newClient.companyName}
                  onChange={(e) => setNewClient({ ...newClient, companyName: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
              </div>
              <div>
                <label className="block text-dark-muted font-semibold mb-1">Contact Person</label>
                <input
                  type="text"
                  value={newClient.contactPerson}
                  onChange={(e) => setNewClient({ ...newClient, contactPerson: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
              </div>
              <div>
                <label className="block text-dark-muted font-semibold mb-1">Email *</label>
                <input
                  type="email"
                  required
                  value={newClient.email}
                  onChange={(e) => setNewClient({ ...newClient, email: e.target.value })}
                  className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-dark-muted font-semibold mb-1">Phone</label>
                  <input
                    type="text"
                    value={newClient.phone}
                    onChange={(e) => setNewClient({ ...newClient, phone: e.target.value })}
                    className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                  />
                </div>
                <div>
                  <label className="block text-dark-muted font-semibold mb-1">GSTIN</label>
                  <input
                    type="text"
                    value={newClient.gstin}
                    onChange={(e) => setNewClient({ ...newClient, gstin: e.target.value })}
                    className="w-full bg-dark-bg border border-dark-border text-white rounded-xl p-2.5"
                  />
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
