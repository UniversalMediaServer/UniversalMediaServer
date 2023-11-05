import { test } from '@playwright/test';

test('has username field', async ({ page }) => {
  await page.goto('/');

  await page.getByLabel('Username').fill('admin');
});

