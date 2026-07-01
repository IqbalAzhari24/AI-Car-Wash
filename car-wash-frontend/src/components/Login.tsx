import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import type { Role } from '../types';

const ROLE_DESTINATION: Record<Role, string> = {
  OWNER:    '/admin/users',
  CLERK:    '/clerk',
  WORKER:   '/worker',
  CUSTOMER: '/',
};

const EyeIcon: React.FC<{ open: boolean }> = ({ open }) =>
  open ? (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
      strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88" />
    </svg>
  ) : (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
      strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
    </svg>
  );

// ─── AICarWash wordmark ───────────────────────────────────────────────────────
const Wordmark: React.FC = () => (
  <div className="flex items-center gap-2.5" aria-label="AICarWash">
    <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <polygon points="16,2 29,9 29,23 16,30 3,23 3,9" fill="none" stroke="#00F0FF" strokeWidth="1.5" strokeLinejoin="round" />
      <polygon points="16,8 24,12.5 24,21.5 16,26 8,21.5 8,12.5" fill="rgba(0,240,255,0.08)" stroke="rgba(0,240,255,0.35)" strokeWidth="1" strokeLinejoin="round" />
      <path d="M16 11 C16 11 12 15.5 12 18 C12 20.2 13.8 22 16 22 C18.2 22 20 20.2 20 18 C20 15.5 16 11 16 11Z" fill="#00F0FF" fillOpacity="0.9" />
    </svg>
    <span className="font-display font-bold text-xl tracking-tight" style={{ color: '#E8E8F0' }}>
      AICarWash
    </span>
  </div>
);

const INPUT_BASE = `
  w-full min-h-[44px] rounded-lg px-4 py-2.5 text-sm
  border transition-all duration-150 placeholder:opacity-40
  focus:outline-none
`;
const INPUT_STYLE = { backgroundColor: '#13131A', borderColor: '#2A2A3D', color: '#E8E8F0' };
const onFocusGlow = (e: React.FocusEvent<HTMLInputElement>) => {
  e.currentTarget.style.borderColor = '#00F0FF';
  e.currentTarget.style.boxShadow   = '0 0 12px rgba(0,240,255,0.20), 0 0 24px rgba(0,240,255,0.08)';
};
const onBlurGlow = (e: React.FocusEvent<HTMLInputElement>) => {
  e.currentTarget.style.borderColor = '#2A2A3D';
  e.currentTarget.style.boxShadow   = '';
};

// ─── Login component ──────────────────────────────────────────────────────────
export const Login: React.FC = () => {
  const [mode, setMode] = useState<'login' | 'register'>('login');

  // login fields
  const [email, setEmail]           = useState('');
  const [password, setPassword]     = useState('');
  const [showPassword, setShowPass] = useState(false);

  // register fields
  const [regEmail, setRegEmail]           = useState('');
  const [regPassword, setRegPassword]     = useState('');
  const [regConfirm, setRegConfirm]       = useState('');
  const [regPhone, setRegPhone]           = useState('');
  const [showRegPass, setShowRegPass]     = useState(false);

  const [error, setError]     = useState('');
  const [loading, setLoading] = useState(false);
  const { login }             = useAuth();
  const navigate              = useNavigate();

  const switchMode = (m: 'login' | 'register') => {
    setMode(m);
    setError('');
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post<{ token: string; role: Role }>('/v1/auth/login', { email, password });
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

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (regPassword !== regConfirm) {
      setError('Passwords do not match.');
      return;
    }
    if (regPassword.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    setLoading(true);
    try {
      const res = await api.post<{ token: string; role: Role }>('/v1/auth/register', {
        email: regEmail,
        password: regPassword,
        phoneNumber: regPhone || undefined,
      });
      login(res.data.token);
      navigate('/', { replace: true });
    } catch (err: any) {
      if (!err?.response) {
        setError('Unable to reach the server. Please try again later.');
      } else if (err.response.status === 409) {
        setError('An account with that email already exists.');
      } else {
        const body = err.response.data;
        const msg = body?.message || body?.detail || body?.error;
        setError(msg || 'Registration failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="hex-grid-dense flex min-h-dvh items-center justify-center px-4 py-12"
      style={{ backgroundColor: '#0D0D11' }}
    >
      <a
        href="#auth-form"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:rounded-lg focus:px-3 focus:py-2 focus:text-sm focus:font-medium focus:shadow-cyan-glow"
        style={{ backgroundColor: '#00F0FF', color: '#0D0D11' }}
      >
        Skip to form
      </a>

      <div className="w-full max-w-md space-y-4">
        <div className="hex-grid hex-border hex-corner rounded-2xl p-8">

          {/* Header */}
          <div className="mb-6">
            <Wordmark />
            <h1 className="mt-6 font-display font-bold text-2xl leading-tight" style={{ color: '#E8E8F0' }}>
              {mode === 'login' ? 'Welcome back' : 'Create your account'}
            </h1>
            <p className="mt-1.5 text-sm leading-relaxed" style={{ color: '#9090A8' }}>
              {mode === 'login'
                ? 'Sign in to book a wash or manage the shop.'
                : 'Register as a customer. Staff accounts are created by the owner.'}
            </p>
          </div>

          {/* Tab switcher */}
          <div className="mb-6 flex rounded-lg overflow-hidden border" style={{ borderColor: '#2A2A3D' }}>
            {(['login', 'register'] as const).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => switchMode(m)}
                className="flex-1 py-2 text-sm font-medium transition-colors duration-150"
                style={{
                  backgroundColor: mode === m ? '#00F0FF' : 'transparent',
                  color:           mode === m ? '#0D0D11' : '#9090A8',
                }}
              >
                {m === 'login' ? 'Sign in' : 'Sign up'}
              </button>
            ))}
          </div>

          {/* Error banner */}
          {error && (
            <div
              role="alert"
              aria-live="assertive"
              className="mb-4 hex-border-danger flex items-start gap-2.5 rounded-xl px-4 py-3"
              style={{ backgroundColor: 'rgba(255,68,102,0.08)' }}
            >
              <svg className="mt-0.5 w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24"
                stroke="#FF4466" strokeWidth={2} aria-hidden="true">
                <path strokeLinecap="round" strokeLinejoin="round"
                  d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
              </svg>
              <p className="text-sm font-medium" style={{ color: '#FF4466' }}>{error}</p>
            </div>
          )}

          {/* ── LOGIN FORM ─────────────────────────────────────────────── */}
          {mode === 'login' && (
            <form id="auth-form" className="space-y-5" onSubmit={handleLogin} noValidate>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="login-email" className="text-xs font-medium uppercase tracking-wide" style={{ color: '#9090A8' }}>
                  Email address
                </label>
                <input
                  id="login-email" type="email" autoComplete="email" required
                  value={email} onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@g.com"
                  className={INPUT_BASE} style={INPUT_STYLE}
                  onFocus={onFocusGlow} onBlur={onBlurGlow}
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label htmlFor="login-password" className="text-xs font-medium uppercase tracking-wide" style={{ color: '#9090A8' }}>
                  Password
                </label>
                <div className="relative">
                  <input
                    id="login-password" type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password" required
                    value={password} onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className={INPUT_BASE + ' pr-12'} style={INPUT_STYLE}
                    onFocus={onFocusGlow} onBlur={onBlurGlow}
                  />
                  <button type="button" onClick={() => setShowPass((v) => !v)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    className="absolute right-0 top-0 bottom-0 flex items-center justify-center w-11 h-full rounded-r-lg transition-colors duration-150"
                    style={{ color: '#5A5A72' }}
                    onMouseEnter={(e) => { e.currentTarget.style.color = '#9090A8'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.color = '#5A5A72'; }}>
                    <EyeIcon open={showPassword} />
                  </button>
                </div>
              </div>

              <SubmitButton loading={loading} disabled={!email || !password} label="Sign in" loadingLabel="Signing in…" />
            </form>
          )}

          {/* ── REGISTER FORM ──────────────────────────────────────────── */}
          {mode === 'register' && (
            <form id="auth-form" className="space-y-5" onSubmit={handleRegister} noValidate>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="reg-email" className="text-xs font-medium uppercase tracking-wide" style={{ color: '#9090A8' }}>
                  Email address
                </label>
                <input
                  id="reg-email" type="email" autoComplete="email" required
                  value={regEmail} onChange={(e) => setRegEmail(e.target.value)}
                  placeholder="you@gmail.com"
                  className={INPUT_BASE} style={INPUT_STYLE}
                  onFocus={onFocusGlow} onBlur={onBlurGlow}
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label htmlFor="reg-phone" className="text-xs font-medium uppercase tracking-wide" style={{ color: '#9090A8' }}>
                  Phone number <span style={{ color: '#5A5A72' }}>(optional)</span>
                </label>
                <input
                  id="reg-phone" type="tel" autoComplete="tel"
                  value={regPhone} onChange={(e) => setRegPhone(e.target.value)}
                  placeholder="+60123456789"
                  className={INPUT_BASE} style={INPUT_STYLE}
                  onFocus={onFocusGlow} onBlur={onBlurGlow}
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label htmlFor="reg-password" className="text-xs font-medium uppercase tracking-wide" style={{ color: '#9090A8' }}>
                  Password
                </label>
                <div className="relative">
                  <input
                    id="reg-password" type={showRegPass ? 'text' : 'password'}
                    autoComplete="new-password" required
                    value={regPassword} onChange={(e) => setRegPassword(e.target.value)}
                    placeholder="Min. 8 characters"
                    className={INPUT_BASE + ' pr-12'} style={INPUT_STYLE}
                    onFocus={onFocusGlow} onBlur={onBlurGlow}
                  />
                  <button type="button" onClick={() => setShowRegPass((v) => !v)}
                    aria-label={showRegPass ? 'Hide password' : 'Show password'}
                    className="absolute right-0 top-0 bottom-0 flex items-center justify-center w-11 h-full rounded-r-lg transition-colors duration-150"
                    style={{ color: '#5A5A72' }}
                    onMouseEnter={(e) => { e.currentTarget.style.color = '#9090A8'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.color = '#5A5A72'; }}>
                    <EyeIcon open={showRegPass} />
                  </button>
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <label htmlFor="reg-confirm" className="text-xs font-medium uppercase tracking-wide" style={{ color: '#9090A8' }}>
                  Confirm password
                </label>
                <input
                  id="reg-confirm" type="password" autoComplete="new-password" required
                  value={regConfirm} onChange={(e) => setRegConfirm(e.target.value)}
                  placeholder="••••••••"
                  className={INPUT_BASE} style={INPUT_STYLE}
                  onFocus={onFocusGlow} onBlur={onBlurGlow}
                />
              </div>

              <SubmitButton
                loading={loading}
                disabled={!regEmail || !regPassword || !regConfirm}
                label="Create account"
                loadingLabel="Creating account…"
              />
            </form>
          )}
        </div>

        <p className="text-center text-xs" style={{ color: '#5A5A72' }}>
          AICarWash · Final Year Project · UMT
        </p>
      </div>
    </div>
  );
};

// ─── Shared submit button ─────────────────────────────────────────────────────
const SubmitButton: React.FC<{
  loading: boolean;
  disabled: boolean;
  label: string;
  loadingLabel: string;
}> = ({ loading, disabled, label, loadingLabel }) => (
  <button
    type="submit"
    disabled={loading || disabled}
    className="w-full inline-flex items-center justify-center gap-2 min-h-[44px] rounded-lg px-5 py-2.5 text-sm font-medium font-sans border transition-all duration-150 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:opacity-40 disabled:pointer-events-none"
    style={{
      borderColor:     loading ? 'rgba(0,240,255,0.40)' : '#00F0FF',
      color:           loading ? '#00B8C4' : '#00F0FF',
      backgroundColor: 'transparent',
      boxShadow:       loading ? '' : '0 0 12px rgba(0,240,255,0.20), 0 0 24px rgba(0,240,255,0.08)',
    } as React.CSSProperties}
    onMouseEnter={(e) => {
      if (!loading) {
        e.currentTarget.style.backgroundColor = '#00F0FF';
        e.currentTarget.style.color = '#0D0D11';
      }
    }}
    onMouseLeave={(e) => {
      if (!loading) {
        e.currentTarget.style.backgroundColor = 'transparent';
        e.currentTarget.style.color = '#00F0FF';
      }
    }}
  >
    {loading ? (
      <>
        <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        {loadingLabel}
      </>
    ) : label}
  </button>
);
