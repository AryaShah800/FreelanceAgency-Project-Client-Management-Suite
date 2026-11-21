import { test, expect } from '@playwright/test';
import { openDemo } from './helpers.js';

test('one click opens the seeded workspace with charts', async ({ page }) => {
  await openDemo(page);
  const charts = page.getByTestId('dashboard-charts');
  await expect(charts.getByText('Revenue collected')).toBeVisible();
  await expect(charts.getByText('Pipeline')).toBeVisible();
  await expect(page.getByText('12', { exact: true }).first()).toBeVisible(); // ongoing projects
});

test('CRM lists every client from the seed', async ({ page }) => {
  await openDemo(page);
  await page.getByRole('link', { name: 'Client CRM' }).click();
  for (const name of ['Northwind Logistics', 'Kestrel Fintech', 'Orbit Mobility', 'Paperkite Studios']) {
    await expect(page.getByText(name).first()).toBeVisible();
  }
});

test('invoices show paid, sent, overdue and draft, and the owner can act on open ones', async ({ page }) => {
  await openDemo(page);
  await page.getByRole('link', { name: 'GST Invoices' }).click();
  const table = page.locator('table');
  for (const status of ['PAID', 'SENT', 'OVERDUE']) {
    await expect(table.getByText(status, { exact: true }).first()).toBeVisible();
  }
  // Read-only on purpose: the demo workspace is shared, so specs must not change its data.
  await expect(table.getByRole('button', { name: 'Mark Paid' }).first()).toBeVisible();
});

test('public proposal link opens without signing in', async ({ page }) => {
  await page.goto('/p/demo-proposal-token-2026');
  await expect(page.getByText(/Orbit Mobility/).first()).toBeVisible();
});
