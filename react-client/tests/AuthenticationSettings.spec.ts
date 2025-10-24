import { test, expect } from '@playwright/test'

test('authentication switches show and confirmation modal appears when toggling', async ({ page }) => {
  // Use the dev server URL. Adjust if your app runs on a different port.
  await page.goto('http://localhost:5173/accounts')

  // Wait for the accounts page to render the switches
  const switches = page.getByRole('switch')
  await expect(switches.first()).toBeVisible()

  // Target the first switch (authentication service)
  const authSwitch = switches.first()
  await expect(authSwitch).toBeVisible()

  // Click the switch. If the action requires confirmation (disable), a dialog should appear.
  await authSwitch.click()

  // The confirmation uses a modal; it should have role=dialog
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
})
