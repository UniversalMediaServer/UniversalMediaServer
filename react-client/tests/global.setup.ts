import { test as setup } from '@playwright/test'

setup('Disable authentication', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Disable authentication'}).click()
  await page.getByRole('button', { name: 'Confirm'}).click()
})
