// Shared API/domain types for the Owner dashboard.

export type Role = 'OWNER' | 'CLERK' | 'WORKER' | 'CUSTOMER';

export interface UserRow {
  id: string;
  email: string;
  phoneNumber: string;
  role: Role;
  createdAt: string;
}

export interface Transaction {
  bookingId: string;
  slotTime: string;
  vehicleClass: string | null;
  bookingStatus: string | null;
  createdAt: string;
  amount: number | null;
  paymentStatus: string | null;
  transactionId: string | null;
}

// Mirrors Spring Data's Page<T> JSON shape (the fields we use).
export interface Page<T> {
  content: T[];
  number: number; // current page index (0-based)
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type SortDir = 'asc' | 'desc';

export interface SortState {
  field: string;
  dir: SortDir;
}
