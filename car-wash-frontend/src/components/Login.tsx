import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import type { Role } from '../types';

const DEV_ACCOUNTS: { label: string; email: string; password: string }[] = import.meta.env.DEV
  ? [
      { label: 'Owner',    email: import.meta.env.VITE_DEV_OWNER_EMAIL    ?? '', password: import.meta.env.VITE_DEV_OWNER_PASSWORD    ?? '' },
      { label: 'Clerk 1', email: import.meta.env.VITE_DEV_CLERK_EMAIL     ?? '', password: import.meta.env.VITE_DEV_CLERK_PASSWORD     ?? '' },
      { label: 'Worker 1',email: import.meta.env.VITE_DEV_WORKER_EMAIL    ?? '', password: import.meta.env.VITE_DEV_WORKER_PASSWORD    ?? '' },
      { label: 'Customer', email: import.meta.env.VITE_DEV_CUSTOMER_EMAIL ?? '', password: import.meta.env.VITE_DEV_CUSTOMER_PASSWORD  ?? '' },
    ]
  : [];

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  // The role on the user record classifies the login: Owner -> admin directory,
  // Staff/Customer -> their dashboard.
  const destinationForRole = (role: Role): string =>
    role === 'OWNER' ? '/admin/users' : '/';

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post<{ token: string; role: Role }>('/v1/auth/login', {
        email,
        password,
      });
      login(res.data.token);
      navigate(destinationForRole(res.data.role), { replace: true });
    } catch (err: any) {
      setError(
        err?.response?.status === 401
          ? 'Invalid email or password.'
          : 'Unable to sign in. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  const setCredentials = (e: string, p: string) => {
    setEmail(e);
    setPassword(p);
  };

  const inputClass =
    'block w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-ink placeholder:text-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary';

  return (
    <div className="flex flex-1 items-center justify-center px-4 py-12 sm:px-6 lg:px-8">
      <div className="w-full max-w-md">
        <div className="rounded-2xl border border-border bg-surface p-8">
          <h1 className="text-2xl font-semibold tracking-tight text-ink">Welcome back</h1>
          <p className="mt-1 text-sm text-muted">
            Sign in to book a wash or manage the shop.
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleLogin}>
            {error && (
              <div
                className="rounded-lg border border-danger/20 bg-danger-soft p-3 text-sm text-danger-soft-ink"
                role="alert"
              >
                {error}
              </div>
            )}

            <div>
              <label htmlFor="login-email" className="mb-1.5 block text-sm font-medium text-ink">
                Email address
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                required
                className={inputClass}
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div>
              <label htmlFor="login-password" className="mb-1.5 block text-sm font-medium text-ink">
                Password
              </label>
              <input
                id="login-password"
                type="password"
                autoComplete="current-password"
                required
                className={inputClass}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-white transition-colors duration-150 hover:bg-primary-strong disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
        </div>

        {import.meta.env.DEV && (
          <div className="mt-4 rounded-2xl border border-dashed border-border p-5">
            <h2 className="text-xs font-semibold text-muted">Dev quick login</h2>
            <div className="mt-3 grid grid-cols-2 gap-2">
              {DEV_ACCOUNTS.map((acct) => (
                <button
                  key={acct.label}
                  type="button"
                  onClick={() => setCredentials(acct.email, acct.password)}
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-sm font-medium text-muted transition-colors duration-150 hover:bg-surface hover:text-ink"
                >
                  {acct.label}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
