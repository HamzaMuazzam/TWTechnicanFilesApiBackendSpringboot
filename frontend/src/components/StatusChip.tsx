import { Chip, type ChipProps, useTheme } from '@mui/material';
import { severityPalette, type ToneKey } from '@/theme/theme';
import { toneFor } from '@/theme/tones';

interface StatusChipProps extends Omit<ChipProps, 'label' | 'color' | 'variant'> {
  value: string | null | undefined;
  /** override the automatic tone */
  tone?: ToneKey;
  /** show a label different from the raw value */
  label?: string;
  /** render a dot + plain text instead of a filled chip */
  variant?: 'filled' | 'outlined' | 'dot';
}

export function StatusChip({ value, tone, label, variant = 'filled', size = 'small', sx, ...rest }: StatusChipProps) {
  const theme = useTheme();
  const mode = theme.palette.mode === 'dark' ? 'dark' : 'light';
  const t = tone ?? toneFor(value);
  const colors = severityPalette[mode][t];
  const text = label ?? (value ?? '—');

  if (variant === 'dot') {
    return (
      <Chip
        size={size}
        label={text}
        variant="outlined"
        icon={<span style={{ width: 8, height: 8, borderRadius: 4, background: colors.main, marginLeft: 8 }} />}
        sx={{ borderColor: 'transparent', bgcolor: 'transparent', fontWeight: 500, '& .MuiChip-label': { pl: 0.75 }, ...sx }}
        {...rest}
      />
    );
  }

  return (
    <Chip
      size={size}
      label={text}
      variant={variant}
      sx={{
        bgcolor: variant === 'outlined' ? 'transparent' : colors.container,
        color: colors.main,
        borderColor: variant === 'outlined' ? colors.main : 'transparent',
        fontWeight: 500,
        letterSpacing: '0.01em',
        ...sx,
      }}
      {...rest}
    />
  );
}
