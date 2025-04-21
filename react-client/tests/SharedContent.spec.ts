import { expect, test } from '@playwright/test'

test('should be able to navigate to Shared Content', async ({ page }) => {
  await page.goto('/')

  // await page.getByText('Main menu').click()
  await page.getByText('Shared Content').click()

  await expect(page).toHaveURL(/.*shared/)

  // await expect(page).toHaveScreenshot()
})

test('should be able to add a YouTube channel as a video feed', async ({ page }) => {
  await page.goto('/shared')

  await page.getByText('Add new shared content').click()

  // await expect(page).toHaveScreenshot()

  await page.getByLabel('Type').first().click()

  await page.getByRole('option', { name: 'Video feed' }).locator('span').click()

  await page.getByLabel('Source/URL').fill('https://www.youtube.com/@kurzgesagt')

  await page.getByText('Add', { exact: true }).click()

  await expect(page.getByText('Kurzgesagt – In a Nutshell')).toBeVisible()

  await page.getByText('Save').click()

  // await expect(page).toHaveScreenshot()
})

test('should be able to select a YouTube video to watch', async ({ page }) => {
  await page.goto('/player')

  await page.getByText('Kurzgesagt – In a Nutshell').click()
  // will update this part of test once fix has been made
  await page.locator('.thumbnail-container').first().click()
  await page.getByText('Play', { exact: true }).click()
  await expect(page.getByTitle('Play Video')).toBeVisible()
})
