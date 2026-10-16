import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  MenuItem,
  Switch,
  TextField,
  Tooltip,
} from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { DataGridPage } from '@/components/datagrid/DataGridPage';
import { adminsEntity } from '@/entities';
import { ADMIN_ROLES, type AdminRole, type AdminRow } from '@/api/types';
import { adminsApi } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useConfirm } from '@/components/ConfirmDialog';
import { useAuth } from '@/auth/AuthContext';

interface Draft {
  id?: number;
  username: string;
  password: string;
  role: AdminRole;
  active: boolean;
}
const EMPTY: Draft = { username: '', password: '', role: 'VIEWER', active: true };

export function AdminsPage() {
  const snackbar = useSnackbar();
  const confirm = useConfirm();
  const queryClient = useQueryClient();
  const { admin: me } = useAuth();
  const [draft, setDraft] = useState<Draft | null>(null);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: queryKeys.entity('admins') });
  const isEdit = draft?.id !== undefined;

  const save = useMutation({
    mutationFn: (d: Draft) =>
      d.id !== undefined
        ? adminsApi.update(d.id, { role: d.role, active: d.active, ...(d.password ? { password: d.password } : {}) })
        : adminsApi.create({ username: d.username.trim(), password: d.password, role: d.role, active: d.active }),
    onSuccess: (_, d) => {
      snackbar.success(d.id !== undefined ? `Updated ${d.username}` : `Created ${d.username}`);
      setDraft(null);
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Save failed'),
  });
  const remove = useMutation({
    mutationFn: (row: AdminRow) => adminsApi.remove(row.id),
    onSuccess: (_, row) => {
      snackbar.success(`Deleted ${row.username}`);
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Delete failed'),
  });

  const onDelete = async (row: AdminRow) => {
    if (await confirm({ title: `Delete admin “${row.username}”?`, description: 'They will be signed out immediately and cannot sign in again.', confirmLabel: 'Delete', danger: true })) {
      remove.mutate(row);
    }
  };

  const actions: GridColDef<AdminRow> = {
    field: '__actions',
    headerName: '',
    width: 90,
    sortable: false,
    filterable: false,
    hideable: false,
    disableColumnMenu: true,
    align: 'right',
    renderCell: (p) => {
      const self = p.row.id === me?.id;
      return (
        <Box onClick={(e) => e.stopPropagation()}>
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => setDraft({ id: p.row.id, username: p.row.username, password: '', role: p.row.role, active: p.row.active })} aria-label="Edit">
              <EditOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title={self ? 'You cannot delete your own account' : 'Delete'}>
            <span>
              <IconButton size="small" disabled={self} onClick={() => onDelete(p.row)} aria-label="Delete">
                <DeleteOutlineIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </Box>
      );
    },
  };

  const valid = !!draft && (isEdit || (draft.username.trim().length >= 3 && draft.password.length >= 8)) && (!draft.password || draft.password.length >= 8);

  return (
    <DataGridPage<AdminRow>
      entity={adminsEntity}
      subtitle="Dashboard accounts. ADMIN can change data; VIEWER is read-only."
      extraColumns={[actions]}
      headerActions={
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDraft({ ...EMPTY })}>
          New admin
        </Button>
      }
    >
      <Dialog open={!!draft} onClose={() => setDraft(null)} maxWidth="xs" fullWidth>
        <DialogTitle>{isEdit ? `Edit ${draft?.username}` : 'New admin'}</DialogTitle>
        {draft && (
          <DialogContent>
            <Box sx={{ display: 'grid', gap: 2, mt: 1 }}>
              {!isEdit && <TextField autoFocus label="Username" value={draft.username} onChange={(e) => setDraft({ ...draft, username: e.target.value })} autoComplete="off" helperText="At least 3 characters" />}
              <TextField
                label={isEdit ? 'New password (leave blank to keep)' : 'Password'}
                type="password"
                value={draft.password}
                onChange={(e) => setDraft({ ...draft, password: e.target.value })}
                autoComplete="new-password"
                helperText="At least 8 characters"
                error={!!draft.password && draft.password.length < 8}
              />
              <TextField select label="Role" value={draft.role} onChange={(e) => setDraft({ ...draft, role: e.target.value as AdminRole })} disabled={isEdit && draft.id === me?.id} helperText={isEdit && draft.id === me?.id ? 'You cannot change your own role' : undefined}>
                {ADMIN_ROLES.map((r) => (
                  <MenuItem key={r} value={r}>
                    {r}
                  </MenuItem>
                ))}
              </TextField>
              <FormControlLabel control={<Switch checked={draft.active} onChange={(e) => setDraft({ ...draft, active: e.target.checked })} disabled={isEdit && draft.id === me?.id} />} label="Active" />
            </Box>
          </DialogContent>
        )}
        <DialogActions>
          <Button onClick={() => setDraft(null)}>Cancel</Button>
          <Button variant="contained" disabled={!valid} loading={save.isPending} onClick={() => draft && save.mutate(draft)}>
            {isEdit ? 'Save' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </DataGridPage>
  );
}
