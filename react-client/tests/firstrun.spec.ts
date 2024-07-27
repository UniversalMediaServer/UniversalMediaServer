import { expect, test } from '@playwright/test';

test('should be able to disable authentication', async ({ page }) => {
  await page.goto('/');
  // await page.waitForTimeout(5000);
  const foo = await page.innerText('html');
  console.log(111,foo);

  await page.getByText('Disable authentication').click();
  await page.getByText('Confirm').click();
});

test('should be able to navigate to Server Settings', async ({ page }) => {
  await page.goto('/');

  await page.getByText('Main menu').click();
  await page.getByText('Server Settings').click();

  await expect(page).toHaveURL(/.*settings/);
});

test('should be able to see the advanced settings', async ({ page }) => {
  await page.goto('/settings');

  await page.getByText('Application').click();
  await page.getByText('Show advanced settings').click();

  await expect(page.getByText('Renderers Settings')).toBeInViewport();
});
