import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import { DataGridPage } from '@/components/datagrid/DataGridPage';
import { sourcesEntity } from '@/entities';
import type { SourceRow } from '@/api/types';
import { sourcesApi } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useConfirm } from '@/components/ConfirmDialog';
import { AdminOnly } from '@/components/AdminOnly';
import { CopyButton } from '@/components/CopyButton';
import { useAuth } from '@/auth/AuthContext';

function KeyRevealDialog({ open, name, apiKey, onClose }: { open: boolean; name: string; apiKey: string | null; onClose: () => void }) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth disableEscapeKeyDown>
      <DialogTitle>API key for “{name}”</DialogTitle>
      <DialogContent>
        <Alert severity="warning" variant="outlined" sx={{ mb: 2 }}>
          This key is shown <b>once</b>. Copy it now and store it securely — the server keeps only a hash. If it is lost, rotate the key again.
        </Alert>
        {apiKey ? (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, p: 1.5, bgcolor: 'background.default', border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
            <Typography component="code" sx={{ fontFamily: 'monospace', fontSize: 14, flex: 1, wordBreak: 'break-all', userSelect: 'all' }}>
              {apiKey}
            </Typography>
            <CopyButton text={apiKey} label="Copy key" />
          </Box>
        ) : (
          <Alert severity="error" variant="outlined">
            The server did not return a plaintext key in the response. Check the backend’s rotate-key payload.
          </Alert>
        )}
        <DialogContentText sx={{ mt: 2, fontSize: 13 }}>
          Send it as the <code>X-Api-Key</code> header on <code>POST /api/v1/ingest/alerts</code>.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button variant="contained" onClick={onClose}>
          I have copied the key
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export function SourcesPage() {
  const snackbar = useSnackbar();
  const confirm = useConfirm();
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();
  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState('');
  const [reveal, setReveal] = useState<{ name: string; key: string | null } | null>(null);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: queryKeys.entity('sources') });

  const create = useMutation({
    mutationFn: () => sourcesApi.create({ name: name.trim() }),
    onSuccess: (res) => {
      snackbar.success(`Source “${name.trim()}” created`);
      setCreateOpen(false);
      setReveal({ name: name.trim(), key: res.apiKey });
      setName('');
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Create failed'),
  });

  const rotate = useMutation({
    mutationFn: (s: SourceRow) => sourcesApi.rotateKey(s.id),
    onSuccess: (res, s) => {
      setReveal({ name: s.name, key: res.apiKey });
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Rotate key failed'),
  });

  const setActive = useMutation({
    mutationFn: ({ s, active }: { s: SourceRow; active: boolean }) => sourcesApi.setActive(s.id, active),
    onSuccess: (_, { s, active }) => {
      snackbar.success(`${s.name} ${active ? 'activated' : 'deactivated'}`);
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Update failed'),
  });

  const onRotate = async (s: SourceRow) => {
    const ok = await confirm({
      title: `Rotate key for “${s.name}”?`,
      description: 'The current key stops working immediately. Any producer using it (e.g. S3) must be updated with the new key.',
      confirmLabel: 'Rotate key',
      danger: true,
    });
    if (ok) rotate.mutate(s);
  };

  const actions: GridColDef<SourceRow> = {
    field: '__actions',
    headerName: 'Actions',
    width: 130,
    sortable: false,
    filterable: false,
    hideable: false,
    disableColumnMenu: true,
    align: 'right',
    headerAlign: 'right',
    renderCell: (p) => (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }} onClick={(e) => e.stopPropagation()}>
        <Tooltip title={isAdmin ? (p.row.active ? 'Deactivate' : 'Activate') : 'Requires the ADMIN role'}>
          <span>
            <Switch size="small" checked={p.row.active} disabled={!isAdmin || setActive.isPending} onChange={(e) => setActive.mutate({ s: p.row, active: e.target.checked })} inputProps={{ 'aria-label': 'Active' }} />
          </span>
        </Tooltip>
        <Tooltip title={isAdmin ? 'Rotate API key' : 'Requires the ADMIN role'}>
          <span>
            <IconButton size="small" disabled={!isAdmin || rotate.isPending} onClick={() => onRotate(p.row)} aria-label="Rotate key">
              <AutorenewIcon fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
      </Box>
    ),
  };

  return (
    <DataGridPage<SourceRow>
      entity={sourcesEntity}
      extraColumns={[actions]}
      headerActions={
        <AdminOnly>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New source
          </Button>
        </AdminOnly>
      }
    >
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>New ingest source</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ fontSize: 13, mb: 1 }}>
            A source is a producer that pushes alerts (for example the S3 tracking server). Its API key is generated on creation.
          </DialogContentText>
          <TextField
            autoFocus
            fullWidth
            label="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && name.trim() && create.mutate()}
            sx={{ mt: 1 }}
            helperText="Unique, e.g. S3, S3-staging"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => create.mutate()} disabled={!name.trim()} loading={create.isPending}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
      <KeyRevealDialog open={!!reveal} name={reveal?.name ?? ''} apiKey={reveal?.key ?? null} onClose={() => setReveal(null)} />
    </DataGridPage>
  );
}
