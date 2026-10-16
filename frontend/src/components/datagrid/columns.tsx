import { Typography } from '@mui/material';
import type { GridColDef, GridRenderCellParams, GridValidRowModel } from '@mui/x-data-grid';
import type { FieldMeta } from '@/entities/types';
import { getPath } from '@/entities/types';
import { StatusChip } from '@/components/StatusChip';
import { TimeText } from '@/components/TimeText';
import { BoolIcon } from '@/components/BoolIcon';
import { fmtCoord, fmtNumber } from '@/utils/format';
import { AlertTypeChip, ColorSwatch } from './cellRenderers';

const mono = { fontFamily: '"Roboto Mono", ui-monospace, SFMono-Regular, Menlo, monospace', fontSize: '0.8125rem' } as const;

export function renderCellFor<R extends GridValidRowModel>(meta: FieldMeta): GridColDef<R>['renderCell'] {
  const render = meta.render ?? (meta.type === 'datetime' ? 'time' : meta.type === 'boolean' ? 'bool' : meta.type === 'number' ? 'number' : 'text');
  switch (render) {
    case 'chip':
      if (meta.enumSource === 'alertTypes') {
        return (p: GridRenderCellParams<R>) => <AlertTypeChip code={p.value as string | null} />;
      }
      return (p: GridRenderCellParams<R>) => (p.value ? <StatusChip value={String(p.value)} /> : <Typography variant="body2" color="text.disabled">—</Typography>);
    case 'time':
      return (p: GridRenderCellParams<R>) => <TimeText value={p.value as string | null} />;
    case 'bool':
      return (p: GridRenderCellParams<R>) => <BoolIcon value={p.value as boolean | null} />;
    case 'mono':
      return (p: GridRenderCellParams<R>) =>
        p.value === null || p.value === undefined || p.value === '' ? (
          <Typography variant="body2" color="text.disabled">—</Typography>
        ) : (
          <Typography component="span" sx={mono} noWrap title={String(p.value)}>
            {String(p.value)}
          </Typography>
        );
    case 'number':
      return (p: GridRenderCellParams<R>) => (
        <Typography component="span" variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          {fmtNumber(p.value as number | null)}
        </Typography>
      );
    case 'coord':
      return (p: GridRenderCellParams<R>) => (
        <Typography component="span" variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          {fmtCoord(p.value as number | null)}
        </Typography>
      );
    case 'color':
      return (p: GridRenderCellParams<R>) => <ColorSwatch value={p.value as string | null} />;
    default:
      return (p: GridRenderCellParams<R>) =>
        p.value === null || p.value === undefined || p.value === '' ? (
          <Typography variant="body2" color="text.disabled">—</Typography>
        ) : (
          <Typography variant="body2" noWrap title={String(p.value)}>
            {String(p.value)}
          </Typography>
        );
  }
}

/** Build a DataGrid column from field metadata. */
export function columnFromMeta<R extends GridValidRowModel>(meta: FieldMeta): GridColDef<R> {
  const align = meta.type === 'number' && (meta.render ?? 'number') === 'number' ? 'right' : meta.type === 'boolean' ? 'center' : 'left';
  return {
    field: meta.field,
    headerName: meta.label,
    description: meta.hint,
    width: meta.width,
    minWidth: meta.minWidth ?? (meta.flex ? 120 : undefined),
    flex: meta.flex,
    sortable: meta.sortable !== false,
    filterable: false,
    align,
    headerAlign: align,
    valueGetter: (_value, row) => getPath(row, meta.field),
    renderCell: renderCellFor<R>(meta),
  };
}
