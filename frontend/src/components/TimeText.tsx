import { Tooltip, Typography, type TypographyProps } from '@mui/material';
import { fmtDateTime, fmtRelative, fmtShort } from '@/utils/format';

/** Absolute time with the relative time (and raw ISO) in a tooltip. `short` drops the year/seconds. */
export function TimeText({ value, relative = false, short = false, ...rest }: { value: string | null | undefined; relative?: boolean; short?: boolean } & TypographyProps) {
  if (!value) {
    return (
      <Typography component="span" variant="body2" color="text.disabled" {...rest}>
        —
      </Typography>
    );
  }
  return (
    <Tooltip title={`${relative ? fmtDateTime(value) : fmtRelative(value)} · ${value}`}>
      <Typography component="span" variant="body2" sx={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }} {...rest}>
        {relative ? fmtRelative(value) : short ? fmtShort(value) : fmtDateTime(value)}
      </Typography>
    </Tooltip>
  );
}
