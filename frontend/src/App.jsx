import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import AppErrorBoundary from './components/AppErrorBoundary';
import { trackPageView } from './services/analytics';
import { normalizeRole } from './utils/roles';

const namedPage = (name) => lazy(() => import('./pages/PublicInfoPages').then((module) => ({ default: module[name] })));
const Login = lazy(() => import('./pages/Login.jsx'));
const Dashboard = lazy(() => import('./pages/Dashboard.jsx'));
const Clients = lazy(() => import('./pages/Clients.jsx'));
const Projects = lazy(() => import('./pages/Projects.jsx'));
const Tasks = lazy(() => import('./pages/Tasks.jsx'));
const Invoices = lazy(() => import('./pages/Invoices.jsx'));
const AiGenerator = lazy(() => import('./pages/AiGenerator.jsx'));
const PublicPortal = lazy(() => import('./pages/PublicPortal.jsx'));
const ProposalView = lazy(() => import('./pages/ProposalView.jsx'));
const AgencySettings = lazy(() => import('./pages/AgencySettings.jsx'));
const SetupOwner = lazy(() => import('./pages/SetupOwner.jsx'));
const VerifyEmail = lazy(() => import('./pages/VerifyEmail.jsx'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword.jsx'));
const ResetPassword = lazy(() => import('./pages/ResetPassword.jsx'));
const AcceptInvite = lazy(() => import('./pages/AcceptInvite.jsx'));
const TeamSettings = lazy(() => import('./pages/TeamSettings.jsx'));
const Proposals = lazy(() => import('./pages/Proposals.jsx'));
const ContactPage = namedPage('ContactPage');
const PrivacyPage = namedPage('PrivacyPage');
const TermsPage = namedPage('TermsPage');
const NotFound = lazy(() => import('./pages/NotFound.jsx'));

function RouteLoading() {
  return <div className="flex min-h-screen items-center justify-center bg-paper text-ink" role="status" aria-label="Loading page"><div className="h-8 w-8 animate-spin rounded-full border-t-2 border-b-2 border-brass-dark" /></div>;
}

function AnalyticsRouteTracker() {
  const { pathname } = useLocation();
  React.useEffect(() => trackPageView(pathname), [pathname]);
  return null;
}

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper text-ink font-sans">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-brass-dark"></div>
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  const userRole = normalizeRole(user.role);
  if (!userRole) {
    return <Navigate to="/login" replace />;
  }
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    return <Navigate to="/" replace />;
  }
  return (
    <div className="flex min-h-screen bg-paper text-ink font-sans">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        {children}
      </div>
    </div>
  );
};

const HomeRoute = () => {
  const { user } = useAuth();
  const role = normalizeRole(user?.role);
  if (role === 'MEMBER') return <Navigate to="/tasks" replace />;
  if (role === 'ADMIN') return <Navigate to="/clients" replace />;
  return <Dashboard />;
};

export default function App() {
  return (
    <AppErrorBoundary>
    <AuthProvider>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AnalyticsRouteTracker />
        <Suspense fallback={<RouteLoading />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Navigate to="/setup" replace />} />
          <Route path="/setup" element={<SetupOwner />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/accept-invite" element={<AcceptInvite />} />
          <Route path="/privacy" element={<PrivacyPage />} />
          <Route path="/terms" element={<TermsPage />} />
          <Route path="/contact" element={<ContactPage />} />
          <Route path="/portal/share/:token" element={<PublicPortal />} />
          <Route path="/p/:token" element={<ProposalView />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <HomeRoute />
              </ProtectedRoute>
            }
          />
          <Route
            path="/clients"
            element={
              <ProtectedRoute allowedRoles={['OWNER', 'ADMIN']}>
                <Clients />
              </ProtectedRoute>
            }
          />
          <Route
            path="/proposals"
            element={
              <ProtectedRoute allowedRoles={['OWNER', 'ADMIN']}>
                <Proposals />
              </ProtectedRoute>
            }
          />
          <Route
            path="/projects"
            element={
              <ProtectedRoute allowedRoles={['OWNER', 'MEMBER', 'CLIENT']}>
                <Projects />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tasks"
            element={
              <ProtectedRoute allowedRoles={['OWNER', 'MEMBER', 'CLIENT']}>
                <Tasks />
              </ProtectedRoute>
            }
          />
          <Route
            path="/invoices"
            element={
              <ProtectedRoute allowedRoles={['OWNER', 'CLIENT']}>
                <Invoices />
              </ProtectedRoute>
            }
          />
          <Route
            path="/ai-generator"
            element={
              <ProtectedRoute allowedRoles={['OWNER', 'ADMIN']}>
                <AiGenerator />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings/agency"
            element={
              <ProtectedRoute allowedRoles={['OWNER']}>
                <AgencySettings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings/team"
            element={
              <ProtectedRoute allowedRoles={['OWNER']}>
                <TeamSettings />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
    </AppErrorBoundary>
  );
}
