import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { Alert, Snackbar, type AlertColor } from '@mui/material';
import { friendlyError } from '@/api/errors';

interface Toast {
  key: number;
  message: string;
  severity: AlertColor;
}

interface SnackbarApi {
  notify: (message: string, severity?: AlertColor) => void;
  success: (message: string) => void;
  error: (err: unknown, prefix?: string) => void;
}

const SnackbarContext = createContext<SnackbarApi | null>(null);

export function SnackbarProvider({ children }: { children: ReactNode }) {
  // The head of the queue is the visible toast; `closingKey` marks it as exiting.
  const [queue, setQueue] = useState<Toast[]>([]);
  const [closingKey, setClosingKey] = useState<number | null>(null);
  const current = queue[0] ?? null;
  const open = !!current && closingKey !== current.key;

  const notify = useCallback((message: string, severity: AlertColor = 'info') => {
    setQueue((q) => [...q, { key: Date.now() + Math.random(), message, severity }]);
  }, []);
  const close = () => current && setClosingKey(current.key);
  const exited = () => {
    setQueue((q) => q.slice(1));
    setClosingKey(null);
  };

  const api = useMemo<SnackbarApi>(
    () => ({
      notify,
      success: (m) => notify(m, 'success'),
      error: (e, prefix) => notify(prefix ? `${prefix}: ${friendlyError(e)}` : friendlyError(e), 'error'),
    }),
    [notify],
  );

  return (
    <SnackbarContext.Provider value={api}>
      {children}
      <Snackbar
        key={current?.key}
        open={open}
        autoHideDuration={current?.severity === 'error' ? 7000 : 3500}
        onClose={(_, reason) => {
          if (reason === 'clickaway') return;
          close();
        }}
        slotProps={{ transition: { onExited: exited } }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        <Alert
          onClose={close}
          severity={current?.severity ?? 'info'}
          variant="filled"
          sx={{ minWidth: 280, boxShadow: 'none' }}
        >
          {current?.message}
        </Alert>
      </Snackbar>
    </SnackbarContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSnackbar(): SnackbarApi {
  const ctx = useContext(SnackbarContext);
  if (!ctx) throw new Error('useSnackbar must be used inside <SnackbarProvider>');
  return ctx;
}
