import { test as setup } from '@playwright/test'

setup('Put back authentication', async ({ page }) => {
  await page.goto('/accounts')

  await page.getByText('Enable').click()
})
