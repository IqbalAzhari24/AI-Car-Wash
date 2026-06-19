import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import type { Role } from '../types';

// ─── Dev quick-login accounts (loaded from .env.local only in dev mode) ──────
const DEV_ACCOUNTS: { label: string; email: string; password: string; role: string }[] =
  import.meta.env.DEV
    ? [
        { label: 'Owner',    role: 'OWNER',    email: import.meta.env.VITE_DEV_OWNER_EMAIL    ?? '', password: import.meta.env.VITE_DEV_OWNER_PASSWORD    ?? '' },
        { label: 'Clerk',    role: 'CLERK',    email: import.meta.env.VITE_DEV_CLERK_EMAIL     ?? '', password: import.meta.env.VITE_DEV_CLERK_PASSWORD     ?? '' },
        { label: 'Worker',   role: 'WORKER',   email: import.meta.env.VITE_DEV_WORKER_EMAIL    ?? '', password: import.meta.env.VITE_DEV_WORKER_PASSWORD    ?? '' },
        { label: 'Customer', role: 'CUSTOMER', email: import.meta.env.VITE_DEV_CUSTOMER_EMAIL ?? '', password: import.meta.env.VITE_DEV_CUSTOMER_PASSWORD  ?? '' },
      ]
    : [];

// ─── Role → destination map (all 4 roles from CLAUDE.md) ────────────────────
const ROLE_DESTINATION: Record<Role, string> = {
  OWNER:    '/admin/users',
  CLERK:    '/clerk',
  WORKER:   '/worker',
  CUSTOMER: '/',
};

// ─── Eye icon (show / hide password) ─────────────────────────────────────────
const EyeIcon: React.FC<{ open: boolean }> = ({ open }) =>
  open ? (
    // Eye-slash (hide)
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
      strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88" />
    </svg>
  ) : (
    // Eye (show)
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
      strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
    </svg>
  );

// ─── Spinner ──────────────────────────────────────────────────────────────────
const Spinner: React.FC = () => (
  <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
    <path className="opacity-75" fill="currentColor"
      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
  </svg>
);

// ─── AzureWash wordmark ───────────────────────────────────────────────────────
const Wordmark: React.FC = () => (
  <div className="flex items-center gap-2.5" aria-label="AzureWash">
    {/* Hex logo mark */}
    <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <polygon
        points="16,2 29,9 29,23 16,30 3,23 3,9"
        fill="none"
        stroke="#00F0FF"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      <polygon
        points="16,8 24,12.5 24,21.5 16,26 8,21.5 8,12.5"
        fill="rgba(0,240,255,0.08)"
        stroke="rgba(0,240,255,0.35)"
        strokeWidth="1"
        strokeLinejoin="round"
      />
      {/* Water drop */}
      <path
        d="M16 11 C16 11 12 15.5 12 18 C12 20.2 13.8 22 16 22 C18.2 22 20 20.2 20 18 C20 15.5 16 11 16 11Z"
        fill="#00F0FF"
        fillOpacity="0.9"
      />
    </svg>
    <span className="font-display font-bold text-xl tracking-tight"
      style={{ color: '#E8E8F0' }}>
      Azure<span style={{ color: '#00F0FF' }}>Wash</span>
    </span>
  </div>
);

// ─── Login component ──────────────────────────────────────────────────────────
export const Login: React.FC = () => {
  const [email, setEmail]           = useState('');
  const [password, setPassword]     = useState('');
  const [showPassword, setShowPass] = useState(false);
  const [error, setError]           = useState('');
  const [loading, setLoading]       = useState(false);
  const { login }                   = useAuth();
  const navigate                    = useNavigate();

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
      navigate(ROLE_DESTINATION[res.data.role] ?? '/', { replace: true });
    } catch (err: any) {
      setError(
        err?.response?.status === 401
          ? 'Invalid email or password.'
          : 'Unable to sign in. Please try again.',
      );
    } finally {
      setLoading(false);
    }
  };

  const fillDevAccount = (acct: (typeof DEV_ACCOUNTS)[number]) => {
    setEmail(acct.email);
    setPassword(acct.password);
    setError('');
  };

  return (
    /* Full-screen centred layout — dark base with dense dot-grid background */
    <div
      className="hex-grid-dense flex min-h-dvh items-center justify-center px-4 py-12"
      style={{ backgroundColor: '#0D0D11' }}
    >
      {/* Skip-to-main for keyboard users */}
      <a
        href="#login-form"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4
          focus:z-50 focus:rounded-lg focus:px-3 focus:py-2
          focus:text-sm focus:font-medium focus:shadow-cyan-glow"
        style={{ backgroundColor: '#00F0FF', color: '#0D0D11' }}
      >
        Skip to sign-in form
      </a>

      <div className="w-full max-w-md space-y-4">

        {/* ── Card ──────────────────────────────────────────────────────── */}
        <div className="hex-grid hex-border hex-corner rounded-2xl p-8">

          {/* Header */}
          <div className="mb-8">
            <Wordmark />
            <h1 className="mt-6 font-display font-bold text-2xl leading-tight"
              style={{ color: '#E8E8F0' }}>
              Welcome back
            </h1>
            <p className="mt-1.5 text-sm leading-relaxed" style={{ color: '#9090A8' }}>
              Sign in to book a wash or manage the shop.
            </p>
          </div>

          {/* Form */}
          <form id="login-form" className="space-y-5" onSubmit={handleLogin} noValidate>

            {/* Error banner */}
            {error && (
              <div
                role="alert"
                aria-live="assertive"
                className="hex-border-danger flex items-start gap-2.5 rounded-xl px-4 py-3"
                style={{ backgroundColor: 'rgba(255,68,102,0.08)' }}
              >
                {/* Warning icon */}
                <svg className="mt-0.5 w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24"
                  stroke="#FF4466" strokeWidth={2} aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round"
                    d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
                </svg>
                <p className="text-sm font-medium" style={{ color: '#FF4466' }}>{error}</p>
              </div>
            )}

            {/* Email */}
            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="login-email"
                className="text-xs font-medium uppercase tracking-wide"
                style={{ color: '#9090A8' }}
              >
                Email address
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                required
                aria-required="true"
                aria-describedby={error ? 'login-error' : undefined}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="
                  w-full min-h-[44px] rounded-lg px-4 py-2.5 text-sm
                  border transition-all duration-150
                  placeholder:opacity-40
                  focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2
                "
                style={{
                  backgroundColor: '#13131A',
                  borderColor: '#2A2A3D',
                  color: '#E8E8F0',
                  // focus styles applied via CSS class below
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = '#00F0FF';
                  e.currentTarget.style.boxShadow   = '0 0 12px rgba(0,240,255,0.20), 0 0 24px rgba(0,240,255,0.08)';
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = '#2A2A3D';
                  e.currentTarget.style.boxShadow   = '';
                }}
              />
            </div>

            {/* Password */}
            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="login-password"
                className="text-xs font-medium uppercase tracking-wide"
                style={{ color: '#9090A8' }}
              >
                Password
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  aria-required="true"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="
                    w-full min-h-[44px] rounded-lg px-4 py-2.5 pr-12 text-sm
                    border transition-all duration-150
                    placeholder:opacity-40
                    focus:outline-none
                  "
                  style={{
                    backgroundColor: '#13131A',
                    borderColor: '#2A2A3D',
                    color: '#E8E8F0',
                  }}
                  onFocus={(e) => {
                    e.currentTarget.style.borderColor = '#00F0FF';
                    e.currentTarget.style.boxShadow   = '0 0 12px rgba(0,240,255,0.20), 0 0 24px rgba(0,240,255,0.08)';
                  }}
                  onBlur={(e) => {
                    e.currentTarget.style.borderColor = '#2A2A3D';
                    e.currentTarget.style.boxShadow   = '';
                  }}
                />
                {/* Show / hide toggle — meets 44×44 touch target */}
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  className="
                    absolute right-0 top-0 bottom-0 flex items-center justify-center
                    w-11 h-full rounded-r-lg
                    transition-colors duration-150
                    focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset
                  "
                  style={{ color: '#5A5A72' }}
                  onMouseEnter={(e) => { e.currentTarget.style.color = '#9090A8'; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = '#5A5A72'; }}
                >
                  <EyeIcon open={showPassword} />
                </button>
              </div>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading || !email || !password}
              className="
                w-full inline-flex items-center justify-center gap-2
                min-h-[44px] rounded-lg px-5 py-2.5
                text-sm font-medium font-sans
                border transition-all duration-150
                active:scale-[0.98]
                focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2
                disabled:opacity-40 disabled:pointer-events-none disabled:cursor-not-allowed
              "
              style={{
                borderColor:     loading ? 'rgba(0,240,255,0.40)' : '#00F0FF',
                color:           loading ? '#00B8C4' : '#00F0FF',
                backgroundColor: 'transparent',
                boxShadow:       loading ? '' : '0 0 12px rgba(0,240,255,0.20), 0 0 24px rgba(0,240,255,0.08)',
                // ring-offset uses base color
                '--tw-ring-color': '#00F0FF',
                '--tw-ring-offset-color': '#0D0D11',
              } as React.CSSProperties}
              onMouseEnter={(e) => {
                if (!loading) {
                  e.currentTarget.style.backgroundColor = '#00F0FF';
                  e.currentTarget.style.color           = '#0D0D11';
                }
              }}
              onMouseLeave={(e) => {
                if (!loading) {
                  e.currentTarget.style.backgroundColor = 'transparent';
                  e.currentTarget.style.color           = '#00F0FF';
                }
              }}
            >
              {loading ? (
                <>
                  <Spinner />
                  Signing in…
                </>
              ) : (
                'Sign in'
              )}
            </button>
          </form>
        </div>

        {/* ── Dev quick-login panel (development mode only) ───────────────── */}
        {import.meta.env.DEV && DEV_ACCOUNTS.some((a) => a.email) && (
          <div
            className="rounded-2xl p-5"
            style={{
              border: '1px dashed rgba(61,74,107,0.7)',
              backgroundColor: 'rgba(19,19,26,0.6)',
            }}
          >
            <p
              className="text-xs font-medium uppercase tracking-wide mb-3"
              style={{ color: '#5A5A72' }}
            >
              Dev quick login
            </p>
            <div className="grid grid-cols-2 gap-2">
              {DEV_ACCOUNTS.map((acct) => (
                <button
                  key={acct.label}
                  type="button"
                  onClick={() => fillDevAccount(acct)}
                  className="
                    min-h-[44px] rounded-lg px-3 py-2
                    text-xs font-medium text-left
                    border transition-all duration-150 active:scale-[0.97]
                    focus-visible:outline-none focus-visible:ring-2
                  "
                  style={{
                    borderColor:     '#2A2A3D',
                    backgroundColor: '#13131A',
                    color:           '#9090A8',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor     = 'rgba(0,240,255,0.25)';
                    e.currentTarget.style.color           = '#E8E8F0';
                    e.currentTarget.style.backgroundColor = '#1A1A24';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor     = '#2A2A3D';
                    e.currentTarget.style.color           = '#9090A8';
                    e.currentTarget.style.backgroundColor = '#13131A';
                  }}
                >
                  <span
                    className="block font-semibold text-xs"
                    style={{ color: '#00F0FF' }}
                  >
                    {acct.label}
                  </span>
                  <span className="block truncate text-[11px] font-mono mt-0.5"
                    style={{ color: '#5A5A72' }}>
                    {acct.email || '—'}
                  </span>
                </button>
              ))}
            </div>
            <p className="mt-3 text-[11px]" style={{ color: '#5A5A72' }}>
              Sets credentials — still requires clicking Sign in.
            </p>
          </div>
        )}

        {/* ── Footer ─────────────────────────────────────────────────────── */}
        <p className="text-center text-xs" style={{ color: '#5A5A72' }}>
          AzureWash · Final Year Project · UMT
        </p>
      </div>
    </div>
  );
};
