import { test as setup } from '@playwright/test';

setup('global setup', async ({ page }) => {
  await page.goto('/');

  // todo: fix the need for this initial delay/reload
  await page.waitForTimeout(1000);
  await page.reload();

  await page.getByText('Disable authentication').click();
  await page.getByText('Confirm').click();
});