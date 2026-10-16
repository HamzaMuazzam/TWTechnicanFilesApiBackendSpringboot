import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { AppShell } from '@/components/layout/AppShell';
import { AnonymousOnly, RequireAdmin, RequireAuth } from '@/auth/guards';
import { LoginPage } from '@/pages/LoginPage';
import { OverviewPage } from '@/pages/OverviewPage';
import { AlertsPage } from '@/pages/alerts/AlertsPage';
import { UsersPage } from '@/pages/users/UsersPage';
import { DeliveriesPage, DevicesPage, DiscardedPage, NotificationsPage } from '@/pages/SimpleGridPages';
import { ConfigurationsPage } from '@/pages/ConfigurationsPage';
import { SourcesPage } from '@/pages/SourcesPage';
import { AlertTypesPage } from '@/pages/AlertTypesPage';
import { AdminsPage } from '@/pages/AdminsPage';
import { SqlConsolePage } from '@/pages/SqlConsolePage';
import { SettingsPage } from '@/pages/SettingsPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

/** Served under /admin (Vite base + router basename). */
export function App() {
  return (
    <BrowserRouter basename="/admin">
      <Routes>
        <Route element={<AnonymousOnly />}>
          <Route path="/login" element={<LoginPage />} />
        </Route>
        <Route element={<RequireAuth />}>
          <Route element={<AppShell />}>
            <Route index element={<OverviewPage />} />
            <Route path="overview" element={<Navigate to="/" replace />} />
            <Route path="alerts" element={<AlertsPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="deliveries" element={<DeliveriesPage />} />
            <Route path="discarded" element={<DiscardedPage />} />
            <Route path="configurations" element={<ConfigurationsPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="devices" element={<DevicesPage />} />
            <Route path="sources" element={<SourcesPage />} />
            <Route path="alert-types" element={<AlertTypesPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route element={<RequireAdmin />}>
              <Route path="admins" element={<AdminsPage />} />
              <Route path="sql" element={<SqlConsolePage />} />
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
