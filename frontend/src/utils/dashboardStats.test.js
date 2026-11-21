import test from 'node:test';
import assert from 'node:assert/strict';
import { revenueByMonth, pipelineCounts, invoiceTotalsByStatus } from './dashboardStats.js';

const now = new Date(2026, 10, 15); // 15 Nov 2026

test('revenueByMonth buckets only PAID invoices into the last six months, oldest first', () => {
  const buckets = revenueByMonth([
    { status: 'PAID', createdAt: '2026-11-02T10:00:00', totalAmount: 1000 },
    { status: 'PAID', createdAt: '2026-11-20T10:00:00', totalAmount: 500 },
    { status: 'PAID', createdAt: '2026-06-10T10:00:00', totalAmount: 700 },
    { status: 'SENT', createdAt: '2026-11-02T10:00:00', totalAmount: 9999 },
    { status: 'PAID', createdAt: '2025-01-10T10:00:00', totalAmount: 4242 },
  ], 6, now);
  assert.equal(buckets.length, 6);
  assert.equal(buckets[0].label, 'Jun');
  assert.equal(buckets[0].value, 700);
  assert.equal(buckets[5].value, 1500);
  assert.equal(buckets.reduce((s, b) => s + b.value, 0), 2200);
});

test('pipelineCounts reports every stage, including empty ones', () => {
  const counts = pipelineCounts([{ dealStage: 'WON' }, { dealStage: 'WON' }, { dealStage: 'LEAD' }]);
  assert.deepEqual(counts.map((c) => [c.key, c.count]), [
    ['LEAD', 1], ['CONTACTED', 0], ['PROPOSAL_SENT', 0], ['WON', 2], ['LOST', 0],
  ]);
});

test('invoiceTotalsByStatus sums counts and amounts per status', () => {
  const t = Object.fromEntries(invoiceTotalsByStatus([
    { status: 'PAID', totalAmount: 100 }, { status: 'PAID', totalAmount: '50' }, { status: 'OVERDUE', totalAmount: 30 },
  ]).map((x) => [x.key, x]));
  assert.equal(t.PAID.total, 150);
  assert.equal(t.PAID.count, 2);
  assert.equal(t.OVERDUE.total, 30);
  assert.equal(t.DRAFT.count, 0);
});
