import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField, Typography } from '@mui/material';
import PersonAddAltOutlinedIcon from '@mui/icons-material/PersonAddAltOutlined';
import { DataGridPage } from '@/components/datagrid/DataGridPage';
import { usersEntity } from '@/entities';
import type { UserRow } from '@/api/types';
import { usersApi } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import { AdminOnly } from '@/components/AdminOnly';
import { useSnackbar } from '@/components/SnackbarProvider';
import { UserDrawer } from './UserDrawer';

const EMPTY = { phone: '', password: '', displayName: '' };

export function UsersPage() {
  const [params, setParams] = useSearchParams();
  const raw = params.get('user');
  const userId = raw && /^\d+$/.test(raw) ? Number(raw) : null;
  const snackbar = useSnackbar();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(EMPTY);

  const setUser = (id: number | null) =>
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (id === null) next.delete('user');
        else next.set('user', String(id));
        return next;
      },
      { replace: true },
    );

  const create = useMutation({
    mutationFn: () => usersApi.create({ phone: draft.phone.trim(), password: draft.password, displayName: draft.displayName.trim() || undefined }),
    onSuccess: (row) => {
      snackbar.success(`${row.phoneE164} can now sign in to the app`);
      setOpen(false);
      setDraft(EMPTY);
      queryClient.invalidateQueries({ queryKey: queryKeys.entity('users') });
      setUser(row.id);
    },
    onError: (e) => snackbar.error(e, 'Could not create user'),
  });

  const valid = draft.phone.trim().length >= 10 && draft.password.length >= 4;

  return (
    <>
      <DataGridPage<UserRow>
        entity={usersEntity}
        onRowClick={(row) => setUser(row.id)}
        selectedId={userId}
        headerActions={
          <AdminOnly>
            <Button variant="contained" startIcon={<PersonAddAltOutlinedIcon />} onClick={() => setOpen(true)}>
              Add user
            </Button>
          </AdminOnly>
        }
      >
        <UserDrawer userId={userId} onClose={() => setUser(null)} />
      </DataGridPage>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Add app user</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            The customer signs in to the app with this phone number and password. The password is stored hashed.
          </Typography>
          <Stack spacing={2}>
            <TextField label="Phone number" placeholder="03001234567" value={draft.phone} onChange={(e) => setDraft({ ...draft, phone: e.target.value })} autoFocus fullWidth required />
            <TextField label="Password" type="password" value={draft.password} onChange={(e) => setDraft({ ...draft, password: e.target.value })} fullWidth required helperText="At least 4 characters" />
            <TextField label="Display name (optional)" value={draft.displayName} onChange={(e) => setDraft({ ...draft, displayName: e.target.value })} fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!valid} loading={create.isPending} onClick={() => create.mutate()}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
