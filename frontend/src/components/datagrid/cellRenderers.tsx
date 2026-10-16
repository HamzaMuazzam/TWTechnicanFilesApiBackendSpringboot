import { Box, Typography } from '@mui/material';
import { StatusChip } from '@/components/StatusChip';
import { useAlertTypes } from '@/api/hooks';

/** Alert-type chip coloured by the type's severity, labelled with the type's label. */
export function AlertTypeChip({ code }: { code: string | null | undefined }) {
  const { data } = useAlertTypes();
  if (!code) return <Typography variant="body2" color="text.disabled">—</Typography>;
  const t = data?.find((x) => x.code === code);
  return <StatusChip value={t?.severity ?? 'INFO'} label={t?.label ?? code} title={code} />;
}

export function ColorSwatch({ value }: { value: string | null | undefined }) {
  if (!value) return <Typography variant="body2" color="text.disabled">—</Typography>;
  return (
    <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 1 }}>
      <Box sx={{ width: 14, height: 14, borderRadius: 0.75, bgcolor: value, border: '1px solid', borderColor: 'divider' }} />
      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
        {value}
      </Typography>
    </Box>
  );
}
