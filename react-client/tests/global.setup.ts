import { test as setup } from '@playwright/test'

setup('Disable authentication', async ({ page }) => {
  page.on('pageerror', (err) => {
    console.log(err.message)
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      console.error(message)
    }
  })
  await page.evaluate(() => {
    console.error('hello from the browser')
  })
  await page.goto('/')
  await page.getByRole('button', { name: 'Disable authentication' }).click()
  await page.getByRole('button', { name: 'Confirm' }).click()
})
