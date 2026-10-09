import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { calculateCommercials } from './taxMath.js';

describe('calculateCommercials', () => {
  it('Same-state client', () => {
    const res = calculateCommercials({
      budget: 100000,
      clientCountry: 'IN',
      clientState: 'Maharashtra',
      agencyState: 'Maharashtra',
      isGstRegistered: true,
      paymentMilestones: [
        { label: 'Advance', percentage: 40 },
        { label: 'Midway', percentage: 30 },
        { label: 'Final', percentage: 30 }
      ]
    });

    assert.equal(res.taxType, 'CGST/SGST');
    assert.equal(res.gstAmount, 18000);
    assert.equal(res.grandTotal, 118000);
    assert.equal(res.invoices[0].invoiceTotal, 47200);
    assert.equal(res.invoices[1].invoiceTotal, 35400);
    assert.equal(res.invoices[2].invoiceTotal, 35400);
  });

  it('Other-state client', () => {
    const res = calculateCommercials({
      budget: 100000,
      clientCountry: 'IN',
      clientState: 'Karnataka',
      agencyState: 'Maharashtra',
      isGstRegistered: true,
      paymentMilestones: [
        { label: 'Advance', percentage: 100 }
      ]
    });

    assert.equal(res.taxType, 'IGST');
    assert.equal(res.gstAmount, 18000);
    assert.equal(res.grandTotal, 118000);
  });

  it('Foreign client', () => {
    const res = calculateCommercials({
      budget: 50000,
      clientCountry: 'US',
      clientState: 'California',
      agencyState: 'Maharashtra',
      isGstRegistered: true,
      paymentMilestones: [
        { label: 'Advance', percentage: 40 },
        { label: 'Midway', percentage: 30 },
        { label: 'Final', percentage: 30 }
      ]
    });

    assert.equal(res.taxType, 'Export');
    assert.equal(res.gstAmount, 0);
    assert.equal(res.grandTotal, 50000);
    assert.equal(res.invoices[0].invoiceTotal, 20000);
    assert.equal(res.invoices[1].invoiceTotal, 15000);
    assert.equal(res.invoices[2].invoiceTotal, 15000);
  });

  it('Rounding', () => {
    const res = calculateCommercials({
      budget: 33333,
      clientCountry: 'US',
      clientState: 'California',
      agencyState: 'Maharashtra',
      isGstRegistered: false,
      paymentMilestones: [
        { label: 'Advance', percentage: 40 },
        { label: 'Midway', percentage: 30 },
        { label: 'Final', percentage: 30 }
      ]
    });

    // budget: 33333
    // 40%: 13333.2
    // 30%: 9999.9
    // 30%: remaining -> 9999.9
    assert.equal(res.grandTotal, 33333);
    const sum = res.invoices.reduce((acc, inv) => acc + inv.invoiceTotal, 0);
    assert.equal(Math.round(sum * 100) / 100, 33333);
  });

  it('Unregistered agency', () => {
    const res = calculateCommercials({
      budget: 100000,
      clientCountry: 'IN',
      clientState: 'Maharashtra',
      agencyState: 'Maharashtra',
      isGstRegistered: false, // Unregistered
      paymentMilestones: [
        { label: 'Advance', percentage: 100 }
      ]
    });

    assert.equal(res.taxType, 'None');
    assert.equal(res.gstAmount, 0);
    assert.equal(res.grandTotal, 100000);
  });
});
