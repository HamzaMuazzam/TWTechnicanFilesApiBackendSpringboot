import { Navigate, Outlet, useLocation } from 'react-router';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from './AuthContext';
import { EmptyState } from '@/components/EmptyState';

function FullPageSpinner() {
  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', bgcolor: 'background.default' }}>
      <CircularProgress size={28} />
    </Box>
  );
}

/** Redirects anonymous visitors to /login, remembering where they came from. */
export function RequireAuth() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === 'booting') return <FullPageSpinner />;
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location }} />;
  return <Outlet />;
}

/** Sends already-signed-in users away from the login page, back to where they came from. */
export function AnonymousOnly() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === 'booting') return <FullPageSpinner />;
  if (status === 'authenticated') {
    const from = (location.state as { from?: { pathname?: string; search?: string } } | null)?.from;
    const to = from?.pathname && from.pathname !== '/login' ? `${from.pathname}${from.search ?? ''}` : '/';
    return <Navigate to={to} replace />;
  }
  return <Outlet />;
}

/** ADMIN role gate for whole pages (SQL console, Admins). */
export function RequireAdmin() {
  const { isAdmin } = useAuth();
  if (!isAdmin) {
    return (
      <EmptyState
        title="ADMIN role required"
        description="Your account has the VIEWER role. This page is only available to administrators."
      />
    );
  }
  return <Outlet />;
}
