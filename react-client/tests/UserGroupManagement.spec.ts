import { expect, test, chromium } from '@playwright/test'

test.describe('User and Group Management', () => {
  test.describe.configure({ mode: 'serial' })

  // Admin credentials used for the initial admin creation and subsequent logins
  const adminUsername = 'testadmin'
  const adminPassword = 'testpassword123'

  /**
   * Helper: log in as the admin user via the password login form.
   * After auth is enabled, navigating to any page will render the Login page
   * if the browser context doesn't have a valid token. This fills the login form
   * and submits it.
   */
  async function loginAsAdmin(page: import('@playwright/test').Page) {
    // Wait for the Login page to be ready.
    // The Login component renders a <Text> heading "Log in" (i18n key LogIn)
    // above the form. Using getByText is the most reliable selector here
    // because it matches regardless of which login form variant is shown
    // (PassLogin, UsersLogin, etc.).
    await expect(page.getByText('Log in').first()).toBeVisible({ timeout: 10000 })

    // The password login form has Username and Password fields
    await page.getByLabel('Username').fill(adminUsername)
    await page.getByLabel('Password').fill(adminPassword)
    await page.getByRole('button', { name: 'Log in' }).click()

    // Wait for the app to fully load after login
    await expect(page.getByText('Manage accounts')).toBeVisible({ timeout: 10000 })
  }

  /**
   * Guaranteed cleanup: disable authentication so global.teardown.ts can find
   * the "Enable" button. This hook runs even when tests in the suite fail,
   * which prevents the cascading teardown timeout.
   */
  test.afterAll(async () => {
    test.setTimeout(60000)
    const browser = await chromium.launch()
    const context = await browser.newContext()
    const page = await context.newPage()
    try {
      // Navigate to the accounts page with an absolute URL since this
      // context does not inherit baseURL from the Playwright config.
      await page.goto('http://localhost:9001/accounts')
      await page.waitForTimeout(2000)

      // Check if the "Enable" button is already visible — if so, auth is
      // already disabled and there is nothing to clean up.
      const enableButton = page.getByRole('button', { name: 'Enable' })
      if (await enableButton.isVisible().catch(() => false)) {
        return
      }

      // Auth is still enabled. We may be on the Login page.
      // The server may render UsersLogin (card view) when multiple users
      // exist, or PassLogin (direct form), or NoAdminLogin (first boot).
      const createButton = page.getByRole('button', { name: 'Create' })
      if (await createButton.isVisible().catch(() => false)) {
        // NoAdminLogin form — admin was never created, but auth is enabled.
        // Use the "Disable authentication" button to turn off auth directly.
        await page.getByRole('button', { name: 'Disable authentication' }).click()
        await page.getByRole('button', { name: 'Confirm' }).click()
        await page.waitForTimeout(2000)
        return
      }

      // UsersLogin or PassLogin is shown. Click the admin user card first
      // so the password field appears (no-op if PassLogin is already shown).
      // 1. Wait a maximum of 5 seconds to see if the UsersLogin card view is rendered
      const adminCard = page.getByText(adminUsername, { exact: false }).first()
      await adminCard.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {})

      // 2. If the card is visible, click it to reveal the password field
      if (await adminCard.isVisible()) {
        await adminCard.click()
      }

      // 3. Fill the username if it is visible (fallback for PassLogin)
      const usernameInput = page.getByLabel('Username').first()
      if (await usernameInput.isVisible()) {
        await usernameInput.fill(adminUsername)
      }

      // 4. Fill the dynamic password and log in
      await page.getByLabel('Password').first().fill(adminPassword)
      await page.getByRole('button', { name: 'Log in' }).first().click()

      // After login the app redirects to Home (/), not /accounts.
      // Wait for the redirect to settle, then navigate explicitly.
      await page.waitForURL('**/', { timeout: 10000 }).catch(() => {})
      await page.waitForTimeout(2000)
      await page.goto('http://localhost:9001/accounts')
      await page.waitForTimeout(2000)

      // Switch to the Settings tab (default tab is "Users" when logged in)
      await page.getByRole('tab', { name: 'Settings' }).click()

      // Click the first "Disable" button (authentication service toggle).
      await page.getByRole('button', { name: 'Disable' }).first().click()

      // Confirm the modal
      await page.getByRole('button', { name: 'Confirm' }).click()
      await page.waitForTimeout(2000)
    }
    finally {
      await page.close()
      await context.close()
      await browser.close()
    }
  })

  test('should be able to navigate to the Accounts page', async ({ page }) => {
    await page.goto('/')

    await page.getByText('Manage accounts').click()

    await expect(page).toHaveURL(/.*accounts/)
  })

  test('should enable authentication and create the first admin user', async ({ page }) => {
    // Navigate to the accounts page where auth is currently disabled
    await page.goto('/accounts')

    // Wait for the settings panel to be ready, then click "Enable"
    await expect(page.getByRole('button', { name: 'Enable' })).toBeVisible({ timeout: 10000 })
    await page.getByRole('button', { name: 'Enable' }).click()

    // The backend restarts its auth context after the toggle, which drops
    // the WebSocket connection. The React frontend transitions to the Login
    // route but gets stuck waiting for user data over the dead socket,
    // leaving a blank form. A short buffer followed by a hard reload forces
    // the app to establish a fresh WebSocket and render correctly.
    await page.waitForTimeout(2000)
    await page.reload({ waitUntil: 'networkidle' })

    // After enabling auth, the app transitions to the Login page.
    // Because no admin user exists yet, the NoAdminLogin form is shown,
    // which has a "Create" button instead of "Log in".
    // Wait for the Create button to confirm the NoAdminLogin form is rendered.
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible({ timeout: 15000 })

    // Fill out the initial admin user creation form (NoAdminLogin)
    await page.getByLabel('Username').fill(adminUsername)
    await page.getByLabel('Password').fill(adminPassword)

    // Submit the form to create the first admin and log in
    await page.getByRole('button', { name: 'Create' }).click()

    // After successful creation, the app should log us in and show the home page.
    // Wait for navigation to settle by checking for a known navbar element.
    await expect(page.getByText('Manage accounts')).toBeVisible({ timeout: 15000 })
  })

  test('should be able to create a new user', async ({ page }) => {
    // Navigate to accounts; auth is now enabled server-side, so we must log in
    await page.goto('/accounts')
    await loginAsAdmin(page)

    // Navigate to accounts after login
    await page.goto('/accounts')

    // Click the "Users" tab
    await page.getByRole('tab', { name: 'Users' }).click()

    // Open the "New User" accordion item to reveal the creation form
    await page.getByText('New User').click()

    // Fill out the new user form — scope to the 'New User' accordion panel
    // to avoid strict mode violations from identically-labeled fields.
    await page.getByLabel('New User').getByLabel('Username').fill('playwright_user')
    await page.getByLabel('New User').getByLabel('Password').fill('pw_user_123')
    await page.getByLabel('New User').getByLabel('Display name').fill('Playwright Test User')

    // Submit the form
    await page.getByLabel('New User').getByRole('button', { name: 'Create' }).click()

    // Assert the success notification appears
    await expect(page.getByText('New user was created successfully')).toBeVisible({ timeout: 10000 })
  })

  test('should be able to create a new group', async ({ page }) => {
    // Navigate to accounts; auth is now enabled server-side, so we must log in
    await page.goto('/accounts')
    await loginAsAdmin(page)

    // Navigate to accounts after login
    await page.goto('/accounts')

    // Click the "Groups" tab
    await page.getByRole('tab', { name: 'Groups' }).click()

    // Open the "New Group" accordion item to reveal the creation form
    await page.getByText('New Group').click()

    // Fill out the display name for the new group — scope to the 'New Group'
    // accordion panel to avoid strict mode violations from other items.
    await page.getByLabel('New Group').getByLabel('Display name').fill('Playwright Test Group')

    // Submit the form
    await page.getByLabel('New Group').getByRole('button', { name: 'Create' }).click()

    // Assert the success notification appears
    await expect(page.getByText('New group was created successfully')).toBeVisible({ timeout: 10000 })
  })
})
