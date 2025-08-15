import { test as setup } from '@playwright/test'

setup('Put back authentication', async ({ page }) => {
  await page.goto('/accounts')
  await page.getByRole('button', { name: 'Enable' }).click()
})
