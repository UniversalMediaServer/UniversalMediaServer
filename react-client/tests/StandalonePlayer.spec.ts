import { expect, test } from '@playwright/test'

test.use({
  baseURL: 'http://localhost:9002'
});

test('should be able to browse the Standalone Player', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/.*player/)
})
