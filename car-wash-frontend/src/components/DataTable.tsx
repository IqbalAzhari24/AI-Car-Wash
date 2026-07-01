import React from 'react';
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react';
import type { SortState, SortDir } from '../types';

export interface Column<T> {
  key: string;
  header: string;
  sortable?: boolean;
  render?: (row: T) => React.ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  loading?: boolean;
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  sort: SortState;
  onPageChange: (page: number) => void;
  onSortChange: (sort: SortState) => void;
  emptyMessage?: string;
}

function nextDir(current: SortDir): SortDir {
  return current === 'asc' ? 'desc' : 'asc';
}

function SkeletonRows({ columnCount, rowCount }: { columnCount: number; rowCount: number }) {
  return (
    <>
      {Array.from({ length: rowCount }, (_, i) => (
        <tr key={i}>
          {Array.from({ length: columnCount }, (_, j) => (
            <td key={j} className="px-4 py-3.5">
              <div className="h-4 animate-pulse rounded bg-surface-raised motion-reduce:animate-none" />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

const pagerBtnClass =
  'min-h-[44px] rounded-lg border border-border bg-surface px-3 py-1.5 text-sm font-medium text-secondary transition-colors duration-150 hover:border-slate hover:text-primary disabled:cursor-not-allowed disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate focus-visible:ring-offset-2 focus-visible:ring-offset-base';

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  loading = false,
  page,
  size,
  totalPages,
  totalElements,
  sort,
  onPageChange,
  onSortChange,
  emptyMessage = 'No records found.',
}: DataTableProps<T>) {
  const handleSort = (col: Column<T>) => {
    if (!col.sortable) return;
    const dir: SortDir = sort.field === col.key ? nextDir(sort.dir) : 'asc';
    onSortChange({ field: col.key, dir });
  };

  const fromRow = totalElements === 0 ? 0 : page * size + 1;
  const toRow   = Math.min(page * size + rows.length, totalElements);

  return (
    <div className="overflow-hidden rounded-xl hex-border bg-surface">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-border" aria-label="Data table">
          <thead className="bg-surface-raised">
            <tr>
              {columns.map((col) => {
                const active    = sort.field === col.key;
                const ariaSort  = !col.sortable
                  ? undefined
                  : active
                  ? sort.dir === 'asc' ? ('ascending' as const) : ('descending' as const)
                  : ('none' as const);
                return (
                  <th
                    key={col.key}
                    scope="col"
                    aria-sort={ariaSort}
                    className={`px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-muted ${col.className ?? ''}`}
                  >
                    {col.sortable ? (
                      <button
                        type="button"
                        onClick={() => handleSort(col)}
                        className="inline-flex items-center gap-1 rounded hover:text-secondary transition-colors"
                      >
                        {col.header}
                        {active ? (
                          sort.dir === 'asc'
                            ? <ChevronUp className="h-3.5 w-3.5" />
                            : <ChevronDown className="h-3.5 w-3.5" />
                        ) : (
                          <ChevronsUpDown className="h-3.5 w-3.5 opacity-40" />
                        )}
                      </button>
                    ) : (
                      col.header
                    )}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody className="divide-y divide-border-subtle bg-surface">
            {loading ? (
              <SkeletonRows columnCount={columns.length} rowCount={Math.min(size, 10)} />
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-sm text-muted">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr
                  key={rowKey(row)}
                  className="transition-colors duration-150 hover:bg-surface-hover"
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className={`px-4 py-3 text-sm text-primary ${col.className ?? ''}`}
                    >
                      {col.render
                        ? col.render(row)
                        : String((row as Record<string, unknown>)[col.key] ?? '')}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination footer */}
      <div className="flex items-center justify-between border-t border-border bg-surface-raised px-4 py-3">
        <p className="text-sm tabular-nums text-muted">
          {totalElements === 0 ? (
            'No results'
          ) : (
            <>
              Showing{' '}
              <span className="font-medium text-primary">{fromRow}</span>–
              <span className="font-medium text-primary">{toRow}</span>{' '}
              of <span className="font-medium text-primary">{totalElements}</span>
            </>
          )}
        </p>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onPageChange(page - 1)}
            disabled={page <= 0 || loading}
            className={pagerBtnClass}
          >
            Previous
          </button>
          <span className="text-sm tabular-nums text-muted">
            Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
          </span>
          <button
            type="button"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1 || loading}
            className={pagerBtnClass}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}
