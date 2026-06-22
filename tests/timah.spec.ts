import { test, expect } from '@playwright/test';

test.describe('Timah chat widget', () => {
  test('unauthenticated user cannot access /chat', async ({ page }) => {
    await page.goto('/chat');
    await expect(page).not.toHaveURL(/\/chat/, { timeout: 5_000 });
  });

  test('chat page loads when authenticated', async ({ page, context }) => {
    const fakeToken = [
      Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url'),
      Buffer.from(JSON.stringify({ sub: '00000000-0000-0000-0000-000000000001', role: 'CUSTOMER', exp: 9999999999 })).toString('base64url'),
      'fakesig',
    ].join('.');

    await context.addInitScript((token) => {
      localStorage.setItem('token', token);
    }, fakeToken);

    await page.goto('/chat');

    // Timah chat should render a message input — not redirect to login
    await expect(page.locator('body')).not.toContainText('Sign in', { timeout: 5_000 });
  });

  test('rate limit error message appears on 429 response', async ({ page, context }) => {
    const fakeToken = [
      Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url'),
      Buffer.from(JSON.stringify({ sub: '00000000-0000-0000-0000-000000000001', role: 'CUSTOMER', exp: 9999999999 })).toString('base64url'),
      'fakesig',
    ].join('.');

    await context.addInitScript((token) => {
      localStorage.setItem('token', token);
    }, fakeToken);

    // Intercept the chat API and return 429 to simulate rate limit exhaustion.
    await page.route('**/api/v1/chat**', (route) =>
      route.fulfill({ status: 429, body: 'Too many requests' })
    );

    await page.goto('/chat');

    // Type a message and submit to trigger the API call.
    const input = page.locator('textarea, input[type="text"]').first();
    await input.fill('Hello Timah');
    await input.press('Enter');

    // A user-friendly rate-limit message must surface.
    await expect(page.getByText(/too many|limit|please wait|try again/i)).toBeVisible({ timeout: 8_000 });
  });
});
