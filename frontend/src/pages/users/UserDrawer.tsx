import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Menu,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import LogoutIcon from '@mui/icons-material/Logout';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import { usersApi } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import type { DeviceRow } from '@/api/types';
import { DetailDrawer, MetaGrid, Section } from '@/components/DetailDrawer';
import { StatusChip } from '@/components/StatusChip';
import { TimeText } from '@/components/TimeText';
import { BoolIcon } from '@/components/BoolIcon';
import { EmptyState } from '@/components/EmptyState';
import { AdminOnly } from '@/components/AdminOnly';
import { AlertTypeChip } from '@/components/datagrid/cellRenderers';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useConfirm } from '@/components/ConfirmDialog';
import { CopyButton } from '@/components/CopyButton';
import { filtersToSearch } from '@/components/datagrid/useGridQueryState';

function DeviceRowItem({ d, onRemove }: { d: DeviceRow; onRemove: (d: DeviceRow) => void }) {
  return (
    <TableRow hover sx={{ opacity: d.active ? 1 : 0.6 }}>
      <TableCell>
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {`${d.manufacturer ?? ''} ${d.model ?? ''}`.trim() || `Device #${d.id}`}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
          Android {d.osVersion ?? '?'} · app {d.appVersion ?? '?'}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace', display: 'inline-flex', alignItems: 'center', gap: 0.5 }} title="deviceId">
          {d.deviceId.slice(0, 8)}… <CopyButton text={d.deviceId} label="Copy device id" sx={{ p: 0.25 }} />
        </Typography>
      </TableCell>
      <TableCell align="center">
        <StatusChip value={d.active ? 'ACTIVE' : 'DISABLED'} label={d.active ? 'Active' : 'Inactive'} />
      </TableCell>
      <TableCell align="center"><BoolIcon value={d.notificationsEnabled} trueLabel="Notifications enabled" falseLabel="Notifications blocked" /></TableCell>
      <TableCell align="center"><BoolIcon value={d.batteryOptIgnored} trueLabel="Battery optimisation ignored" falseLabel="Battery optimisation ON" /></TableCell>
      <TableCell align="center"><BoolIcon value={d.unusedRestrictionsDisabled} trueLabel="Pause-if-unused disabled" falseLabel="Pause-if-unused ON" /></TableCell>
      <TableCell><TimeText value={d.lastSeenAt} relative /></TableCell>
      <TableCell align="right">
        <AdminOnly>
          <Tooltip title="Remove device">
            <IconButton size="small" onClick={() => onRemove(d)} aria-label="Remove device">
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </AdminOnly>
      </TableCell>
    </TableRow>
  );
}

export function UserDrawer({ userId, onClose }: { userId: number | null; onClose: () => void }) {
  const snackbar = useSnackbar();
  const confirm = useConfirm();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [renameOpen, setRenameOpen] = useState(false);
  const [newName, setNewName] = useState('');

  const q = useQuery({
    queryKey: queryKeys.userDetail(userId ?? 0),
    queryFn: () => usersApi.detail(userId as number),
    enabled: userId !== null,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.userDetail(userId ?? 0) });
    queryClient.invalidateQueries({ queryKey: queryKeys.entity('users') });
    queryClient.invalidateQueries({ queryKey: queryKeys.entity('devices') });
  };

  const update = useMutation({
    mutationFn: (body: Parameters<typeof usersApi.update>[1]) => usersApi.update(userId as number, body),
    onSuccess: (_, body) => {
      snackbar.success(body.status ? `User ${body.status === 'DISABLED' ? 'disabled' : 'enabled'}` : 'User updated');
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Update failed'),
  });
  const [pwOpen, setPwOpen] = useState(false);
  const [newPw, setNewPw] = useState('');
  const setPassword = useMutation({
    mutationFn: () => usersApi.setPassword(userId as number, newPw),
    onSuccess: () => {
      snackbar.success('Password updated');
      setPwOpen(false);
      setNewPw('');
    },
    onError: (e) => snackbar.error(e, 'Could not set password'),
  });

  const forceLogout = useMutation({
    mutationFn: () => usersApi.forceLogout(userId as number),
    onSuccess: () => {
      snackbar.success('All sessions revoked');
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Force sign-out failed'),
  });
  const removeDevice = useMutation({
    mutationFn: (deviceId: number) => usersApi.removeDevice(deviceId),
    onSuccess: () => {
      snackbar.success('Device removed');
      invalidate();
    },
    onError: (e) => snackbar.error(e, 'Could not remove device'),
  });

  const u = q.data?.user;
  const disabled = u?.status === 'DISABLED';

  const toggleStatus = async () => {
    setMenuAnchor(null);
    if (!u) return;
    const ok = await confirm({
      title: disabled ? `Enable ${u.phoneE164}?` : `Disable ${u.phoneE164}?`,
      description: disabled ? 'The user will be able to sign in and receive alerts again.' : 'The user will be signed out of the app and new alerts for this number will be discarded with reason USER_DISABLED.',
      confirmLabel: disabled ? 'Enable' : 'Disable',
      danger: !disabled,
    });
    if (ok) update.mutate({ status: disabled ? 'ACTIVE' : 'DISABLED' });
  };

  const doForceLogout = async () => {
    setMenuAnchor(null);
    if (!u) return;
    if (await confirm({ title: 'Force sign-out?', description: 'Every refresh token of this user is revoked; all devices must sign in again.', confirmLabel: 'Sign out everywhere', danger: true })) {
      forceLogout.mutate();
    }
  };

  const doRemoveDevice = async (d: DeviceRow) => {
    if (await confirm({ title: 'Remove this device?', description: `${d.manufacturer ?? ''} ${d.model ?? ''} will stop receiving pushes until the app registers again.`, confirmLabel: 'Remove', danger: true })) {
      removeDevice.mutate(d.id);
    }
  };

  return (
    <DetailDrawer
      open={userId !== null}
      onClose={onClose}
      loading={q.isPending}
      error={q.error}
      onRetry={() => q.refetch()}
      title={u ? <span style={{ fontFamily: 'monospace' }}>{u.phoneE164}</span> : `User #${userId ?? ''}`}
      subtitle={u ? (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <StatusChip value={u.status} />
          <span>{u.displayName || 'No display name'}</span>
        </Box>
      ) : undefined}
      actions={
        u ? (
          <AdminOnly>
            <Button size="small" variant="outlined" endIcon={<MoreVertIcon />} onClick={(e) => setMenuAnchor(e.currentTarget)} loading={update.isPending || forceLogout.isPending}>
              Actions
            </Button>
          </AdminOnly>
        ) : undefined
      }
    >
      <Menu open={!!menuAnchor} anchorEl={menuAnchor} onClose={() => setMenuAnchor(null)}>
        <MenuItem onClick={toggleStatus}>
          {disabled ? <CheckCircleOutlineIcon fontSize="small" sx={{ mr: 1.5 }} /> : <BlockIcon fontSize="small" sx={{ mr: 1.5 }} />}
          {disabled ? 'Enable user' : 'Disable user'}
        </MenuItem>
        <MenuItem onClick={doForceLogout}>
          <LogoutIcon fontSize="small" sx={{ mr: 1.5 }} /> Force sign-out
        </MenuItem>
        <MenuItem
          onClick={() => {
            setMenuAnchor(null);
            setNewName(u?.displayName ?? '');
            setRenameOpen(true);
          }}
        >
          <EditOutlinedIcon fontSize="small" sx={{ mr: 1.5 }} /> Edit display name
        </MenuItem>
        <MenuItem
          onClick={() => {
            setMenuAnchor(null);
            setNewPw('');
            setPwOpen(true);
          }}
        >
          <KeyOutlinedIcon fontSize="small" sx={{ mr: 1.5 }} /> Set password
        </MenuItem>
      </Menu>

      <Dialog open={pwOpen} onClose={() => setPwOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Set password</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth type="password" label="New password" value={newPw} onChange={(e) => setNewPw(e.target.value)} sx={{ mt: 1 }} helperText="At least 4 characters. The user signs in with phone + this password." />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPwOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={newPw.length < 4} loading={setPassword.isPending} onClick={() => setPassword.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={renameOpen} onClose={() => setRenameOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Display name</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth value={newName} onChange={(e) => setNewName(e.target.value)} sx={{ mt: 1 }} onKeyDown={(e) => {
            if (e.key === 'Enter') {
              update.mutate({ displayName: newName.trim() || null });
              setRenameOpen(false);
            }
          }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRenameOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => { update.mutate({ displayName: newName.trim() || null }); setRenameOpen(false); }}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {u && q.data && (
        <>
          <Box sx={{ bgcolor: 'background.paper', border: '1px solid', borderColor: 'divider', borderRadius: 3, p: 2 }}>
            <MetaGrid
              items={[
                { label: 'User id', value: `#${u.id}` },
                { label: 'Status', value: <StatusChip value={u.status} /> },
                { label: 'Created', value: <TimeText value={u.createdAt} /> },
                { label: 'Last login', value: <TimeText value={u.lastLoginAt} /> },
              ]}
            />
          </Box>

          <Section title="Devices" count={q.data.devices.length}>
            {q.data.devices.length === 0 ? (
              <EmptyState compact title="No devices registered" description="Alerts for this user will be marked NO_DEVICE until the app signs in." />
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small" sx={{ minWidth: 500, '& td, & th': { px: 1 } }}>
                  <TableHead>
                    <TableRow sx={{ '& th': { color: 'text.secondary', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.05em', whiteSpace: 'nowrap' } }}>
                      <TableCell>Device</TableCell>
                      <TableCell align="center">State</TableCell>
                      <TableCell align="center" title="Notifications permission">Notif.</TableCell>
                      <TableCell align="center" title="Ignore battery optimisation">Battery</TableCell>
                      <TableCell align="center" title="Pause app if unused — disabled">Unused</TableCell>
                      <TableCell>Last seen</TableCell>
                      <TableCell />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {q.data.devices.map((d) => (
                      <DeviceRowItem key={d.id} d={d} onRemove={doRemoveDevice} />
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}
          </Section>

          <Section
            title="Recent notifications"
            count={q.data.recentNotifications.length}
            action={
              <Button size="small" onClick={() => navigate(`/notifications${filtersToSearch([{ field: 'userPhone', op: 'eq', value: u.phoneE164 }])}`)}>
                View all
              </Button>
            }
          >
            {q.data.recentNotifications.length === 0 ? (
              <EmptyState compact title="No notifications yet" />
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small" sx={{ minWidth: 480, '& td, & th': { px: 1 } }}>
                  <TableHead>
                    <TableRow sx={{ '& th': { color: 'text.secondary', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.05em', whiteSpace: 'nowrap' } }}>
                      <TableCell>Alert</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Created</TableCell>
                      <TableCell>Read</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {q.data.recentNotifications.map((n) => (
                      <TableRow key={n.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/alerts?alert=${n.alertId}`)}>
                        <TableCell sx={{ fontFamily: 'monospace' }}>
                          #{n.alertId}
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                            uid {n.uid}
                          </Typography>
                        </TableCell>
                        <TableCell><AlertTypeChip code={n.alertTypeCode} /></TableCell>
                        <TableCell><StatusChip value={n.status} /></TableCell>
                        <TableCell><TimeText value={n.createdAt} short /></TableCell>
                        <TableCell><TimeText value={n.readAt} short /></TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}
          </Section>
        </>
      )}
    </DetailDrawer>
  );
}
