import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import RemoveIcon from '@mui/icons-material/Remove';
import { Tooltip, useTheme } from '@mui/material';
import { severityPalette } from '@/theme/theme';

export function BoolIcon({ value, trueLabel = 'Yes', falseLabel = 'No' }: { value: boolean | null | undefined; trueLabel?: string; falseLabel?: string }) {
  const theme = useTheme();
  const mode = theme.palette.mode === 'dark' ? 'dark' : 'light';
  if (value === null || value === undefined) {
    return (
      <Tooltip title="Unknown">
        <RemoveIcon fontSize="small" sx={{ color: 'text.disabled', verticalAlign: 'middle' }} />
      </Tooltip>
    );
  }
  return value ? (
    <Tooltip title={trueLabel}>
      <CheckCircleOutlineIcon fontSize="small" sx={{ color: severityPalette[mode].SUCCESS.main, verticalAlign: 'middle' }} />
    </Tooltip>
  ) : (
    <Tooltip title={falseLabel}>
      <CancelOutlinedIcon fontSize="small" sx={{ color: severityPalette[mode].CRITICAL.main, verticalAlign: 'middle' }} />
    </Tooltip>
  );
}
