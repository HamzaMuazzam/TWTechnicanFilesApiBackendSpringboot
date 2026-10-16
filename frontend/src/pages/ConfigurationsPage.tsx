import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, Stack, TextField, Tooltip, Typography } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { configurationsApi, type ConfigurationInput } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import type { ConfigurationRow } from '@/api/types';
import { DataGridPage } from '@/components/datagrid/DataGridPage';
import { configurationsEntity } from '@/entities';
import { AdminOnly } from '@/components/AdminOnly';
import { useConfirm } from '@/components/ConfirmDialog';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useAuth } from '@/auth/AuthContext';

const EMPTY: ConfigurationInput = { phoneNumber: '', uid: '', alertType: '', crtBy: 'dashboard' };

/**
 * Who receives alerts: one row = (tracker uid, alert type or '*', phone number).
 * Normally pushed by S3 to POST /api/v1/ingest/configurations; this page lets you add/remove rows by hand.
 */
export function ConfigurationsPage() {
  const { isAdmin } = useAuth();
  const snackbar = useSnackbar();
  const confirm = useConfirm();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<ConfigurationInput>(EMPTY);
  const invalidate = () => queryClient.invalidateQueries({ queryKey: queryKeys.entity('configurations') });

  const create = useMutation({
    mutationFn: (body: ConfigurationInput) =>
      configurationsApi.create({ ...body, alertType: body.alertType?.trim() || '*', id: body.id?.trim() || undefined }),
    onSuccess: (row) => {
      snackbar.success(`${row.phoneE164} will receive ${row.alertType === '*' ? 'all alerts' : row.alertType} for uid ${row.uid}`);
      setOpen(false);
      setDraft(EMPTY);
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Could not save'),
  });

  const remove = useMutation({
    mutationFn: (row: ConfigurationRow) => configurationsApi.remove(row.id),
    onSuccess: () => {
      snackbar.success('Configuration removed');
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Delete failed'),
  });

  const actions: GridColDef<ConfigurationRow> = {
    field: '__actions',
    headerName: '',
    width: 56,
    sortable: false,
    filterable: false,
    hideable: false,
    disableColumnMenu: true,
    align: 'center',
    renderCell: (p) =>
      isAdmin ? (
        <Tooltip title="Remove">
          <IconButton
            size="small"
            aria-label="Remove configuration"
            onClick={async (e) => {
              e.stopPropagation();
              if (await confirm({ title: `Stop sending uid ${p.row.uid} alerts to ${p.row.phoneE164}?`, confirmLabel: 'Remove', danger: true })) {
                remove.mutate(p.row);
              }
            }}
          >
            <DeleteOutlineIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      ) : null,
  };

  const valid = draft.phoneNumber.trim().length >= 10 && draft.uid.trim().length > 0;

  return (
    <>
      <DataGridPage<ConfigurationRow>
        entity={configurationsEntity}
        extraColumns={[actions]}
        headerActions={
          <AdminOnly>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
              Add configuration
            </Button>
          </AdminOnly>
        }
      />
      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Add configuration</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            The phone number receives alerts for this tracker. Leave alert type empty for all types.
          </Typography>
          <Stack spacing={2}>
            <TextField label="Tracker UID" value={draft.uid} onChange={(e) => setDraft({ ...draft, uid: e.target.value })} autoFocus fullWidth required />
            <TextField label="Phone number" placeholder="03001234567" value={draft.phoneNumber} onChange={(e) => setDraft({ ...draft, phoneNumber: e.target.value })} fullWidth required />
            <TextField label="Alert type" placeholder="* (all)" value={draft.alertType} onChange={(e) => setDraft({ ...draft, alertType: e.target.value.toUpperCase() })} fullWidth />
            <TextField label="S3 id (optional)" value={draft.id ?? ''} onChange={(e) => setDraft({ ...draft, id: e.target.value })} fullWidth helperText="If S3 later pushes the same id, it updates this row" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!valid} loading={create.isPending} onClick={() => create.mutate(draft)}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
