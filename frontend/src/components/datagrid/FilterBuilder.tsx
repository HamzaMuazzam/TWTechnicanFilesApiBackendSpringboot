import { useState } from 'react';
import {
  Box,
  Button,
  Collapse,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Select,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CloseIcon from '@mui/icons-material/Close';
import type { FilterClause, FilterLogic, FilterOp, FilterValue } from '@/api/types';
import { OPERATORS_BY_TYPE, OPERATOR_LABELS, type EntityConfig, type FieldMeta } from '@/entities/types';
import { FilterValueInput } from './FilterValueInput';

interface DraftRow {
  key: number;
  field: string;
  op: FilterOp;
  value: FilterValue | undefined;
}

interface FilterBuilderProps {
  open: boolean;
  entity: EntityConfig;
  filters: FilterClause[];
  logic: FilterLogic;
  onApply: (filters: FilterClause[], logic: FilterLogic) => void;
  onClear: () => void;
  onClose: () => void;
}

let keySeq = 1;
const newKey = () => keySeq++;

function defaultOp(meta: FieldMeta): FilterOp {
  return OPERATORS_BY_TYPE[meta.type][0];
}

function isComplete(row: DraftRow): boolean {
  if (row.op === 'isNull' || row.op === 'notNull') return true;
  if (row.value === undefined || row.value === null || row.value === '') return false;
  if (Array.isArray(row.value)) {
    if (row.op === 'between') return row.value.length === 2 && row.value[0] !== null && row.value[1] !== null && row.value[0] !== '' && row.value[1] !== '';
    return row.value.length > 0;
  }
  return true;
}

export function FilterBuilder({ open, entity, filters, logic, onApply, onClear, onClose }: FilterBuilderProps) {
  const filterableFields = entity.fields.filter((f) => f.filterable !== false);
  const metaOf = (field: string) => filterableFields.find((f) => f.field === field) ?? filterableFields[0];

  const toDraft = (list: FilterClause[]): DraftRow[] =>
    list.map((f) => ({ key: newKey(), field: f.field, op: f.op, value: f.value }));

  const [rows, setRows] = useState<DraftRow[]>(() => toDraft(filters));
  const [draftLogic, setDraftLogic] = useState<FilterLogic>(logic);

  // Re-sync the draft when the applied filters change from outside (URL, saved filter, chip delete).
  const appliedKey = JSON.stringify({ filters, logic });
  const [syncedKey, setSyncedKey] = useState(appliedKey);
  if (syncedKey !== appliedKey) {
    setSyncedKey(appliedKey);
    setRows(toDraft(filters));
    setDraftLogic(logic);
  }

  const addRow = () => {
    const first = filterableFields[0];
    setRows((r) => [...r, { key: newKey(), field: first.field, op: defaultOp(first), value: undefined }]);
  };

  const patchRow = (key: number, patch: Partial<DraftRow>) =>
    setRows((r) => r.map((row) => (row.key === key ? { ...row, ...patch } : row)));

  const apply = () => {
    const clean: FilterClause[] = rows.filter(isComplete).map((r) => ({
      field: r.field,
      op: r.op,
      ...(r.op === 'isNull' || r.op === 'notNull' ? {} : { value: r.value }),
    }));
    onApply(clean, draftLogic);
  };

  const incomplete = rows.filter((r) => !isComplete(r)).length;

  return (
    <Collapse in={open} unmountOnExit>
      <Paper
        sx={{ mb: 1.5, p: 2, bgcolor: 'background.paper' }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) apply();
          if (e.key === 'Escape') onClose();
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1.5, flexWrap: 'wrap' }}>
          <Typography variant="subtitle2">Filters</Typography>
          <ToggleButtonGroup
            size="small"
            exclusive
            value={draftLogic}
            onChange={(_, v: FilterLogic | null) => v && setDraftLogic(v)}
            sx={{ '& .MuiToggleButton-root': { px: 1.5, py: 0.25, textTransform: 'none', fontSize: 12 } }}
          >
            <ToggleButton value="AND">Match all (AND)</ToggleButton>
            <ToggleButton value="OR">Match any (OR)</ToggleButton>
          </ToggleButtonGroup>
          <Box sx={{ flex: 1 }} />
          <Tooltip title="Close (Esc)">
            <IconButton size="small" onClick={onClose} aria-label="Close filter panel">
              <CloseIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>

        {rows.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            No conditions yet. Add one below.
          </Typography>
        )}

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          {rows.map((row, idx) => {
            const meta = metaOf(row.field);
            const ops = OPERATORS_BY_TYPE[meta.type];
            return (
              <Box
                key={row.key}
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', md: '56px minmax(160px, 220px) minmax(120px, 160px) 1fr 36px' },
                  gap: 1,
                  alignItems: 'start',
                }}
              >
                <Typography variant="caption" color="text.secondary" sx={{ pt: 1.1, textAlign: { md: 'right' }, pr: 1 }}>
                  {idx === 0 ? 'Where' : draftLogic}
                </Typography>
                <Select
                  size="small"
                  value={meta.field}
                  onChange={(e) => {
                    const m = metaOf(e.target.value);
                    patchRow(row.key, { field: m.field, op: defaultOp(m), value: undefined });
                  }}
                  renderValue={(v) => metaOf(String(v)).label}
                  MenuProps={{ PaperProps: { sx: { maxHeight: 360 } } }}
                >
                  {filterableFields.map((f) => (
                    <MenuItem key={f.field} value={f.field} dense>
                      <Box>
                        <Typography variant="body2">{f.label}</Typography>
                        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                          {f.field}
                        </Typography>
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
                <Select
                  size="small"
                  value={row.op}
                  onChange={(e) => {
                    const op = e.target.value as FilterOp;
                    // reset the value when the shape changes (scalar ↔ array)
                    const wasArray = row.op === 'in' || row.op === 'between';
                    const isArray = op === 'in' || op === 'between';
                    patchRow(row.key, { op, value: wasArray !== isArray ? undefined : row.value });
                  }}
                >
                  {ops.map((op) => (
                    <MenuItem key={op} value={op} dense>
                      {OPERATOR_LABELS[op]}
                    </MenuItem>
                  ))}
                </Select>
                <Box sx={{ minHeight: 40 }}>
                  <FilterValueInput meta={meta} op={row.op} value={row.value} onChange={(v) => patchRow(row.key, { value: v })} />
                </Box>
                <Tooltip title="Remove condition">
                  <IconButton size="small" onClick={() => setRows((r) => r.filter((x) => x.key !== row.key))} aria-label="Remove condition" sx={{ mt: 0.25 }}>
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Box>
            );
          })}
        </Box>

        <Divider sx={{ my: 1.5 }} />
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
          <Button size="small" startIcon={<AddIcon />} onClick={addRow}>
            Add condition
          </Button>
          <Box sx={{ flex: 1 }} />
          {incomplete > 0 && (
            <Typography variant="caption" color="text.secondary">
              {incomplete} incomplete condition{incomplete > 1 ? 's' : ''} will be ignored
            </Typography>
          )}
          <Button
            size="small"
            onClick={() => {
              setRows([]);
              onClear();
            }}
            disabled={rows.length === 0 && filters.length === 0}
          >
            Clear
          </Button>
          <Tooltip title="Ctrl/⌘ + Enter">
            <span>
              <Button size="small" variant="contained" onClick={apply}>
                Apply
              </Button>
            </span>
          </Tooltip>
        </Box>
      </Paper>
    </Collapse>
  );
}
