import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Paper,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import {
  DataGrid,
  GridActionsCellItem,
  GridRowEditStopReasons,
  GridRowModes,
  useGridApiContext,
  type GridColDef,
  type GridEventListener,
  type GridRenderEditCellParams,
  type GridRowModesModel,
} from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import CloseIcon from '@mui/icons-material/Close';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { alertTypesApi } from '@/api/endpoints';
import { queryKeys, useAlertTypes } from '@/api/hooks';
import { SEVERITIES, type AlertTypeRow, type Severity } from '@/api/types';
import { PageHeader } from '@/components/PageHeader';
import { StatusChip } from '@/components/StatusChip';
import { BoolIcon } from '@/components/BoolIcon';
import { ErrorState } from '@/components/ErrorState';
import { EmptyState } from '@/components/EmptyState';
import { AdminOnly } from '@/components/AdminOnly';
import { ColorSwatch } from '@/components/datagrid/cellRenderers';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useConfirm } from '@/components/ConfirmDialog';
import { useAuth } from '@/auth/AuthContext';

function ColorEditCell(props: GridRenderEditCellParams<AlertTypeRow, string | null>) {
  const { id, field, value } = props;
  const apiRef = useGridApiContext();
  const set = (v: string) => apiRef.current.setEditCellValue({ id, field, value: v || null });
  const safe = value && /^#[0-9a-fA-F]{6}$/.test(value) ? value : '#0B57D0';
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 1, width: '100%' }}>
      <input type="color" value={safe} onChange={(e) => set(e.target.value)} aria-label="Pick colour" style={{ width: 28, height: 24, padding: 0, border: 'none', background: 'none', cursor: 'pointer' }} />
      <TextField variant="standard" value={value ?? ''} onChange={(e) => set(e.target.value)} placeholder="#RRGGBB" slotProps={{ input: { disableUnderline: true, sx: { fontFamily: 'monospace', fontSize: 13 } } }} fullWidth />
    </Box>
  );
}

const EMPTY: AlertTypeRow = { code: '', label: '', severity: 'INFO', icon: null, color: null, channelId: null, active: true, sortOrder: null };

export function AlertTypesPage() {
  const { isAdmin } = useAuth();
  const snackbar = useSnackbar();
  const confirm = useConfirm();
  const queryClient = useQueryClient();
  const types = useAlertTypes();
  const [rowModesModel, setRowModesModel] = useState<GridRowModesModel>({});
  const [addOpen, setAddOpen] = useState(false);
  const [draft, setDraft] = useState<AlertTypeRow>(EMPTY);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.alertTypes });
    queryClient.invalidateQueries({ queryKey: queryKeys.entity('alert-types') });
  };

  const update = useMutation({
    mutationFn: (row: AlertTypeRow) => alertTypesApi.update(row.code, row),
    onSuccess: (_, row) => {
      snackbar.success(`Saved ${row.code}`);
      invalidate();
    },
  });
  const create = useMutation({
    mutationFn: (row: AlertTypeRow) => alertTypesApi.create(row),
    onSuccess: (_, row) => {
      snackbar.success(`Created ${row.code}`);
      setAddOpen(false);
      setDraft(EMPTY);
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Create failed'),
  });
  const remove = useMutation({
    mutationFn: (code: string) => alertTypesApi.remove(code),
    onSuccess: (_, code) => {
      snackbar.success(`Deleted ${code}`);
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Delete failed'),
  });

  const onRowEditStop: GridEventListener<'rowEditStop'> = (params, event) => {
    if (params.reason === GridRowEditStopReasons.rowFocusOut) event.defaultMuiPrevented = true; // explicit Save only
  };
  const processRowUpdate = async (newRow: AlertTypeRow) => {
    const cleaned: AlertTypeRow = {
      ...newRow,
      label: newRow.label?.trim() || newRow.code,
      icon: newRow.icon?.trim() || null,
      color: newRow.color?.trim() || null,
      channelId: newRow.channelId?.trim() || null,
      sortOrder: newRow.sortOrder === null || newRow.sortOrder === undefined || Number.isNaN(Number(newRow.sortOrder)) ? null : Number(newRow.sortOrder),
    };
    if (cleaned.color && !/^#[0-9a-fA-F]{6}$/.test(cleaned.color)) throw new Error('Colour must be #RRGGBB');
    await update.mutateAsync(cleaned);
    return cleaned;
  };

  const onDelete = async (row: AlertTypeRow) => {
    const ok = await confirm({
      title: `Delete alert type ${row.code}?`,
      description: 'Existing alerts keep the code; if a new alert with this code arrives it will be auto-created again as INFO.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (ok) remove.mutate(row.code);
  };

  const columns = useMemo<GridColDef<AlertTypeRow>[]>(() => {
    const cols: GridColDef<AlertTypeRow>[] = [
      { field: 'sortOrder', headerName: 'Order', type: 'number', width: 80, editable: isAdmin },
      { field: 'code', headerName: 'Code', width: 170, renderCell: (p) => <Typography component="span" sx={{ fontFamily: 'monospace', fontSize: 13 }}>{p.value}</Typography> },
      { field: 'label', headerName: 'Label', flex: 1, minWidth: 140, editable: isAdmin },
      {
        field: 'severity',
        headerName: 'Severity',
        width: 130,
        type: 'singleSelect',
        valueOptions: SEVERITIES,
        editable: isAdmin,
        renderCell: (p) => <StatusChip value={p.value as Severity} />,
      },
      { field: 'icon', headerName: 'Icon', width: 110, editable: isAdmin, description: 'Material icon name used by the Android app' },
      { field: 'color', headerName: 'Color', width: 150, editable: isAdmin, renderCell: (p) => <ColorSwatch value={p.value as string | null} />, renderEditCell: (p) => <ColorEditCell {...p} /> },
      { field: 'channelId', headerName: 'Channel id', width: 150, editable: isAdmin, renderCell: (p) => <Typography component="span" sx={{ fontFamily: 'monospace', fontSize: 13 }}>{p.value ?? '—'}</Typography> },
      { field: 'active', headerName: 'Active', width: 90, type: 'boolean', editable: isAdmin, renderCell: (p) => <BoolIcon value={p.value as boolean} /> },
    ];
    if (isAdmin) {
      cols.push({
        field: 'actions',
        type: 'actions',
        headerName: '',
        width: 96,
        getActions: ({ id, row }) => {
          const editing = rowModesModel[id]?.mode === GridRowModes.Edit;
          if (editing) {
            return [
              <GridActionsCellItem key="save" icon={<SaveOutlinedIcon />} label="Save" color="primary" onClick={() => setRowModesModel((m) => ({ ...m, [id]: { mode: GridRowModes.View } }))} />,
              <GridActionsCellItem key="cancel" icon={<CloseIcon />} label="Cancel" onClick={() => setRowModesModel((m) => ({ ...m, [id]: { mode: GridRowModes.View, ignoreModifications: true } }))} />,
            ];
          }
          return [
            <GridActionsCellItem key="edit" icon={<EditOutlinedIcon />} label="Edit" onClick={() => setRowModesModel((m) => ({ ...m, [id]: { mode: GridRowModes.Edit } }))} />,
            <GridActionsCellItem key="delete" icon={<DeleteOutlineIcon />} label="Delete" onClick={() => onDelete(row)} showInMenu={false} />,
          ];
        },
      });
    }
    return cols;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin, rowModesModel]);

  const rows = useMemo(() => [...(types.data ?? [])].sort((a, b) => (a.sortOrder ?? 9999) - (b.sortOrder ?? 9999) || a.code.localeCompare(b.code)), [types.data]);

  const canCreate = /^[A-Z][A-Z0-9_]{1,63}$/.test(draft.code) && (!draft.color || /^#[0-9a-fA-F]{6}$/.test(draft.color));

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <PageHeader
        title="Alert types"
        subtitle={isAdmin ? 'This catalogue builds itself: the first alert S3 sends with a new alertType code creates a row here (severity from the keyword rules in Settings). Edit label, severity, icon, colour and channel inline, then Save.' : 'Read-only — the VIEWER role cannot edit alert types. Rows are created automatically from the alertType codes S3 sends.'}
        actions={
          <AdminOnly>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)}>
              Add type
            </Button>
          </AdminOnly>
        }
      />
      {types.isError && <ErrorState error={types.error} onRetry={() => types.refetch()} />}
      <Paper sx={{ flex: 1, minHeight: 420, overflow: 'hidden' }}>
        <DataGrid<AlertTypeRow>
          rows={rows}
          columns={columns}
          getRowId={(r) => r.code}
          loading={types.isPending}
          density="compact"
          editMode="row"
          rowModesModel={rowModesModel}
          onRowModesModelChange={setRowModesModel}
          onRowEditStop={onRowEditStop}
          processRowUpdate={processRowUpdate}
          onProcessRowUpdateError={(e) => snackbar.error(e, 'Save failed')}
          disableRowSelectionOnClick
          disableColumnFilter
          hideFooter={rows.length <= 100}
          slots={{ noRowsOverlay: () => <EmptyState compact title="No alert types" /> }}
          sx={{
            border: 'none',
            '& .MuiDataGrid-columnHeaders': { bgcolor: 'background.default' },
            '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 500, fontSize: '0.75rem', letterSpacing: '0.02em', color: 'text.secondary' },
            '& .MuiDataGrid-cell': { display: 'flex', alignItems: 'center' },
            '& .MuiDataGrid-row--editing': { bgcolor: 'action.selected' },
            '& .MuiDataGrid-cell:focus, & .MuiDataGrid-cell:focus-within': { outline: 'none' },
          }}
        />
      </Paper>

      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add alert type</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, mt: 1 }}>
            <TextField autoFocus label="Code" value={draft.code} onChange={(e) => setDraft({ ...draft, code: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })} helperText="UPPER_SNAKE, e.g. DOOR_OPEN" slotProps={{ input: { sx: { fontFamily: 'monospace' } } }} />
            <TextField label="Label" value={draft.label} onChange={(e) => setDraft({ ...draft, label: e.target.value })} helperText="Shown in the app and notifications" />
            <TextField select label="Severity" value={draft.severity} onChange={(e) => setDraft({ ...draft, severity: e.target.value as Severity })}>
              {SEVERITIES.map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
            <TextField label="Icon" value={draft.icon ?? ''} onChange={(e) => setDraft({ ...draft, icon: e.target.value || null })} helperText="Material icon name (optional)" />
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <input type="color" value={draft.color && /^#[0-9a-fA-F]{6}$/.test(draft.color) ? draft.color : '#0B57D0'} onChange={(e) => setDraft({ ...draft, color: e.target.value })} aria-label="Pick colour" style={{ width: 40, height: 40, border: 'none', background: 'none', padding: 0, cursor: 'pointer' }} />
              <TextField label="Color" value={draft.color ?? ''} onChange={(e) => setDraft({ ...draft, color: e.target.value || null })} placeholder="#RRGGBB" fullWidth error={!!draft.color && !/^#[0-9a-fA-F]{6}$/.test(draft.color)} />
            </Box>
            <TextField label="Channel id" value={draft.channelId ?? ''} onChange={(e) => setDraft({ ...draft, channelId: e.target.value || null })} helperText="Android notification channel (optional)" />
            <TextField label="Sort order" type="number" value={draft.sortOrder ?? ''} onChange={(e) => setDraft({ ...draft, sortOrder: e.target.value === '' ? null : Number(e.target.value) })} />
            <FormControlLabel control={<Switch checked={draft.active} onChange={(e) => setDraft({ ...draft, active: e.target.checked })} />} label="Active" />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!canCreate} loading={create.isPending} onClick={() => create.mutate({ ...draft, label: draft.label.trim() || draft.code })}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
