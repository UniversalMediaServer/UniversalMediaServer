import { test as setup } from '@playwright/test'

setup('Disable authentication', async ({ page }) => {
  page.on('pageerror', (err) => {
    console.error('Browser page error:', err.message)
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      console.error('Browser console error:', message)
    }
  })
  await page.goto('/')
  await page.getByRole('button', { name: 'Disable authentication' }).click()
  await page.getByRole('button', { name: 'Confirm' }).click()
})
