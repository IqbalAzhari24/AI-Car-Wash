
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { MessageCircle, ShieldAlert } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { btnPrimary, btnSecondary, btnGhost, cardPanel } from './components/ui';
import { Login } from './components/Login';
import { Landing } from './pages/Landing';
import { TimahChat } from './pages/customer/TimahChat';
import { CheckoutPage } from './pages/customer/Checkout';
import { BookingFlow } from './pages/customer/book/BookingFlow';
import { MyBookings } from './pages/customer/MyBookings';
import { WorkerJobs } from './pages/worker/WorkerJobs';
import { ClerkConsole } from './pages/clerk/ClerkConsole';
import { ValetRequest } from './pages/customer/ValetRequest';
import { ValetRequests } from './pages/clerk/ValetRequests';
import { UserDirectory } from './pages/admin/UserDirectory';
import { TransactionHistory } from './pages/admin/TransactionHistory';
import { OwnerAnalytics } from './pages/admin/OwnerAnalytics';

const Dashboard = () => (
  <div className="flex flex-1 items-center justify-center px-4 py-16">
    <div className={`${cardPanel} hex-corner w-full max-w-md p-8 text-center`}>
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-[#00F0FF]/30 bg-[#00F0FF]/10">
        <MessageCircle className="h-7 w-7 text-[#00F0FF]" />
      </div>
      <h1 className="mt-5 font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">Ready for a wash?</h1>
      <p className="mt-2 text-sm text-[#9090A8]">
        Book your next wash in three quick steps, or ask Timah anything.
      </p>
      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
        <Link to="/book" className={btnPrimary}>
          Book a wash
        </Link>
        <Link to="/chat" className={btnSecondary}>
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
    <div className="hex-grid hex-border-danger hex-corner w-full max-w-md rounded-2xl p-8 text-center">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-[#FF4466]/30 bg-[#FF4466]/10">
        <ShieldAlert className="h-7 w-7 text-[#FF4466]" />
      </div>
      <h1 className="mt-5 font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">No access to this page</h1>
      <p className="mt-2 text-sm text-[#9090A8]">
        Your account doesn't have permission to view this area. If that seems wrong, ask the
        shop owner to check your role.
      </p>
      <Link to="/" className={`${btnGhost} mt-6`}>
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
            <Route path="/bookings" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <MyBookings />
              </ProtectedRoute>
            } />
            <Route path="/chat" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <TimahChat />
              </ProtectedRoute>
            } />
            <Route path="/valet" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <ValetRequest />
              </ProtectedRoute>
            } />
            <Route path="/checkout/:bookingId" element={
              <ProtectedRoute allowedRoles={['CUSTOMER']}>
                <CheckoutPage />
              </ProtectedRoute>
            } />

            {/* Clerk routes */}
            <Route path="/clerk" element={
              <ProtectedRoute allowedRoles={['CLERK', 'OWNER']}>
                <ClerkConsole />
              </ProtectedRoute>
            } />
            <Route path="/clerk/valet" element={
              <ProtectedRoute allowedRoles={['CLERK', 'OWNER']}>
                <ValetRequests />
              </ProtectedRoute>
            } />

            {/* Worker routes */}
            <Route path="/worker" element={
              <ProtectedRoute allowedRoles={['WORKER', 'OWNER']}>
                <WorkerJobs />
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
