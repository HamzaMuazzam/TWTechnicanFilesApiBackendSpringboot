import { Box, Paper, Skeleton, Typography, useTheme } from '@mui/material';
import type { ReactNode } from 'react';
import { severityPalette, type ToneKey } from '@/theme/theme';

interface KpiTileProps {
  label: string;
  value: ReactNode;
  hint?: string;
  tone?: ToneKey;
  loading?: boolean;
  onClick?: () => void;
}

export function KpiTile({ label, value, hint, tone, loading, onClick }: KpiTileProps) {
  const theme = useTheme();
  const mode = theme.palette.mode === 'dark' ? 'dark' : 'light';
  const accent = tone ? severityPalette[mode][tone].main : undefined;
  return (
    <Paper
      component={onClick ? 'button' : 'div'}
      onClick={onClick}
      sx={{
        p: 2,
        display: 'flex',
        flexDirection: 'column',
        gap: 0.5,
        textAlign: 'left',
        minWidth: 0,
        cursor: onClick ? 'pointer' : 'default',
        font: 'inherit',
        color: 'inherit',
        bgcolor: 'background.paper',
        borderRadius: 3,
        '&:hover': onClick ? { bgcolor: 'action.hover' } : undefined,
        '&:focus-visible': { outline: `2px solid ${theme.palette.primary.main}`, outlineOffset: 2 },
      }}
    >
      <Typography variant="overline" color="text.secondary" sx={{ lineHeight: 1.4 }}>
        {label}
      </Typography>
      {loading ? (
        <Skeleton width="60%" height={36} />
      ) : (
        <Typography variant="h4" sx={{ fontWeight: 400, fontSize: '1.75rem', color: accent ?? 'text.primary', lineHeight: 1.2 }}>
          {value}
        </Typography>
      )}
      <Box sx={{ minHeight: 18 }}>
        {hint && (
          <Typography variant="caption" color="text.secondary">
            {hint}
          </Typography>
        )}
      </Box>
    </Paper>
  );
}
