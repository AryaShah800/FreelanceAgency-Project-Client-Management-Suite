import { test, expect } from '@playwright/test';
import { openDemo } from './helpers.js';

test('mobile: drawer opens, invoices are cards, page does not scroll sideways', async ({ page }) => {
  await openDemo(page);
  await page.getByRole('button', { name: 'Open navigation menu' }).click();
  await page.getByRole('link', { name: 'GST Invoices' }).click();
  await expect(page.getByRole('list', { name: 'Invoices' })).toBeVisible();
  await expect(page.locator('table')).toBeHidden();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(overflow).toBeLessThanOrEqual(0);
});
