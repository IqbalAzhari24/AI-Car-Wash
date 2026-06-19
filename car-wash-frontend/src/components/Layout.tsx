import React, { useState } from 'react';
import { Outlet, Link, NavLink, useLocation } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

const BrandMark: React.FC = () => (
  <svg viewBox="0 0 32 32" className="h-8 w-8" aria-hidden="true">
    <rect width="32" height="32" rx="8" className="fill-primary" />
    <path
      d="M16 6.5c3.6 4.6 6.5 8.2 6.5 11.6a6.5 6.5 0 1 1-13 0c0-3.4 2.9-7 6.5-11.6Z"
      fill="white"
    />
    <path
      d="M16 10.4c2.4 3.1 4.3 5.6 4.3 7.9a4.3 4.3 0 0 1-8.6 0c0-2.3 1.9-4.8 4.3-7.9Z"
      className="fill-accent"
    />
  </svg>
);

interface NavItem {
  to: string;
  label: string;
  roles?: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard' },
  { to: '/book', label: 'Book a Wash', roles: ['CUSTOMER'] },
  { to: '/chat', label: 'Chat with Timah', roles: ['CUSTOMER'] },
  { to: '/admin/analytics', label: 'Analytics', roles: ['OWNER'] },
  { to: '/admin/users', label: 'Users', roles: ['OWNER'] },
];

const desktopLinkClass = ({ isActive }: { isActive: boolean }): string =>
  `inline-flex items-center border-b-2 px-1 pt-1 text-sm font-medium transition-colors duration-150 ${
    isActive
      ? 'border-primary text-ink'
      : 'border-transparent text-muted hover:border-border hover:text-ink'
  }`;

const mobileLinkClass = ({ isActive }: { isActive: boolean }): string =>
  `block rounded-lg px-3 py-2 text-base font-medium ${
    isActive ? 'bg-primary-soft text-primary-soft-ink' : 'text-muted hover:bg-surface hover:text-ink'
  }`;

export const Layout: React.FC = () => {
  const { isAuthenticated, logout, user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role as Role))
  );

  // The chat fills the remaining viewport; everything else scrolls normally.
  const isChat = location.pathname === '/chat';

  const closeMenu = () => setMenuOpen(false);

  return (
    <div className={`flex flex-col bg-bg ${isChat ? 'h-dvh' : 'min-h-dvh'}`}>
      <nav className="border-b border-border bg-bg">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 justify-between">
            <div className="flex">
              <Link
                to="/"
                onClick={closeMenu}
                className="flex flex-shrink-0 items-center gap-2.5 rounded-lg"
              >
                <BrandMark />
                <span className="text-lg font-semibold tracking-tight text-ink">
                  Timah Wash
                </span>
              </Link>
              <div className="hidden sm:ml-8 sm:flex sm:gap-6">
                {visibleItems.map((item) => (
                  <NavLink key={item.to} to={item.to} className={desktopLinkClass}>
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </div>

            <div className="hidden sm:flex sm:items-center sm:gap-3">
              {isAuthenticated && user && (
                <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-xs font-medium capitalize text-muted">
                  {user.role.toLowerCase()}
                </span>
              )}
              {isAuthenticated ? (
                <button
                  type="button"
                  onClick={logout}
                  className="inline-flex items-center rounded-lg border border-border bg-bg px-4 py-2 text-sm font-medium text-ink transition-colors duration-150 hover:bg-surface"
                >
                  Log out
                </button>
              ) : (
                <Link
                  to="/login"
                  className="inline-flex items-center rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition-colors duration-150 hover:bg-primary-strong"
                >
                  Log in
                </Link>
              )}
            </div>

            {/* Mobile menu toggle */}
            <div className="flex items-center sm:hidden">
              <button
                type="button"
                onClick={() => setMenuOpen((open) => !open)}
                aria-expanded={menuOpen}
                aria-controls="mobile-menu"
                className="inline-flex items-center justify-center rounded-lg p-2 text-muted hover:bg-surface hover:text-ink"
              >
                <span className="sr-only">{menuOpen ? 'Close menu' : 'Open menu'}</span>
                {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile menu panel */}
        {menuOpen && (
          <div id="mobile-menu" className="border-t border-border px-4 pb-4 pt-2 sm:hidden">
            <div className="space-y-1">
              {visibleItems.map((item) => (
                <NavLink key={item.to} to={item.to} onClick={closeMenu} className={mobileLinkClass}>
                  {item.label}
                </NavLink>
              ))}
            </div>
            <div className="mt-3 border-t border-border pt-3">
              {isAuthenticated && user && (
                <p className="px-3 pb-2 text-xs capitalize text-muted">
                  Signed in as {user.role.toLowerCase()}
                </p>
              )}
              {isAuthenticated ? (
                <button
                  type="button"
                  onClick={() => {
                    closeMenu();
                    logout();
                  }}
                  className="block w-full rounded-lg px-3 py-2 text-left text-base font-medium text-muted hover:bg-surface hover:text-ink"
                >
                  Log out
                </button>
              ) : (
                <Link
                  to="/login"
                  onClick={closeMenu}
                  className="block rounded-lg bg-primary px-3 py-2 text-center text-base font-medium text-white hover:bg-primary-strong"
                >
                  Log in
                </Link>
              )}
            </div>
          </div>
        )}
      </nav>

      <main className={`flex min-h-0 flex-1 flex-col ${isChat ? 'overflow-hidden' : ''}`}>
        <Outlet />
      </main>
    </div>
  );
};
