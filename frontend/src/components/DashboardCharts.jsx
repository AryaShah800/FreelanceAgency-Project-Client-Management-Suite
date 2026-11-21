import React, { useMemo } from 'react';

import { STATUS_STYLE, revenueByMonth, pipelineCounts, invoiceTotalsByStatus } from '../utils/dashboardStats';

const inr = (n) => `₹${Math.round(n).toLocaleString('en-IN')}`;
const compact = (n) => (n >= 100000 ? `${(n / 100000).toFixed(1)}L` : n >= 1000 ? `${Math.round(n / 1000)}k` : `${n}`);

function Panel({ title, note, children }) {
  return (
    <section className="bg-surface p-6 rounded border border-border shadow-sm space-y-4">
      <div className="flex items-baseline justify-between pb-2 border-b border-border">
        <h3 className="font-display font-semibold text-base text-ink">{title}</h3>
        {note && <span className="text-xs font-mono text-ink-muted">{note}</span>}
      </div>
      {children}
    </section>
  );
}

export default function DashboardCharts({ invoices, clients }) {
  const revenue = useMemo(() => revenueByMonth(invoices), [invoices]);
  const pipeline = useMemo(() => pipelineCounts(clients), [clients]);
  const statuses = useMemo(() => invoiceTotalsByStatus(invoices), [invoices]);

  const maxRevenue = Math.max(1, ...revenue.map((r) => r.value));
  const maxStage = Math.max(1, ...pipeline.map((p) => p.count));
  const outstanding = statuses.filter((s) => s.key === 'SENT' || s.key === 'OVERDUE').reduce((s, x) => s + x.total, 0);
  const grandTotal = Math.max(1, statuses.reduce((s, x) => s + x.total, 0));
  const hasRevenue = revenue.some((r) => r.value > 0);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6" data-testid="dashboard-charts">
      <div className="lg:col-span-6">
        <Panel title="Revenue collected" note="Last 6 months, paid invoices">
          {hasRevenue ? (
            <svg viewBox="0 0 360 170" role="img" aria-label="Paid revenue by month" className="w-full h-auto">
              {[0, 0.5, 1].map((f) => (
                <g key={f}>
                  <line x1="34" x2="356" y1={140 - f * 120} y2={140 - f * 120} stroke="currentColor" className="text-border" strokeWidth="1" />
                  <text x="30" y={144 - f * 120} textAnchor="end" className="fill-ink-muted" fontSize="9">{compact(maxRevenue * f)}</text>
                </g>
              ))}
              {revenue.map((r, i) => {
                const h = (r.value / maxRevenue) * 120;
                const x = 44 + i * 52;
                return (
                  <g key={r.key}>
                    <rect x={x} y={140 - h} width="34" height={h} rx="2" className="fill-forest">
                      <title>{`${r.label}: ${inr(r.value)}`}</title>
                    </rect>
                    <text x={x + 17} y="156" textAnchor="middle" className="fill-ink-muted" fontSize="10">{r.label}</text>
                  </g>
                );
              })}
            </svg>
          ) : (
            <p className="text-xs text-ink-muted py-8 text-center">No paid invoices in the last six months. Revenue appears here once an invoice is paid.</p>
          )}
        </Panel>
      </div>

      <div className="lg:col-span-3">
        <Panel title="Pipeline" note={`${clients.length} clients`}>
          <ul className="space-y-2.5">
            {pipeline.map((s) => (
              <li key={s.key}>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-ink">{s.label}</span>
                  <span className="font-mono text-ink-muted">{s.count}</span>
                </div>
                <div className="h-2 bg-paper rounded border border-border overflow-hidden">
                  <div
                    className={`h-full ${s.key === 'LOST' ? 'bg-ink-muted/40' : s.key === 'WON' ? 'bg-forest' : 'bg-brass'}`}
                    style={{ width: `${(s.count / maxStage) * 100}%` }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </Panel>
      </div>

      <div className="lg:col-span-3">
        <Panel title="Invoices" note="by status">
          <div className="flex h-3 rounded overflow-hidden border border-border" role="img" aria-label="Invoice value by status">
            {statuses.filter((s) => s.total > 0).map((s) => (
              <div key={s.key} className={s.bar} style={{ width: `${(s.total / grandTotal) * 100}%` }} title={`${s.label}: ${inr(s.total)}`} />
            ))}
          </div>
          <ul className="space-y-2 text-xs">
            {statuses.map((s) => (
              <li key={s.key} className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-ink"><span className={`inline-block w-2.5 h-2.5 rounded-sm ${s.bar}`} />{s.label}</span>
                <span className="font-mono text-ink-muted">{s.count} · {inr(s.total)}</span>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </div>
  );
}
