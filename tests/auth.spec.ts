import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('login page loads', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
  });

  test('invalid credentials shows error message', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('nobody@example.com');
    await page.getByLabel(/password/i).fill('wrongpassword');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.getByText(/invalid email or password/i)).toBeVisible({ timeout: 8_000 });
  });

  test('unauthenticated visit to /book redirects to login or landing', async ({ page }) => {
    await page.goto('/book');
    // ProtectedRoute redirects unauthenticated users away from /book
    await expect(page).not.toHaveURL(/\/book/, { timeout: 5_000 });
  });
});
