import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/api';
import { btnPrimary, btnGhost, cardPanel, inputBase } from '../components/ui';
import { MY_PHONE_RE, normalizePhone } from '../components/Login';
import { fmtDate } from '../utils/format';

interface Profile {
  id: string;
  email: string;
  phoneNumber: string | null;
  role: string;
  createdAt: string;
}

export const Account: React.FC = () => {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [phone, setPhone] = useState('');
  const [savingPhone, setSavingPhone] = useState(false);
  const [phoneMsg, setPhoneMsg] = useState<string | null>(null);
  const [phoneErr, setPhoneErr] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [savingPw, setSavingPw] = useState(false);
  const [pwMsg, setPwMsg] = useState<string | null>(null);
  const [pwErr, setPwErr] = useState<string | null>(null);

  useEffect(() => {
    api.get<Profile>('/v1/users/me')
      .then((r) => { setProfile(r.data); setPhone(r.data.phoneNumber ?? ''); })
      .catch(() => setLoadError('Could not load your account. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  async function savePhone(e: React.FormEvent) {
    e.preventDefault();
    if (savingPhone) return;
    setPhoneErr(null);
    setPhoneMsg(null);
    const normalized = phone.trim() ? normalizePhone(phone) : '';
    if (normalized && !MY_PHONE_RE.test(normalized)) {
      setPhoneErr('Phone number must be a valid Malaysian number starting with +60 (e.g. +60123456789).');
      return;
    }
    setSavingPhone(true);
    try {
      const res = await api.patch<Profile>('/v1/users/me', { phoneNumber: normalized || null });
      setProfile(res.data);
      setPhone(res.data.phoneNumber ?? '');
      setPhoneMsg('Phone number updated.');
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setPhoneErr(ax.response?.data?.message ?? 'Could not update phone number. Please try again.');
    } finally {
      setSavingPhone(false);
    }
  }

  async function changePassword(e: React.FormEvent) {
    e.preventDefault();
    if (savingPw) return;
    setPwErr(null);
    setPwMsg(null);
    if (newPassword.length < 8) {
      setPwErr('New password must be at least 8 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPwErr('Passwords do not match.');
      return;
    }
    setSavingPw(true);
    try {
      await api.post('/v1/users/me/change-password', { currentPassword, newPassword });
      setPwMsg('Password updated.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setPwErr(ax.response?.data?.message ?? 'Could not update password. Please try again.');
    } finally {
      setSavingPw(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto w-full max-w-2xl space-y-4 px-4 py-8" aria-busy="true">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-32 animate-pulse rounded-2xl bg-surface-raised" />
        ))}
      </div>
    );
  }

  if (loadError || !profile) {
    return (
      <div className="mx-auto w-full max-w-2xl px-4 py-16 text-center">
        <p role="alert" className="text-sm text-danger">{loadError ?? 'Account not found.'}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-2xl space-y-6 px-4 py-8">
      <header>
        <h1 className="font-display text-2xl font-semibold tracking-tight text-primary">My Account</h1>
        <p className="mt-1 text-sm text-secondary">Manage your profile and password.</p>
      </header>

      {/* Profile summary */}
      <div className={`${cardPanel} hex-corner p-5`}>
        <dl className="space-y-2.5 text-sm">
          <div className="flex justify-between gap-2">
            <dt className="text-secondary">Email</dt>
            <dd className="font-medium text-primary">{profile.email}</dd>
          </div>
          <div className="flex justify-between gap-2">
            <dt className="text-secondary">Role</dt>
            <dd className="font-medium capitalize text-primary">{profile.role.toLowerCase()}</dd>
          </div>
          <div className="flex justify-between gap-2">
            <dt className="text-secondary">Joined</dt>
            <dd className="font-medium text-primary">{fmtDate(profile.createdAt)}</dd>
          </div>
        </dl>
      </div>

      {/* Phone number */}
      <form onSubmit={savePhone} className={`${cardPanel} hex-corner space-y-3 p-5`}>
        <label htmlFor="account-phone" className="block text-sm font-semibold text-primary">
          Phone number
        </label>
        <input
          id="account-phone"
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="+60123456789"
          className={inputBase}
        />
        {phoneMsg && <p className="text-sm text-success">{phoneMsg}</p>}
        {phoneErr && <p role="alert" className="text-sm text-danger">{phoneErr}</p>}
        <button type="submit" disabled={savingPhone} className={btnPrimary}>
          {savingPhone ? 'Saving…' : 'Save phone number'}
        </button>
      </form>

      {/* Change password */}
      <form onSubmit={changePassword} className={`${cardPanel} hex-corner space-y-3 p-5`}>
        <p className="text-sm font-semibold text-primary">Change password</p>
        <input
          type="password"
          autoComplete="current-password"
          placeholder="Current password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          className={inputBase}
        />
        <input
          type="password"
          autoComplete="new-password"
          placeholder="New password (min. 8 characters)"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          className={inputBase}
        />
        <input
          type="password"
          autoComplete="new-password"
          placeholder="Confirm new password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          className={inputBase}
        />
        {pwMsg && <p className="text-sm text-success">{pwMsg}</p>}
        {pwErr && <p role="alert" className="text-sm text-danger">{pwErr}</p>}
        <button
          type="submit"
          disabled={savingPw || !currentPassword || !newPassword || !confirmPassword}
          className={btnPrimary}
        >
          {savingPw ? 'Updating…' : 'Update password'}
        </button>
      </form>

      {profile.role === 'CUSTOMER' && (
        <Link to="/bookings" className={`${btnGhost} w-full`}>
          View booking history
        </Link>
      )}
    </div>
  );
};
