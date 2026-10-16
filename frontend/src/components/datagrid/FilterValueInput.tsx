import { Box, Checkbox, Chip, ListItemText, MenuItem, Select, TextField } from '@mui/material';
import type { FilterOp, FilterValue } from '@/api/types';
import type { FieldMeta } from '@/entities/types';
import { isoToLocalInput, localInputToIso } from '@/utils/format';
import { useAlertTypes } from '@/api/hooks';

interface Props {
  meta: FieldMeta;
  op: FilterOp;
  value: FilterValue | undefined;
  onChange: (v: FilterValue | undefined) => void;
  autoFocus?: boolean;
}

function asArray(v: FilterValue | undefined): Array<string | number | boolean | null> {
  return Array.isArray(v) ? v : v === undefined || v === null || v === '' ? [] : [v];
}

/** Type-aware editor for one filter clause value. */
export function FilterValueInput({ meta, op, value, onChange, autoFocus }: Props) {
  const alertTypes = useAlertTypes();
  if (op === 'isNull' || op === 'notNull') return null;

  const options: readonly string[] =
    meta.enumSource === 'alertTypes' ? (alertTypes.data?.map((t) => t.code) ?? []) : (meta.options ?? []);

  // ---- enum ----
  if (meta.type === 'enum') {
    if (op === 'in') {
      const selected = asArray(value).map(String);
      return (
        <Select
          multiple
          size="small"
          fullWidth
          autoFocus={autoFocus}
          displayEmpty
          value={selected}
          onChange={(e) => onChange(typeof e.target.value === 'string' ? e.target.value.split(',') : e.target.value)}
          renderValue={(sel) =>
            sel.length === 0 ? (
              <Box component="span" sx={{ color: 'text.disabled' }}>Select values…</Box>
            ) : (
              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                {sel.map((s) => (
                  <Chip key={s} label={s} size="small" />
                ))}
              </Box>
            )
          }
        >
          {options.map((o) => (
            <MenuItem key={o} value={o} dense>
              <Checkbox size="small" checked={selected.includes(o)} sx={{ p: 0.5, mr: 1 }} />
              <ListItemText primary={o} />
            </MenuItem>
          ))}
        </Select>
      );
    }
    return (
      <Select
        size="small"
        fullWidth
        autoFocus={autoFocus}
        displayEmpty
        value={value === undefined || value === null ? '' : String(value)}
        onChange={(e) => onChange(e.target.value === '' ? undefined : e.target.value)}
        renderValue={(v) => (v === '' ? <Box component="span" sx={{ color: 'text.disabled' }}>Select…</Box> : v)}
      >
        {options.map((o) => (
          <MenuItem key={o} value={o} dense>
            {o}
          </MenuItem>
        ))}
      </Select>
    );
  }

  // ---- boolean ----
  if (meta.type === 'boolean') {
    return (
      <Select
        size="small"
        fullWidth
        autoFocus={autoFocus}
        value={(value === true ? 'true' : value === false ? 'false' : '') as string}
        displayEmpty
        onChange={(e) => onChange(e.target.value === '' ? undefined : e.target.value === 'true')}
        renderValue={(v: string) => (v === '' ? <Box component="span" sx={{ color: 'text.disabled' }}>Select…</Box> : v === 'true' ? 'Yes' : 'No')}
      >
        <MenuItem value="true" dense>Yes</MenuItem>
        <MenuItem value="false" dense>No</MenuItem>
      </Select>
    );
  }

  // ---- datetime ----
  if (meta.type === 'datetime') {
    if (op === 'between') {
      const [a, b] = asArray(value);
      return (
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <TextField
            type="datetime-local"
            size="small"
            fullWidth
            autoFocus={autoFocus}
            value={isoToLocalInput(a as string | null)}
            onChange={(e) => onChange([localInputToIso(e.target.value), (b as string | null) ?? null])}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <Box component="span" sx={{ color: 'text.secondary', fontSize: 12 }}>to</Box>
          <TextField
            type="datetime-local"
            size="small"
            fullWidth
            value={isoToLocalInput(b as string | null)}
            onChange={(e) => onChange([(a as string | null) ?? null, localInputToIso(e.target.value)])}
            slotProps={{ inputLabel: { shrink: true } }}
          />
        </Box>
      );
    }
    return (
      <TextField
        type="datetime-local"
        size="small"
        fullWidth
        autoFocus={autoFocus}
        value={isoToLocalInput(typeof value === 'string' ? value : null)}
        onChange={(e) => onChange(localInputToIso(e.target.value) ?? undefined)}
      />
    );
  }

  // ---- number ----
  if (meta.type === 'number') {
    if (op === 'between') {
      const [a, b] = asArray(value);
      const num = (s: string) => (s === '' ? null : Number(s));
      return (
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <TextField type="number" size="small" fullWidth autoFocus={autoFocus} placeholder="min" value={a ?? ''} onChange={(e) => onChange([num(e.target.value), (b as number | null) ?? null])} />
          <Box component="span" sx={{ color: 'text.secondary', fontSize: 12 }}>to</Box>
          <TextField type="number" size="small" fullWidth placeholder="max" value={b ?? ''} onChange={(e) => onChange([(a as number | null) ?? null, num(e.target.value)])} />
        </Box>
      );
    }
    if (op === 'in') {
      return (
        <TextField
          size="small"
          fullWidth
          autoFocus={autoFocus}
          placeholder="1, 2, 3"
          helperText="Comma-separated"
          value={asArray(value).join(', ')}
          onChange={(e) =>
            onChange(
              e.target.value
                .split(',')
                .map((s) => s.trim())
                .filter(Boolean)
                .map(Number)
                .filter((n) => !Number.isNaN(n)),
            )
          }
        />
      );
    }
    return (
      <TextField
        type="number"
        size="small"
        fullWidth
        autoFocus={autoFocus}
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value === '' ? undefined : Number(e.target.value))}
      />
    );
  }

  // ---- text ----
  if (op === 'in') {
    return (
      <TextField
        size="small"
        fullWidth
        autoFocus={autoFocus}
        placeholder="a, b, c"
        helperText="Comma-separated"
        value={asArray(value).join(', ')}
        onChange={(e) => onChange(e.target.value.split(',').map((s) => s.trim()).filter(Boolean))}
      />
    );
  }
  return (
    <TextField
      size="small"
      fullWidth
      autoFocus={autoFocus}
      placeholder="Value"
      value={value === undefined || value === null ? '' : String(value)}
      onChange={(e) => onChange(e.target.value === '' ? undefined : e.target.value)}
    />
  );
}
