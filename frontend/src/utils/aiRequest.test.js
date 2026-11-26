import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { buildAiContractRequest } from './aiRequest.js';

const agency = { state: 'Maharashtra', isGstRegistered: true };
const form = {
  clientName: 'Acme',
  projectScope: 'Build a booking portal with admin reporting and payments.',
  deliverables: 'Web app',
  clientCountry: 'IN',
  clientState: 'Gujarat',
  pricingType: 'Fixed Price',
  currency: 'INR',
  totalBudget: '50000',
  startDate: '2027-01-10',
  timeline: '',
};

describe('buildAiContractRequest', () => {
  it('sends the fields the backend expects, not the raw form fields', () => {
    const req = buildAiContractRequest(form, agency, '50% - Advance');
    assert.deepEqual(Object.keys(req).sort(), [
      'budgetGuidance', 'clientName', 'deliverables', 'gstStatus', 'jurisdiction', 'paymentTerms', 'projectScope', 'timeline',
    ]);
    assert.equal(req.budgetGuidance, 'INR 50000 (Fixed Price, before GST)');
    assert.equal(req.gstStatus, 'IGST @18% extra');
    assert.equal(req.jurisdiction, 'Courts of Maharashtra, India');
    assert.equal(req.timeline, 'Starting 2027-01-10');
    assert.equal(req.paymentTerms, '50% - Advance');
  });

  it('describes same-state, unregistered and export cases', () => {
    assert.equal(buildAiContractRequest({ ...form, clientState: 'Maharashtra' }, agency, '').gstStatus, 'CGST/SGST @18% extra');
    assert.equal(buildAiContractRequest(form, { ...agency, isGstRegistered: false }, '').gstStatus, 'No GST charged');
    assert.equal(buildAiContractRequest({ ...form, clientCountry: 'US' }, agency, '').gstStatus, 'Export of services, zero-rated under LUT');
  });

  it('leaves facts blank when the form does not give them', () => {
    const req = buildAiContractRequest({ ...form, totalBudget: '', startDate: '' }, { state: '' }, '');
    assert.equal(req.budgetGuidance, '');
    assert.equal(req.gstStatus, '');
    assert.equal(req.jurisdiction, '');
    assert.equal(req.timeline, '');
  });
});
