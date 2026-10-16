import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, Autocomplete, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField, Typography } from '@mui/material';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import { DataGridPage } from '@/components/datagrid/DataGridPage';
import { alertsEntity } from '@/entities';
import type { AlertRow } from '@/api/types';
import { alertsApi, type SendAlertInput, type SendAlertResult } from '@/api/endpoints';
import { queryKeys, useAlertTypes } from '@/api/hooks';
import { AdminOnly } from '@/components/AdminOnly';
import { useSnackbar } from '@/components/SnackbarProvider';
import { AlertDrawer } from './AlertDrawer';

const EMPTY: SendAlertInput = { uid: '', alertType: '', message: '', lat: 24.8607, lon: 67.0011 };

/** The open alert id lives in the URL (`?alert=123`) so the drawer survives reloads and can be linked. */
export function AlertsPage() {
  const [params, setParams] = useSearchParams();
  const raw = params.get('alert');
  const alertId = raw && /^\d+$/.test(raw) ? Number(raw) : null;
  const snackbar = useSnackbar();
  const queryClient = useQueryClient();
  const types = useAlertTypes();
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<SendAlertInput>(EMPTY);
  const [last, setLast] = useState<SendAlertResult | null>(null);

  const setAlert = (id: number | null) =>
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (id === null) next.delete('alert');
        else next.set('alert', String(id));
        return next;
      },
      { replace: true },
    );

  const send = useMutation({
    mutationFn: () =>
      alertsApi.send({
        uid: draft.uid.trim(),
        alertType: draft.alertType.trim().toUpperCase(),
        message: draft.message?.trim() || undefined,
        lat: draft.lat === null || Number.isNaN(draft.lat) ? undefined : draft.lat,
        lon: draft.lon === null || Number.isNaN(draft.lon) ? undefined : draft.lon,
      }),
    onSuccess: (res) => {
      setLast(res);
      queryClient.invalidateQueries({ queryKey: queryKeys.entity('alerts') });
      queryClient.invalidateQueries({ queryKey: queryKeys.alertTypes });
      if (res.result === 'NO_CONFIGURATION') snackbar.error(`No configuration for uid ${draft.uid} / ${draft.alertType}`, 'Nobody notified');
      else snackbar.success(`Alert #${res.alertId}: ${res.notified} notified, ${res.discarded} discarded`);
    },
    onError: (e) => snackbar.error(e, 'Send failed'),
  });

  const valid = draft.uid.trim().length > 0 && draft.alertType.trim().length > 0;
  const num = (v: string) => (v.trim() === '' ? null : Number(v));

  return (
    <>
      <DataGridPage<AlertRow>
        entity={alertsEntity}
        onRowClick={(row) => setAlert(row.id)}
        selectedId={alertId}
        headerActions={
          <AdminOnly>
            <Button variant="contained" startIcon={<SendOutlinedIcon />} onClick={() => { setLast(null); setOpen(true); }}>
              Send alert
            </Button>
          </AdminOnly>
        }
      >
        <AlertDrawer alertId={alertId} onClose={() => setAlert(null)} />
      </DataGridPage>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Send alert</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Goes through the same pipeline as S3: recipients are looked up from the configurations for this UID and alert type, then pushed to their phones.
          </Typography>
          <Stack spacing={2}>
            <TextField label="Tracker UID" value={draft.uid} onChange={(e) => setDraft({ ...draft, uid: e.target.value })} autoFocus fullWidth required />
            <Autocomplete
              freeSolo
              options={(types.data ?? []).map((t) => t.code)}
              getOptionLabel={(o) => o}
              inputValue={draft.alertType}
              onInputChange={(_, v) => setDraft({ ...draft, alertType: v })}
              renderInput={(p) => <TextField {...p} label="Alert type" placeholder="OVERSPEED" required helperText="Pick an existing type or type a new code" />}
            />
            <TextField label="Message" value={draft.message ?? ''} onChange={(e) => setDraft({ ...draft, message: e.target.value })} fullWidth multiline minRows={2} />
            <Stack direction="row" spacing={2}>
              <TextField label="Latitude" type="number" value={draft.lat ?? ''} onChange={(e) => setDraft({ ...draft, lat: num(e.target.value) })} fullWidth slotProps={{ htmlInput: { step: 'any' } }} />
              <TextField label="Longitude" type="number" value={draft.lon ?? ''} onChange={(e) => setDraft({ ...draft, lon: num(e.target.value) })} fullWidth slotProps={{ htmlInput: { step: 'any' } }} />
            </Stack>
            {last && (
              <Alert severity={last.result === 'ACCEPTED' && last.notified > 0 ? 'success' : 'warning'} onClose={() => setLast(null)}>
                <b>{last.result}</b> — alert #{last.alertId}: {last.configuredPhones} configured phone{last.configuredPhones === 1 ? '' : 's'}, {last.notified} notified, {last.discarded} discarded
                {last.discardedPhones.length > 0 && (
                  <span> ({last.discardedPhones.map((d) => `${d.phone}: ${d.reason}`).join(', ')})</span>
                )}
                {' · '}
                <Button size="small" onClick={() => { setOpen(false); setAlert(last.alertId); }}>Open</Button>
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Close</Button>
          <Button variant="contained" disabled={!valid} loading={send.isPending} onClick={() => send.mutate()}>
            Send
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
