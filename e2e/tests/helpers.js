import { expect } from '@playwright/test';

export async function openDemo(page) {
  await page.goto('/login');
  await page.getByRole('button', { name: /recruiter demo/i }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText('Welcome to Freelance Suite Demo')).toBeVisible();
}
