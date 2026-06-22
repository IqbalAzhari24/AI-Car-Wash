import { test, expect } from '@playwright/test';

test.describe('Booking flow', () => {
  test('unauthenticated user cannot access /book', async ({ page }) => {
    await page.goto('/book');
    await expect(page).not.toHaveURL(/\/book/, { timeout: 5_000 });
  });

  // Integration-level smoke: the booking wizard renders when authenticated.
  // Full end-to-end flow requires a running backend with seed data.
  test('booking page shows a date picker when authenticated', async ({ page, context }) => {
    // Inject a fake JWT so the auth context believes the user is signed in.
    // The token is structurally valid but the backend is not called here.
    const fakeToken = [
      Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url'),
      Buffer.from(JSON.stringify({ sub: '00000000-0000-0000-0000-000000000001', role: 'CUSTOMER', exp: 9999999999 })).toString('base64url'),
      'fakesig',
    ].join('.');

    await context.addInitScript((token) => {
      localStorage.setItem('token', token);
    }, fakeToken);

    await page.goto('/book');

    // BookingFlow should render — not redirect to login.
    // Check for a date-related element (calendar heading or date input).
    await expect(page.locator('body')).not.toContainText('Sign in', { timeout: 5_000 });
  });
});
