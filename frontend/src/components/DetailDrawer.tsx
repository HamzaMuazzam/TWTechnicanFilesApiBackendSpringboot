import { Box, Drawer, IconButton, Skeleton, Typography, useMediaQuery, useTheme } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import type { ReactNode } from 'react';
import { ErrorState } from './ErrorState';

interface DetailDrawerProps {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  subtitle?: ReactNode;
  /** header actions (buttons) */
  actions?: ReactNode;
  width?: number;
  loading?: boolean;
  error?: unknown;
  onRetry?: () => void;
  children: ReactNode;
}

/** Right-side detail drawer (560px, full-width on phones). */
export function DetailDrawer({ open, onClose, title, subtitle, actions, width = 560, loading, error, onRetry, children }: DetailDrawerProps) {
  const theme = useTheme();
  const small = useMediaQuery(theme.breakpoints.down('sm'));
  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      slotProps={{ paper: { sx: { width: small ? '100%' : width, maxWidth: '100vw', borderLeft: `1px solid ${theme.palette.divider}`, borderRight: 'none', bgcolor: 'background.default' } } }}
    >
      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, px: 3, py: 2, bgcolor: 'background.paper', borderBottom: '1px solid', borderColor: 'divider', position: 'sticky', top: 0, zIndex: 1 }}>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="h6" component="h2" sx={{ fontWeight: 500, lineHeight: 1.3 }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary" component="div" sx={{ mt: 0.25 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions && <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>{actions}</Box>}
        <IconButton onClick={onClose} aria-label="Close" size="small" sx={{ ml: 0.5 }}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </Box>
      <Box sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2, overflowY: 'auto', flex: 1, '& > *': { flexShrink: 0 } }}>
        {error ? <ErrorState error={error} onRetry={onRetry} /> : loading ? (
          <>
            <Skeleton variant="rounded" height={120} />
            <Skeleton variant="rounded" height={200} />
          </>
        ) : (
          children
        )}
      </Box>
    </Drawer>
  );
}

/** Label/value grid used in drawers. */
export function MetaGrid({ items }: { items: Array<{ label: string; value: ReactNode; hint?: string; full?: boolean }> }) {
  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 1.5 }}>
      {items.map((it) => (
        <Box key={it.label} sx={{ gridColumn: it.full ? '1 / -1' : undefined, minWidth: 0 }}>
          <Typography variant="caption" color="text.secondary" component="div" title={it.hint} sx={{ textTransform: 'uppercase', letterSpacing: '0.06em', fontSize: 10.5 }}>
            {it.label}
          </Typography>
          <Typography variant="body2" component="div" sx={{ mt: 0.25, wordBreak: 'break-word' }}>
            {it.value ?? '—'}
          </Typography>
        </Box>
      ))}
    </Box>
  );
}

export function Section({ title, count, action, children }: { title: string; count?: number; action?: ReactNode; children: ReactNode }) {
  return (
    <Box component="section" sx={{ bgcolor: 'background.paper', border: '1px solid', borderColor: 'divider', borderRadius: 3, overflow: 'hidden' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', px: 2, py: 1.25, borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="subtitle2" sx={{ flex: 1 }}>
          {title}
          {typeof count === 'number' && (
            <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
              {count}
            </Typography>
          )}
        </Typography>
        {action}
      </Box>
      {children}
    </Box>
  );
}
