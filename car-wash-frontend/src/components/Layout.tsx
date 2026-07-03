import React, { useState } from 'react';
import { Outlet, NavLink, Link, useLocation } from 'react-router-dom';
import { Menu, X, Bell } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useUpdateToasts } from '../hooks/useUpdateToasts';
import type { Role } from '../types';

const Wordmark: React.FC = () => (
  <div className="flex items-center gap-2.5" aria-label="AICarWash">
    <svg width="28" height="28" viewBox="0 0 32 32" fill="none" aria-hidden="true">
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
      <path
        d="M16 11 C16 11 12 15.5 12 18 C12 20.2 13.8 22 16 22 C18.2 22 20 20.2 20 18 C20 15.5 16 11 16 11Z"
        fill="#00F0FF"
        fillOpacity="0.9"
      />
    </svg>
    <span className="font-display font-bold text-lg tracking-tight text-primary">
      AICarWash
    </span>
  </div>
);

interface NavItem {
  to: string;
  label: string;
  roles?: Role[];
}

const ALL_ROLES: Role[] = ['CUSTOMER', 'CLERK', 'WORKER', 'OWNER'];

const NAV_ITEMS: NavItem[] = [
  { to: '/',                label: 'Home' },
  { to: '/book',            label: 'Book a Wash',    roles: ['CUSTOMER'] },
  { to: '/bookings',        label: 'My Bookings',    roles: ['CUSTOMER'] },
  { to: '/valet',           label: 'Valet Pick-up',  roles: ['CUSTOMER'] },
  { to: '/chat',            label: 'Chat with Timah',roles: ['CUSTOMER'] },
  { to: '/clerk',           label: 'Console',        roles: ['CLERK'] },
  { to: '/clerk/valet',     label: 'Valet Requests', roles: ['CLERK'] },
  { to: '/worker',          label: 'My Jobs',        roles: ['WORKER'] },
  { to: '/admin/analytics', label: 'Analytics',      roles: ['OWNER'] },
  { to: '/admin/users',     label: 'Users',          roles: ['OWNER'] },
  { to: '/account',         label: 'Account',        roles: ALL_ROLES },
];

export const Layout: React.FC = () => {
  const { isAuthenticated, logout, user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const { toasts, dismiss } = useUpdateToasts();

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role as Role))
  );

  const isChat = location.pathname === '/chat';
  const closeMenu = () => setMenuOpen(false);

  return (
    <div className={`flex flex-col bg-base ${isChat ? 'h-dvh' : 'min-h-dvh'}`}>
      {/* Top navbar */}
      <nav className="border-b border-border bg-base/90 backdrop-blur-md safe-top">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-14 items-center justify-between">

            {/* Brand */}
            <Link to="/" onClick={closeMenu} className="flex-shrink-0 rounded-lg">
              <Wordmark />
            </Link>

            {/* Desktop nav links */}
            <div className="hidden sm:flex sm:items-center sm:gap-1">
              {visibleItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `min-h-[44px] inline-flex items-center px-3 rounded-lg text-sm font-medium transition-colors duration-150 ${
                      isActive
                        ? 'text-cyan bg-cyan/10'
                        : 'text-secondary hover:text-primary hover:bg-surface-hover'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>

            {/* Desktop right controls */}
            <div className="hidden sm:flex sm:items-center sm:gap-3">
              {isAuthenticated && user && (
                <span className="rounded-full border border-border px-2.5 py-0.5 text-xs font-medium capitalize text-muted">
                  {user.role.toLowerCase()}
                </span>
              )}
              {isAuthenticated ? (
                <button
                  type="button"
                  onClick={logout}
                  className="min-h-[44px] inline-flex items-center rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-secondary transition-colors duration-150 hover:border-slate hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base"
                >
                  Log out
                </button>
              ) : (
                <Link
                  to="/login"
                  className="min-h-[44px] inline-flex items-center rounded-lg border border-cyan px-4 py-2 text-sm font-medium text-cyan shadow-cyan-glow transition-all duration-150 hover:bg-cyan hover:text-base focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base"
                >
                  Sign in
                </Link>
              )}
            </div>

            {/* Mobile menu toggle */}
            <button
              type="button"
              onClick={() => setMenuOpen((o) => !o)}
              aria-expanded={menuOpen}
              aria-controls="mobile-menu"
              aria-label={menuOpen ? 'Close menu' : 'Open menu'}
              className="sm:hidden min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-lg text-secondary hover:bg-surface-hover hover:text-primary transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan"
            >
              {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {menuOpen && (
          <div id="mobile-menu" className="border-t border-border-subtle px-4 pb-4 pt-2 sm:hidden">
            <div className="space-y-1">
              {visibleItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={closeMenu}
                  className={({ isActive }) =>
                    `block min-h-[44px] flex items-center rounded-lg px-3 text-base font-medium transition-colors duration-150 ${
                      isActive
                        ? 'bg-cyan/10 text-cyan'
                        : 'text-secondary hover:bg-surface-hover hover:text-primary'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
            <div className="mt-3 border-t border-border-subtle pt-3">
              {isAuthenticated && user && (
                <p className="mb-2 px-3 text-xs capitalize text-muted">
                  Signed in as {user.role.toLowerCase()}
                </p>
              )}
              {isAuthenticated ? (
                <button
                  type="button"
                  onClick={() => { closeMenu(); logout(); }}
                  className="block w-full min-h-[44px] rounded-lg px-3 text-left text-base font-medium text-secondary transition-colors duration-150 hover:bg-surface-hover hover:text-primary"
                >
                  Log out
                </button>
              ) : (
                <Link
                  to="/login"
                  onClick={closeMenu}
                  className="block min-h-[44px] flex items-center justify-center rounded-lg border border-cyan px-3 text-center text-base font-medium text-cyan transition-all duration-150 hover:bg-cyan hover:text-base"
                >
                  Sign in
                </Link>
              )}
            </div>
          </div>
        )}
      </nav>

      <main className={`flex min-h-0 flex-1 flex-col ${isChat ? 'overflow-hidden' : ''}`}>
        <Outlet />
      </main>

      {/* Live status toasts (STOMP /user/queue/updates) */}
      {toasts.length > 0 && (
        <div className="fixed bottom-4 right-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2" role="status" aria-live="polite">
          {toasts.map((t) => (
            <div
              key={t.id}
              className="flex items-start gap-3 rounded-xl border border-cyan/40 bg-surface-raised p-3.5 shadow-cyan-glow"
            >
              <Bell className="mt-0.5 h-4 w-4 flex-shrink-0 text-cyan" aria-hidden="true" />
              <div className="min-w-0 flex-1">
                <p className="text-[10px] font-semibold uppercase tracking-widest text-cyan">
                  {t.kind === 'VALET' ? 'Valet update' : 'Booking update'}
                </p>
                <p className="mt-0.5 text-sm text-primary">{t.message}</p>
              </div>
              <button
                type="button"
                onClick={() => dismiss(t.id)}
                aria-label="Dismiss notification"
                className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded text-muted transition-colors hover:text-primary"
              >
                <X className="h-3.5 w-3.5" aria-hidden="true" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
