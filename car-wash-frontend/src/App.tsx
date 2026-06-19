
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { MessageCircle, ShieldAlert } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { Login } from './components/Login';
import { Landing } from './pages/Landing';
import { TimahChat } from './pages/customer/TimahChat';
import { CheckoutPage } from './pages/customer/Checkout';
import { BookingFlow } from './pages/customer/book/BookingFlow';
import { UserDirectory } from './pages/admin/UserDirectory';
import { TransactionHistory } from './pages/admin/TransactionHistory';
import { OwnerAnalytics } from './pages/admin/OwnerAnalytics';

const Dashboard = () => (
  <div className="flex flex-1 items-center justify-center px-4 py-16">
    <div className="max-w-md text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary-soft">
        <MessageCircle className="h-6 w-6 text-primary-soft-ink" />
      </div>
      <h1 className="mt-4 text-xl font-semibold tracking-tight text-ink">Ready for a wash?</h1>
      <p className="mt-2 text-sm text-muted">
        Book your next wash in three quick steps, or ask Timah anything.
      </p>
      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
        <Link
          to="/book"
          className="inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-white transition-colors duration-150 hover:bg-primary-strong"
        >
          Book a wash
        </Link>
        <Link
          to="/chat"
          className="inline-flex items-center justify-center gap-2 rounded-lg border border-border bg-bg px-4 py-2.5 text-sm font-medium text-ink transition-colors duration-150 hover:bg-surface"
        >
          <MessageCircle className="h-4 w-4" />
          Chat with Timah
        </Link>
      </div>
    </div>
  </div>
);

// Unauthenticated visitors land on the marketing page; everyone else sees their dashboard.
const Home = () => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Dashboard /> : <Landing />;
};

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

            {/* Public landing for visitors; dashboard for signed-in users */}
            <Route path="/" element={<Home />} />

            {/* Customer routes */}
            <Route path="/book" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <BookingFlow />
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
            <Route path="/admin" element={<Navigate to="/admin/analytics" replace />} />
            <Route path="/admin/analytics" element={
              <ProtectedRoute allowedRoles={['OWNER']}>
                <OwnerAnalytics />
              </ProtectedRoute>
            } />
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
