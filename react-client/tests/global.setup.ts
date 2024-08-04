import { test as setup } from '@playwright/test';

setup('global setup', async ({ page }) => {
  // todo: fix the need for this initial delay
  await page.waitForTimeout(1000);
  await page.reload();
});