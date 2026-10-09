import React, { useEffect, useState } from 'react';
import { Building2, Save } from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const emptyProfile = {
  name: '', gstin: '', pan: '', registeredAddress: '', stateCode: '',
  bankName: '', bankAccountNumber: '', bankIfsc: '',
};
const inputClass = 'w-full rounded border border-border bg-surface px-3 py-2 text-sm text-ink outline-none focus:border-brass';

function Field({ label, name, value, onChange, placeholder, maxLength, pattern, required = false }) {
  return (
    <label className="block space-y-1.5 text-sm">
      <span className="font-medium text-ink">{label}</span>
      <input className={inputClass} name={name} value={value || ''} onChange={onChange}
        placeholder={placeholder} maxLength={maxLength} pattern={pattern} required={required} />
    </label>
  );
}

export default function AgencySettings() {
  const { refreshUser } = useAuth();
  const [profile, setProfile] = useState(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/agency/profile')
      .then(({ data }) => setProfile({ ...emptyProfile, ...data }))
      .catch((err) => setError(err.response?.data?.message || 'Could not load agency profile.'))
      .finally(() => setLoading(false));
  }, []);

  const onChange = (event) => setProfile((current) => ({ ...current, [event.target.name]: event.target.value }));
  const onSubmit = async (event) => {
    event.preventDefault();
    setSaving(true); setError(''); setNotice('');
    try {
      const { data } = await api.put('/agency/profile', profile);
      setProfile({ ...emptyProfile, ...data });
      await refreshUser();
      setNotice('Agency profile saved.');
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save agency profile. Check the tax and bank details.');
    } finally { setSaving(false); }
  };

  if (loading) return <main className="p-8 text-sm text-ink-muted">Loading agency profile…</main>;

  return (
    <main className="mx-auto w-full max-w-4xl space-y-6 p-6 md:p-10">
      <header className="flex items-start gap-3 border-b border-border pb-5">
        <div className="rounded-lg bg-brass/10 p-2 text-brass-dark"><Building2 className="h-5 w-5" /></div>
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Agency profile</h1>
          <p className="mt-1 text-sm text-ink-muted">Business identity for proposals and invoice workflows.</p>
        </div>
      </header>

      {notice && <div role="status" className="rounded border border-forest/30 bg-forest-soft p-3 text-sm text-forest">{notice}</div>}
      {error && <div role="alert" className="rounded border border-rust/30 bg-rust-soft/20 p-3 text-sm text-rust">{error}</div>}

      <form onSubmit={onSubmit} className="space-y-6">
        <section className="space-y-4 rounded-lg border border-border bg-surface p-5">
          <div><h2 className="font-semibold text-ink">Business details</h2><p className="text-xs text-ink-muted">Keep these aligned with registered business records.</p></div>
          <Field label="Agency or legal business name" name="name" value={profile.name} onChange={onChange} maxLength={150} required />
          <label className="block space-y-1.5 text-sm">
            <span className="font-medium text-ink">Registered address</span>
            <textarea className={inputClass} name="registeredAddress" value={profile.registeredAddress || ''} onChange={onChange} rows={3} maxLength={2000} />
          </label>
          <div className="grid gap-4 md:grid-cols-3">
            <Field label="GSTIN" name="gstin" value={profile.gstin} onChange={onChange} maxLength={15} pattern="[0-9]{2}[A-Za-z0-9]{13}" placeholder="Optional" />
            <Field label="PAN" name="pan" value={profile.pan} onChange={onChange} maxLength={10} pattern="[A-Za-z]{5}[0-9]{4}[A-Za-z]" placeholder="Optional" />
            <Field label="GST state code" name="stateCode" value={profile.stateCode} onChange={onChange} maxLength={2} pattern="[0-9]{2}" placeholder="Optional" />
          </div>
        </section>

        <section className="space-y-4 rounded-lg border border-border bg-surface p-5">
          <div><h2 className="font-semibold text-ink">Invoice bank details</h2><p className="text-xs text-ink-muted">Only agency owners can access these details here.</p></div>
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Bank name" name="bankName" value={profile.bankName} onChange={onChange} maxLength={120} />
            <Field label="IFSC" name="bankIfsc" value={profile.bankIfsc} onChange={onChange} maxLength={11} pattern="[A-Za-z]{4}0[A-Za-z0-9]{6}" />
          </div>
          <Field label="Account number" name="bankAccountNumber" value={profile.bankAccountNumber} onChange={onChange} maxLength={80} />
        </section>

        <div className="flex justify-end">
          <button disabled={saving} className="inline-flex items-center gap-2 rounded bg-forest px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60">
            <Save className="h-4 w-4" />{saving ? 'Saving…' : 'Save agency profile'}
          </button>
        </div>
      </form>
    </main>
  );
}
