import { Box, Chip, Typography } from '@mui/material';
import type { FilterClause, FilterLogic } from '@/api/types';
import { OPERATOR_LABELS, type EntityConfig } from '@/entities/types';
import { fmtDateTime } from '@/utils/format';

function describeValue(entity: EntityConfig, f: FilterClause): string {
  const meta = entity.fields.find((m) => m.field === f.field);
  if (f.op === 'isNull' || f.op === 'notNull') return '';
  const fmt = (v: unknown): string => {
    if (v === null || v === undefined) return '…';
    if (meta?.type === 'datetime' && typeof v === 'string') return fmtDateTime(v);
    if (typeof v === 'boolean') return v ? 'Yes' : 'No';
    return String(v);
  };
  if (Array.isArray(f.value)) {
    if (f.op === 'between') return `${fmt(f.value[0])} – ${fmt(f.value[1])}`;
    return f.value.length > 3 ? `${f.value.slice(0, 3).map(fmt).join(', ')} +${f.value.length - 3}` : f.value.map(fmt).join(', ');
  }
  return fmt(f.value);
}

interface FilterChipsProps {
  entity: EntityConfig;
  filters: FilterClause[];
  logic: FilterLogic;
  search: string;
  onRemove: (index: number) => void;
  onClearSearch: () => void;
  onClearAll: () => void;
}

/** Summary of the active server-side filters as removable chips. */
export function FilterChips({ entity, filters, logic, search, onRemove, onClearSearch, onClearAll }: FilterChipsProps) {
  if (filters.length === 0 && !search) return null;
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, flexWrap: 'wrap', mb: 1.5 }}>
      {search && (
        <Chip size="small" variant="outlined" label={`Search: “${search}”`} onDelete={onClearSearch} />
      )}
      {filters.map((f, i) => {
        const meta = entity.fields.find((m) => m.field === f.field);
        const val = describeValue(entity, f);
        return (
          <Chip
            key={`${f.field}-${i}`}
            size="small"
            variant="outlined"
            onDelete={() => onRemove(i)}
            label={
              <span>
                <b style={{ fontWeight: 500 }}>{meta?.label ?? f.field}</b> {OPERATOR_LABELS[f.op]} {val && <span style={{ fontFamily: 'monospace' }}>{val}</span>}
              </span>
            }
          />
        );
      })}
      {filters.length > 1 && (
        <Typography variant="caption" color="text.secondary">
          matched with {logic}
        </Typography>
      )}
      <Chip size="small" label="Clear all" onClick={onClearAll} sx={{ bgcolor: 'transparent' }} />
    </Box>
  );
}
