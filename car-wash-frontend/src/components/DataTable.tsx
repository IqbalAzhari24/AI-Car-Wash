import React from 'react';
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react';
import type { SortState, SortDir } from '../types';

export interface Column<T> {
  /** Unique column id; also the server-side sort field when sortable. */
  key: string;
  header: string;
  sortable?: boolean;
  /** Custom cell renderer; falls back to String(row[key]) when omitted. */
  render?: (row: T) => React.ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  loading?: boolean;
  // Server-side pagination state (page is 0-based).
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
              <div className="h-4 animate-pulse rounded bg-[#1A1A24] motion-reduce:animate-none" />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

const pagerButtonClass =
  'rounded-lg border border-[#2A2A3D] bg-[#13131A] px-3 py-1.5 text-sm font-medium text-[#E8E8F0] transition-colors duration-150 hover:bg-[#1F1F2E] disabled:cursor-not-allowed disabled:opacity-50';

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
  const toRow = Math.min(page * size + rows.length, totalElements);

  return (
    <div className="hex-border overflow-hidden rounded-xl bg-[#13131A]">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-[#1E1E2D]">
          <thead className="bg-[#1A1A24]">
            <tr>
              {columns.map((col) => {
                const active = sort.field === col.key;
                const ariaSort = !col.sortable
                  ? undefined
                  : active
                    ? sort.dir === 'asc'
                      ? ('ascending' as const)
                      : ('descending' as const)
                    : ('none' as const);
                return (
                  <th
                    key={col.key}
                    scope="col"
                    aria-sort={ariaSort}
                    className={`px-4 py-3 text-left font-mono text-xs font-semibold uppercase tracking-wider ${
                      active ? 'text-[#00F0FF]' : 'text-[#9090A8]'
                    } ${col.className ?? ''}`}
                  >
                    {col.sortable ? (
                      <button
                        type="button"
                        onClick={() => handleSort(col)}
                        className={`inline-flex items-center gap-1 rounded transition-colors ${
                          active ? 'text-[#00F0FF]' : 'hover:text-[#E8E8F0]'
                        }`}
                      >
                        {col.header}
                        {active ? (
                          sort.dir === 'asc' ? (
                            <ChevronUp className="h-3.5 w-3.5" />
                          ) : (
                            <ChevronDown className="h-3.5 w-3.5" />
                          )
                        ) : (
                          <ChevronsUpDown className="h-3.5 w-3.5 opacity-60" />
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
          <tbody className="divide-y divide-[#1E1E2D] bg-[#13131A]">
            {loading ? (
              <SkeletonRows columnCount={columns.length} rowCount={Math.min(size, 10)} />
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-sm text-[#5A5A72]">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={rowKey(row)} className="transition-colors duration-150 hover:bg-[#1F1F2E]">
                  {columns.map((col) => (
                    <td key={col.key} className={`px-4 py-3 text-sm text-[#E8E8F0] ${col.className ?? ''}`}>
                      {col.render ? col.render(row) : String((row as Record<string, unknown>)[col.key] ?? '')}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination footer */}
      <div className="flex items-center justify-between border-t border-[#1E1E2D] bg-[#13131A] px-4 py-3">
        <p className="font-mono text-sm tabular-nums text-[#9090A8]">
          {totalElements === 0 ? (
            'No results'
          ) : (
            <>
              Showing <span className="font-medium text-[#E8E8F0]">{fromRow}</span>–
              <span className="font-medium text-[#E8E8F0]">{toRow}</span> of{' '}
              <span className="font-medium text-[#E8E8F0]">{totalElements}</span>
            </>
          )}
        </p>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onPageChange(page - 1)}
            disabled={page <= 0 || loading}
            className={pagerButtonClass}
          >
            Previous
          </button>
          <span className="font-mono text-sm tabular-nums text-[#9090A8]">
            Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
          </span>
          <button
            type="button"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1 || loading}
            className={pagerButtonClass}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}
