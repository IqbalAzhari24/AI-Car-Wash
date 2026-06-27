import React, { useState } from 'react';
import { Outlet, Link, NavLink, useLocation } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

// Cyan hex-and-droplet brand mark — matches the Login wordmark.
const BrandMark: React.FC = () => (
  <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true" className="h-8 w-8">
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
);

interface NavItem {
  to: string;
  label: string;
  roles?: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard' },
  { to: '/book', label: 'Book a Wash', roles: ['CUSTOMER'] },
  { to: '/bookings', label: 'My Bookings', roles: ['CUSTOMER'] },
  { to: '/valet', label: 'Valet', roles: ['CUSTOMER'] },
  { to: '/chat', label: 'Chat with Timah', roles: ['CUSTOMER'] },
  { to: '/clerk', label: 'Clerk Console', roles: ['CLERK'] },
  { to: '/clerk/valet', label: 'Valet Requests', roles: ['CLERK'] },
  { to: '/worker', label: 'Job Board', roles: ['WORKER'] },
  { to: '/admin/analytics', label: 'Analytics', roles: ['OWNER'] },
  { to: '/admin/users', label: 'Users', roles: ['OWNER'] },
];

const desktopLinkClass = ({ isActive }: { isActive: boolean }): string =>
  `inline-flex items-center border-b-2 px-1 pt-1 text-sm font-medium transition-colors duration-150 ${
    isActive
      ? 'border-[#00F0FF] text-[#00F0FF] [text-shadow:0_0_12px_rgba(0,240,255,0.45)]'
      : 'border-transparent text-[#9090A8] hover:border-[#2A2A3D] hover:text-[#E8E8F0]'
  }`;

const mobileLinkClass = ({ isActive }: { isActive: boolean }): string =>
  `block min-h-[44px] rounded-lg px-3 py-2.5 text-base font-medium ${
    isActive
      ? 'hex-border-active text-[#00F0FF]'
      : 'text-[#9090A8] hover:bg-[#1F1F2E] hover:text-[#E8E8F0]'
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
    <div className={`flex flex-col bg-[#0D0D11] text-[#E8E8F0] ${isChat ? 'h-dvh' : 'min-h-dvh'}`}>
      <nav className="hex-grid-dense border-b border-[#1E1E2D]">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 justify-between">
            <div className="flex">
              <Link
                to="/"
                onClick={closeMenu}
                className="flex flex-shrink-0 items-center gap-2.5 rounded-lg"
              >
                <BrandMark />
                <span className="font-display text-lg font-semibold tracking-tight text-[#E8E8F0]">
                  AI Car <span className="text-[#00F0FF]">Wash</span>
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
                <span className="rounded-full border border-[#2A2A3D] bg-[#13131A] px-2.5 py-0.5 font-mono text-xs uppercase tracking-wider text-[#9090A8]">
                  {user.role.toLowerCase()}
                </span>
              )}
              {isAuthenticated ? (
                <button
                  type="button"
                  onClick={logout}
                  className="inline-flex min-h-[40px] items-center rounded-lg border border-[#2A2A3D] bg-[#13131A] px-4 py-2 text-sm font-medium text-[#E8E8F0] transition-colors duration-150 hover:bg-[#1F1F2E]"
                >
                  Log out
                </button>
              ) : (
                <Link
                  to="/login"
                  className="inline-flex min-h-[40px] items-center rounded-lg bg-[#00F0FF] px-4 py-2 text-sm font-semibold text-[#0D0D11] shadow-cyan-glow transition-colors duration-150 hover:bg-[#00B8C4]"
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
                className="inline-flex h-11 w-11 items-center justify-center rounded-lg text-[#9090A8] hover:bg-[#1F1F2E] hover:text-[#E8E8F0]"
              >
                <span className="sr-only">{menuOpen ? 'Close menu' : 'Open menu'}</span>
                {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile menu panel */}
        {menuOpen && (
          <div id="mobile-menu" className="hex-grid-subtle border-t border-[#1E1E2D] px-4 pb-4 pt-2 sm:hidden">
            <div className="space-y-1">
              {visibleItems.map((item) => (
                <NavLink key={item.to} to={item.to} onClick={closeMenu} className={mobileLinkClass}>
                  {item.label}
                </NavLink>
              ))}
            </div>
            <div className="mt-3 border-t border-[#1E1E2D] pt-3">
              {isAuthenticated && user && (
                <p className="px-3 pb-2 font-mono text-xs uppercase tracking-wider text-[#5A5A72]">
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
                  className="block min-h-[44px] w-full rounded-lg px-3 py-2.5 text-left text-base font-medium text-[#9090A8] hover:bg-[#1F1F2E] hover:text-[#E8E8F0]"
                >
                  Log out
                </button>
              ) : (
                <Link
                  to="/login"
                  onClick={closeMenu}
                  className="block min-h-[44px] rounded-lg bg-[#00F0FF] px-3 py-2.5 text-center text-base font-semibold text-[#0D0D11] shadow-cyan-glow hover:bg-[#00B8C4]"
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
