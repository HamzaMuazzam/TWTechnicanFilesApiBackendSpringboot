import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/api/endpoints';
import { refreshAccessToken } from '@/api/client';
import type { AdminPrincipal } from '@/api/types';
import { tokenStore } from './tokenStore';

export type AuthStatus = 'booting' | 'anonymous' | 'authenticated';

interface AuthContextValue {
  status: AuthStatus;
  admin: AdminPrincipal | null;
  /** true when role === ADMIN (may mutate). VIEWER is read-only. */
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('booting');
  const [admin, setAdmin] = useState<AdminPrincipal | null>(null);
  const queryClient = useQueryClient();

  const logout = useCallback(() => {
    tokenStore.setAccess(null);
    tokenStore.setRefresh(null);
    setAdmin(null);
    setStatus('anonymous');
    queryClient.clear();
  }, [queryClient]);

  // Boot: restore the session from the refresh token, then load /me.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        // With AUTH_ENABLED=false the server answers /me without any token → straight in, no login page.
        if (tokenStore.getRefresh()) await refreshAccessToken();
        const me = await authApi.me();
        if (!cancelled) {
          setAdmin(me);
          setStatus('authenticated');
        }
      } catch {
        if (!cancelled) logout();
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [logout]);

  // Forced sign-out from the axios layer (refresh failed).
  useEffect(() => tokenStore.onCleared(() => logout()), [logout]);

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password);
    tokenStore.setAccess(res.accessToken);
    tokenStore.setRefresh(res.refreshToken);
    let principal: AdminPrincipal = res.admin;
    try {
      principal = await authApi.me();
    } catch {
      /* fall back to the login payload */
    }
    setAdmin(principal);
    setStatus('authenticated');
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ status, admin, isAdmin: admin?.role === 'ADMIN', login, logout }),
    [status, admin, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
