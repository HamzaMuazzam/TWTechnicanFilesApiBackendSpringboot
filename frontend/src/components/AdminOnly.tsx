import { Tooltip, Box } from '@mui/material';
import { cloneElement, isValidElement, type ReactElement } from 'react';
import { useAuth } from '@/auth/AuthContext';

interface AdminOnlyProps {
  children: ReactElement<{ disabled?: boolean }>;
  /** 'hide' removes the control for VIEWER; 'disable' (default) greys it out with a tooltip. */
  mode?: 'hide' | 'disable';
  reason?: string;
}

/** Wrap mutating controls so VIEWER sees a disabled control with an explanation. */
export function AdminOnly({ children, mode = 'disable', reason = 'Requires the ADMIN role' }: AdminOnlyProps) {
  const { isAdmin } = useAuth();
  if (isAdmin) return children;
  if (mode === 'hide') return null;
  if (!isValidElement(children)) return null;
  return (
    <Tooltip title={reason}>
      <Box component="span" sx={{ display: 'inline-flex' }}>
        {cloneElement(children, { disabled: true })}
      </Box>
    </Tooltip>
  );
}
