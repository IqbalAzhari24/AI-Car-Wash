import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Receipt } from 'lucide-react';
import api from '../../api/api';
import { DataTable, type Column } from '../../components/DataTable';
import type { Page, Role, SortState, UserRow } from '../../types';

type Tab = 'customers' | 'staff';
type StaffFilter = 'ALL' | 'OWNER' | 'CLERK' | 'WORKER';

const PAGE_SIZE = 10;
const STAFF_ROLES: Role[] = ['OWNER', 'CLERK', 'WORKER'];

const roleBadgeClass: Record<Role, string> = {
  OWNER: 'bg-primary-soft text-primary-soft-ink',
  CLERK: 'bg-accent-soft text-accent-soft-ink',
  WORKER: 'bg-warning-soft text-warning-soft-ink',
  CUSTOMER: 'bg-surface-2 text-muted',
};

const formatDate = (iso: string): string => {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString();
};

export const UserDirectory: React.FC = () => {
  const navigate = useNavigate();

  const [tab, setTab] = useState<Tab>('customers');
  const [staffFilter, setStaffFilter] = useState<StaffFilter>('ALL');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<SortState>({ field: 'createdAt', dir: 'desc' });

  const [data, setData] = useState<Page<UserRow> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // The roles to request for the active tab.
  const activeRoles = useMemo<Role[]>(() => {
    if (tab === 'customers') return ['CUSTOMER'];
    return staffFilter === 'ALL' ? STAFF_ROLES : [staffFilter];
  }, [tab, staffFilter]);

  // Debounce the search box; reset to first page on a new term.
  useEffect(() => {
    const t = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 350);
    return () => clearTimeout(t);
  }, [searchInput]);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      // Build params manually so repeated roles serialize as role=A&role=B (not role[]).
      const params = new URLSearchParams();
      activeRoles.forEach((r) => params.append('role', r));
      if (search) params.append('search', search);
      params.append('page', String(page));
      params.append('size', String(PAGE_SIZE));
      params.append('sort', `${sort.field},${sort.dir}`);

      const res = await api.get<Page<UserRow>>('/v1/owner/users', { params });
      setData(res.data);
    } catch (err: any) {
      setError(
        err?.response?.status === 403
          ? 'You do not have permission to view the user directory.'
          : 'Failed to load users. Please try again.'
      );
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [activeRoles, search, page, sort]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const switchTab = (next: Tab) => {
    if (next === tab) return;
    setTab(next);
    setPage(0);
    setSort({ field: 'createdAt', dir: 'desc' });
  };

  const columns = useMemo<Column<UserRow>[]>(() => {
    const base: Column<UserRow>[] = [
      { key: 'email', header: 'Email', sortable: true },
      { key: 'phoneNumber', header: 'Phone', sortable: true },
      {
        key: 'role',
        header: 'Role',
        render: (row) => (
          <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${roleBadgeClass[row.role]}`}>
            {row.role}
          </span>
        ),
      },
      { key: 'createdAt', header: 'Joined', sortable: true, render: (row) => formatDate(row.createdAt) },
    ];

    if (tab === 'customers') {
      base.push({
        key: 'actions',
        header: 'Actions',
        className: 'text-right',
        render: (row) => (
          <button
            type="button"
            onClick={() => navigate(`/admin/users/${row.id}/transactions`)}
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary-soft px-3 py-1.5 text-xs font-medium text-primary-soft-ink transition-colors duration-150 hover:bg-primary-soft/70"
          >
            <Receipt className="h-3.5 w-3.5" />
            View Transactions
          </button>
        ),
      });
    }

    return base;
  }, [tab, navigate]);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight text-ink">User directory</h1>
        <p className="mt-1 text-sm text-muted">Browse and search all customer and staff accounts.</p>
      </div>

      {/* Tabs */}
      <div className="mb-4 border-b border-border">
        <nav className="-mb-px flex gap-6">
          {(['customers', 'staff'] as Tab[]).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => switchTab(t)}
              className={`whitespace-nowrap border-b-2 px-1 py-3 text-sm font-medium capitalize transition-colors duration-150 ${
                tab === t
                  ? 'border-primary text-ink'
                  : 'border-transparent text-muted hover:border-border hover:text-ink'
              }`}
            >
              {t}
            </button>
          ))}
        </nav>
      </div>

      {/* Toolbar: search + (staff) role sub-filter */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative max-w-sm flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
          <label htmlFor="user-search" className="sr-only">
            Search by email or phone
          </label>
          <input
            id="user-search"
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by email or phone…"
            className="w-full rounded-lg border border-border bg-bg py-2 pl-9 pr-3 text-sm text-ink placeholder:text-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          />
        </div>

        {tab === 'staff' && (
          <select
            value={staffFilter}
            onChange={(e) => {
              setStaffFilter(e.target.value as StaffFilter);
              setPage(0);
            }}
            aria-label="Filter staff by role"
            className="rounded-lg border border-border bg-bg px-3 py-2 text-sm text-ink focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="ALL">All staff</option>
            <option value="OWNER">Owner</option>
            <option value="CLERK">Clerk</option>
            <option value="WORKER">Worker</option>
          </select>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded-lg border border-danger/20 bg-danger-soft p-3 text-sm text-danger-soft-ink" role="alert">
          {error}
        </div>
      )}

      <DataTable<UserRow>
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(row) => row.id}
        loading={loading}
        page={data?.number ?? page}
        size={data?.size ?? PAGE_SIZE}
        totalPages={data?.totalPages ?? 0}
        totalElements={data?.totalElements ?? 0}
        sort={sort}
        onPageChange={setPage}
        onSortChange={(s) => {
          setSort(s);
          setPage(0);
        }}
        emptyMessage={
          tab === 'customers'
            ? 'No customers match. Try a different email or phone, or clear the search.'
            : 'No staff match. Try a different search or role filter.'
        }
      />
    </div>
  );
};
