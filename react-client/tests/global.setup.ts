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

  await page.waitForTimeout(5000)

  await page.goto('/')

  // if page is empty, reload until the server is ready to serve
  let html = await page.content()
  console.log('html', html)

  while (
    html === '<html><head></head><body></body></html>'
    || html.includes('404 - File Not Found')
  ) {
    console.log('Got an empty page from server, reloading in 1 second...')
    await page.waitForTimeout(1000)
    await page.reload()
    html = await page.content()
  }

  await page.screenshot({ path: 'screenshot.png' })
  await page.getByText('Disable authentication').click()
  await page.getByText('Confirm').click()
})
