export const STAGES = [
  { key: 'LEAD', label: 'Lead' },
  { key: 'CONTACTED', label: 'Contacted' },
  { key: 'PROPOSAL_SENT', label: 'Proposal sent' },
  { key: 'WON', label: 'Won' },
  { key: 'LOST', label: 'Lost' },
];

export const STATUS_STYLE = {
  PAID: { label: 'Paid', bar: 'bg-forest' },
  SENT: { label: 'Sent', bar: 'bg-brass' },
  OVERDUE: { label: 'Overdue', bar: 'bg-rust' },
  DRAFT: { label: 'Draft', bar: 'bg-ink-muted/40' },
};

/** Paid revenue per calendar month for the last `months` months, oldest first. */
export function revenueByMonth(invoices, months = 6, now = new Date()) {
  const buckets = [];
  for (let i = months - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    buckets.push({
      key: `${d.getFullYear()}-${d.getMonth()}`,
      label: d.toLocaleString('en-IN', { month: 'short' }),
      value: 0,
    });
  }
  const byKey = Object.fromEntries(buckets.map((b) => [b.key, b]));
  for (const inv of invoices) {
    if (inv.status !== 'PAID' || !inv.createdAt) continue;
    const d = new Date(inv.createdAt);
    const bucket = byKey[`${d.getFullYear()}-${d.getMonth()}`];
    if (bucket) bucket.value += Number(inv.totalAmount) || 0;
  }
  return buckets;
}

export function pipelineCounts(clients) {
  return STAGES.map((s) => ({ ...s, count: clients.filter((c) => c.dealStage === s.key).length }));
}

export function invoiceTotalsByStatus(invoices) {
  return Object.keys(STATUS_STYLE).map((key) => ({
    key,
    ...STATUS_STYLE[key],
    count: invoices.filter((i) => i.status === key).length,
    total: invoices.filter((i) => i.status === key).reduce((s, i) => s + (Number(i.totalAmount) || 0), 0),
  }));
}

