import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('login page loads', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
  });

  test('invalid credentials shows error message', async ({ page }) => {
    // CI has no backend — stub the real 401 rejection path (same route-mock
    // pattern the other specs use) so this stays deterministic.
    await page.route('**/v1/auth/login', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Invalid email or password.' }),
      })
    );
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('nobody@example.com');
    // exact: the show/hide toggle's aria-label ("Show password") also matches /password/i
    await page.getByLabel('Password', { exact: true }).fill('wrongpassword');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.getByText(/invalid email or password/i)).toBeVisible({ timeout: 8_000 });
  });

  test('unauthenticated visit to /book redirects to login or landing', async ({ page }) => {
    await page.goto('/book');
    // ProtectedRoute redirects unauthenticated users away from /book
    await expect(page).not.toHaveURL(/\/book/, { timeout: 5_000 });
  });
});
