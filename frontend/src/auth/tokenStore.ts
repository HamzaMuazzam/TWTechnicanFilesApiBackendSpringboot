/**
 * Access token lives in memory only (lost on reload → re-obtained via refresh token).
 * Refresh token is persisted in localStorage.
 */
const REFRESH_KEY = 'tw.admin.refreshToken';

let accessToken: string | null = null;
const listeners = new Set<() => void>();

export const tokenStore = {
  getAccess(): string | null {
    return accessToken;
  },
  setAccess(token: string | null) {
    accessToken = token;
  },
  getRefresh(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  },
  setRefresh(token: string | null) {
    try {
      if (token) localStorage.setItem(REFRESH_KEY, token);
      else localStorage.removeItem(REFRESH_KEY);
    } catch {
      /* storage unavailable */
    }
  },
  clear() {
    accessToken = null;
    tokenStore.setRefresh(null);
    listeners.forEach((l) => l());
  },
  /** Subscribe to forced sign-outs triggered from the axios layer. */
  onCleared(listener: () => void): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};
