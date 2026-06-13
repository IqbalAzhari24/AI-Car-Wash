
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { MessageCircle, ShieldAlert } from 'lucide-react';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { Login } from './components/Login';
import { TimahChat } from './pages/customer/TimahChat';
import { CheckoutPage } from './pages/customer/Checkout';
import { UserDirectory } from './pages/admin/UserDirectory';
import { TransactionHistory } from './pages/admin/TransactionHistory';

const Dashboard = () => (
  <div className="flex flex-1 items-center justify-center px-4 py-16">
    <div className="max-w-md text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary-soft">
        <MessageCircle className="h-6 w-6 text-primary-soft-ink" />
      </div>
      <h1 className="mt-4 text-xl font-semibold tracking-tight text-ink">Ready for a wash?</h1>
      <p className="mt-2 text-sm text-muted">
        Your dashboard is on its way. In the meantime, Timah can get your next wash booked in
        under a minute.
      </p>
      <Link
        to="/chat"
        className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-white transition-colors duration-150 hover:bg-primary-strong"
      >
        <MessageCircle className="h-4 w-4" />
        Chat with Timah
      </Link>
    </div>
  </div>
);

const Unauthorized = () => (
  <div className="flex flex-1 items-center justify-center px-4 py-16">
    <div className="max-w-md text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-danger-soft">
        <ShieldAlert className="h-6 w-6 text-danger-soft-ink" />
      </div>
      <h1 className="mt-4 text-xl font-semibold tracking-tight text-ink">No access to this page</h1>
      <p className="mt-2 text-sm text-muted">
        Your account doesn't have permission to view this area. If that seems wrong, ask the
        shop owner to check your role.
      </p>
      <Link
        to="/"
        className="mt-6 inline-flex items-center rounded-lg border border-border bg-bg px-4 py-2.5 text-sm font-medium text-ink transition-colors duration-150 hover:bg-surface"
      >
        Back to dashboard
      </Link>
    </div>
  </div>
);


function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/login" element={<Login />} />
            <Route path="/unauthorized" element={<Unauthorized />} />

            {/* Customer routes */}
            <Route path="/" element={
              <ProtectedRoute allowedRoles={['CUSTOMER', 'CLERK', 'WORKER', 'OWNER']}>
                <Dashboard />
              </ProtectedRoute>
            } />
            <Route path="/chat" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <TimahChat />
              </ProtectedRoute>
            } />
            <Route path="/checkout/:bookingId" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <CheckoutPage />
              </ProtectedRoute>
            } />

            {/* Admin routes (Owner only) */}
            <Route path="/admin" element={<Navigate to="/admin/users" replace />} />
            <Route path="/admin/users" element={
              <ProtectedRoute allowedRoles={['OWNER']}>
                <UserDirectory />
              </ProtectedRoute>
            } />
            <Route path="/admin/users/:id/transactions" element={
              <ProtectedRoute allowedRoles={['OWNER']}>
                <TransactionHistory />
              </ProtectedRoute>
            } />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
