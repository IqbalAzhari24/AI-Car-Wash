import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { MessageCircle, ShieldAlert } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { Login } from './components/Login';
import { Account } from './pages/Account';
import { Landing } from './pages/Landing';
import { TimahChat } from './pages/customer/TimahChat';
import { CheckoutPage } from './pages/customer/Checkout';
import { BookingFlow } from './pages/customer/book/BookingFlow';
import { UserDirectory } from './pages/admin/UserDirectory';
import { TransactionHistory } from './pages/admin/TransactionHistory';
import { OwnerAnalytics } from './pages/admin/OwnerAnalytics';
import { ClerkConsole } from './pages/clerk/ClerkConsole';
import { ValetRequests } from './pages/clerk/ValetRequests';
import { ValetRequest } from './pages/customer/ValetRequest';
import { MyBookings } from './pages/customer/MyBookings';
import { WorkerJobs } from './pages/worker/WorkerJobs';


const Dashboard = () => (
  <div className="flex flex-1 items-center justify-center px-4 py-16">
    <div className="w-full max-w-sm text-center">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl hex-grid hex-border">
        <MessageCircle className="h-6 w-6 text-cyan" aria-hidden="true" />
      </div>
      <h1 className="mt-5 font-display text-xl font-semibold text-primary">
        Ready for a wash?
      </h1>
      <p className="mt-2 text-sm text-secondary">
        Book your next wash in three quick steps, or ask Timah anything.
      </p>
      <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
        <Link
          to="/book"
          className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-lg border border-cyan px-5 py-2.5 text-sm font-medium text-cyan shadow-cyan-glow transition-all duration-150 hover:bg-cyan hover:text-base active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base"
        >
          Book a wash
        </Link>
        <Link
          to="/chat"
          className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-lg border border-border bg-surface px-5 py-2.5 text-sm font-medium text-secondary transition-colors duration-150 hover:border-slate hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate focus-visible:ring-offset-2 focus-visible:ring-offset-base"
        >
          <MessageCircle className="h-4 w-4" aria-hidden="true" />
          Chat with Timah
        </Link>
      </div>
    </div>
  </div>
);

const Home = () => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Dashboard /> : <Landing />;
};

const Unauthorized = () => (
  <div className="flex flex-1 items-center justify-center px-4 py-16">
    <div className="w-full max-w-sm text-center">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl hex-border-danger" style={{ backgroundColor: 'rgba(255,68,102,0.08)' }}>
        <ShieldAlert className="h-6 w-6 text-danger" aria-hidden="true" />
      </div>
      <h1 className="mt-5 font-display text-xl font-semibold text-primary">
        No access to this page
      </h1>
      <p className="mt-2 text-sm text-secondary">
        Your account doesn't have permission to view this area. If that seems wrong, ask the
        shop owner to check your role.
      </p>
      <Link
        to="/"
        className="mt-7 inline-flex min-h-[44px] items-center rounded-lg border border-border bg-surface px-5 py-2.5 text-sm font-medium text-secondary transition-colors duration-150 hover:border-slate hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate focus-visible:ring-offset-2 focus-visible:ring-offset-base"
      >
        Back to home
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
            <Route path="/login"        element={<Login />} />
            <Route path="/unauthorized" element={<Unauthorized />} />
            <Route path="/"             element={<Home />} />

            <Route path="/account" element={
              <ProtectedRoute>
                <Account />
              </ProtectedRoute>
            } />

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
            <Route path="/clerk" element={
              <ProtectedRoute allowedRoles={['CLERK']}>
                <ClerkConsole />
              </ProtectedRoute>
            } />
            <Route path="/clerk/valet" element={
              <ProtectedRoute allowedRoles={['CLERK']}>
                <ValetRequests />
              </ProtectedRoute>
            } />

            <Route path="/valet" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <ValetRequest />
              </ProtectedRoute>
            } />
            <Route path="/bookings" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <MyBookings />
              </ProtectedRoute>
            } />

            <Route path="/worker" element={
              <ProtectedRoute allowedRoles={['WORKER']}>
                <WorkerJobs />
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
