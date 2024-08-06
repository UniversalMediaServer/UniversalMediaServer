import { expect, test } from '@playwright/test';

test('should be able to navigate to Shared Content', async ({ page }) => {
  await page.goto('/');

  await page.getByText('Main menu').click();
  await page.getByText('Shared Content').click();

  await expect(page).toHaveURL(/.*shared/);

  //await expect(page).toHaveScreenshot();
});

test('should be able to add a YouTube channel as a video feed', async ({ page }) => {
  await page.goto('/shared');

  await page.getByText('Add new shared content').click();

  //await expect(page).toHaveScreenshot();

  await page.getByLabel('Type').first().click();
  await page.getByText('Video feed').click();

  await page.getByLabel('Source/URL').fill('https://www.youtube.com/@kurzgesagt');

  await page.getByText('Add', { exact: true }).click();

  await expect(page.getByText('Kurzgesagt – In a Nutshell')).toBeVisible();

  //await expect(page).toHaveScreenshot();
});
