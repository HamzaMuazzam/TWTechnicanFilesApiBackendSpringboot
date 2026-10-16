import { Fragment, useState } from 'react';
import { Link as RouterLink } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Button,
  Collapse,
  IconButton,
  Link,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ReplayIcon from '@mui/icons-material/Replay';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { alertsApi } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import type { AlertRecipient, RecipientDelivery } from '@/api/types';
import { DetailDrawer, MetaGrid, Section } from '@/components/DetailDrawer';
import { StatusChip } from '@/components/StatusChip';
import { TimeText } from '@/components/TimeText';
import { EmptyState } from '@/components/EmptyState';
import { AdminOnly } from '@/components/AdminOnly';
import { AlertTypeChip } from '@/components/datagrid/cellRenderers';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useConfirm } from '@/components/ConfirmDialog';
import { useAuth } from '@/auth/AuthContext';
import { fmtCoord, fmtDateTime } from '@/utils/format';
import { severityPalette } from '@/theme/theme';
import { filtersToSearch } from '@/components/datagrid/useGridQueryState';

const DELIVERY_STEPS = ['QUEUED', 'SENT', 'DELIVERED'] as const;

function DeliveryTimeline({ d }: { d: RecipientDelivery }) {
  const theme = useTheme();
  const mode = theme.palette.mode === 'dark' ? 'dark' : 'light';
  const sev = severityPalette[mode];
  const failed = d.status === 'FAILED' || d.status === 'INVALID_TOKEN';
  const reached = (step: (typeof DELIVERY_STEPS)[number]) => {
    const order: Record<string, number> = { QUEUED: 0, SENT: 1, DELIVERED: 2 };
    const current = failed ? (d.sentAt ? 1 : 0) : (order[d.status] ?? 0);
    return order[step] <= current;
  };
  const stepTime = (step: (typeof DELIVERY_STEPS)[number]) => (step === 'QUEUED' ? d.queuedAt : step === 'SENT' ? d.sentAt : d.deliveredAt);
  return (
    <Box sx={{ px: 2, py: 1.5, bgcolor: 'background.default', borderTop: '1px solid', borderColor: 'divider' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.25, flexWrap: 'wrap' }}>
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {`${d.manufacturer ?? ''} ${d.model ?? ''}`.trim() || `Device #${d.deviceId}`}{d.deviceActive ? '' : ' · inactive'}
        </Typography>
        <StatusChip value={d.status} />
        <Typography variant="caption" color="text.secondary">
          {d.attempts} attempt{d.attempts === 1 ? '' : 's'}
        </Typography>
        <Box sx={{ flex: 1 }} />
        {d.fcmMessageId && (
          <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }} title="FCM message id">
            {d.fcmMessageId}
          </Typography>
        )}
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'stretch' }}>
        {DELIVERY_STEPS.map((step, i) => {
          const ok = reached(step);
          const isFailHere = failed && ((step === 'SENT' && !d.sentAt) || (step === 'DELIVERED' && d.sentAt));
          const color = isFailHere ? sev.CRITICAL.main : ok ? sev.SUCCESS.main : theme.palette.divider;
          return (
            <Fragment key={step}>
              <Box sx={{ flex: '0 0 auto', textAlign: 'center', minWidth: 110 }}>
                <Box sx={{ width: 10, height: 10, borderRadius: 5, bgcolor: color, mx: 'auto', outline: `3px solid ${theme.palette.background.default}` }} />
                <Typography variant="caption" sx={{ display: 'block', mt: 0.5, fontWeight: ok ? 500 : 400, color: isFailHere ? sev.CRITICAL.main : ok ? 'text.primary' : 'text.disabled' }}>
                  {isFailHere ? d.status : step}
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', fontVariantNumeric: 'tabular-nums' }}>
                  {stepTime(step) ? fmtDateTime(stepTime(step)) : '—'}
                </Typography>
              </Box>
              {i < DELIVERY_STEPS.length - 1 && <Box sx={{ flex: 1, height: 2, bgcolor: reached(DELIVERY_STEPS[i + 1]) ? sev.SUCCESS.main : 'divider', mt: '4px' }} />}
            </Fragment>
          );
        })}
      </Box>
      {(d.lastError || d.nextAttemptAt) && (
        <Box sx={{ mt: 1.25, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          {d.lastError && (
            <Typography variant="caption" sx={{ color: sev.CRITICAL.main, fontFamily: 'monospace', wordBreak: 'break-all' }}>
              {d.lastError}
            </Typography>
          )}
          {d.nextAttemptAt && (
            <Typography variant="caption" color="text.secondary">
              Next attempt: <TimeText value={d.nextAttemptAt} variant="caption" />
            </Typography>
          )}
        </Box>
      )}
    </Box>
  );
}

function RecipientRow({ r, onResend, resending }: { r: AlertRecipient; onResend: (id: number) => void; resending: boolean }) {
  const [open, setOpen] = useState(false);
  const { isAdmin } = useAuth();
  const canResend = isAdmin && r.status !== 'READ';
  return (
    <>
      <TableRow hover sx={{ '& td': { borderBottom: open ? 'none' : undefined }, cursor: 'pointer' }} onClick={() => setOpen((o) => !o)}>
        <TableCell padding="checkbox">
          <IconButton size="small" aria-label={open ? 'Collapse' : 'Expand'} sx={{ transform: open ? 'rotate(180deg)' : 'none', transition: 'transform .15s' }}>
            <ExpandMoreIcon fontSize="small" />
          </IconButton>
        </TableCell>
        <TableCell sx={{ fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
          <Link component={RouterLink} to={`/users${filtersToSearch([{ field: 'phoneE164', op: 'eq', value: r.phone }])}`} onClick={(e) => e.stopPropagation()} underline="hover" color="inherit">
            {r.phone}
          </Link>
          {r.displayName && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', fontFamily: 'Roboto' }}>
              {r.displayName}
            </Typography>
          )}
        </TableCell>
        <TableCell>
          <StatusChip value={r.status} />
        </TableCell>
        <TableCell><TimeText value={r.sentAt} short /></TableCell>
        <TableCell><TimeText value={r.deliveredAt} short /></TableCell>
        <TableCell><TimeText value={r.readAt} short /></TableCell>
        <TableCell align="right" onClick={(e) => e.stopPropagation()}>
          <AdminOnly>
            <Tooltip title={r.status === 'READ' ? 'Already read' : 'Requeue deliveries for this recipient'}>
              <span>
                <IconButton size="small" disabled={!canResend || resending} onClick={() => onResend(r.notificationId)} aria-label="Resend">
                  <ReplayIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
          </AdminOnly>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell colSpan={7} sx={{ p: 0, borderBottom: open ? undefined : 'none' }}>
          <Collapse in={open} unmountOnExit>
            {r.deliveries.length === 0 ? (
              <Box sx={{ px: 2, py: 1.5, bgcolor: 'background.default', borderTop: '1px solid', borderColor: 'divider' }}>
                <Typography variant="body2" color="text.secondary">
                  No deliveries — the user had no active device with a token when the alert arrived.
                </Typography>
              </Box>
            ) : (
              r.deliveries.map((d) => <DeliveryTimeline key={d.id} d={d} />)
            )}
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

export function AlertDrawer({ alertId, onClose }: { alertId: number | null; onClose: () => void }) {
  const snackbar = useSnackbar();
  const confirm = useConfirm();
  const queryClient = useQueryClient();
  const q = useQuery({
    queryKey: queryKeys.alertDetail(alertId ?? 0),
    queryFn: () => alertsApi.detail(alertId as number),
    enabled: alertId !== null,
  });

  const resend = useMutation({
    mutationFn: (notificationId: number) => alertsApi.resend(notificationId),
    onSuccess: () => {
      snackbar.success('Deliveries requeued');
      queryClient.invalidateQueries({ queryKey: queryKeys.alertDetail(alertId ?? 0) });
      queryClient.invalidateQueries({ queryKey: queryKeys.entity('notifications') });
      queryClient.invalidateQueries({ queryKey: queryKeys.entity('deliveries') });
    },
    onError: (e) => snackbar.error(e, 'Resend failed'),
  });

  const resendAll = async () => {
    const targets = (q.data?.recipients ?? []).filter((r) => r.status !== 'READ');
    if (targets.length === 0) return;
    if (!(await confirm({ title: `Resend to ${targets.length} recipient${targets.length === 1 ? '' : 's'}?`, description: 'Deliveries for every unread recipient will be queued again.', confirmLabel: 'Resend' }))) return;
    for (const r of targets) {
      // sequential to keep the backend happy and errors attributable
      await resend.mutateAsync(r.notificationId).catch(() => undefined);
    }
  };

  const a = q.data?.alert;
  const hasCoords = a && a.lat !== null && a.lat !== undefined && a.lon !== null && a.lon !== undefined;

  return (
    <DetailDrawer
      open={alertId !== null}
      onClose={onClose}
      loading={q.isPending}
      error={q.error}
      onRetry={() => q.refetch()}
      title={
        a ? (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
            <AlertTypeChip code={a.alertTypeCode} />
            <span>Alert #{a.id}</span>
            <Typography component="span" variant="body2" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
              uid {a.uid}
            </Typography>
          </Box>
        ) : (
          `Alert #${alertId ?? ''}`
        )
      }
      subtitle={a?.message}
      actions={
        q.data && q.data.recipients.some((r) => r.status !== 'READ') ? (
          <AdminOnly>
            <Button size="small" variant="outlined" startIcon={<ReplayIcon />} onClick={resendAll} loading={resend.isPending}>
              Resend all
            </Button>
          </AdminOnly>
        ) : undefined
      }
    >
      {a && q.data && (
        <>
          <Box sx={{ bgcolor: 'background.paper', border: '1px solid', borderColor: 'divider', borderRadius: 3, p: 2 }}>
            <MetaGrid
              items={[
                { label: 'Device time (S3)', hint: 'dbdt — timestamp sent by the tracking server', value: <TimeText value={a.dbdt} /> },
                { label: 'Received by server', hint: 'sdt — when this backend accepted the alert', value: <TimeText value={a.sdt} /> },
                { label: 'Severity', value: a.severity ? <StatusChip value={a.severity} /> : '—' },
                { label: 'Source', value: a.sourceName ?? (a.sourceId ? `#${a.sourceId}` : '—') },
                { label: 'Source alert id', value: a.sourceAlertId ? <span style={{ fontFamily: 'monospace' }}>{a.sourceAlertId}</span> : '—' },
                {
                  label: 'Location',
                  value: hasCoords ? (
                    <Link href={`https://www.openstreetmap.org/?mlat=${a.lat}&mlon=${a.lon}#map=16/${a.lat}/${a.lon}`} target="_blank" rel="noopener noreferrer" underline="hover" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, fontVariantNumeric: 'tabular-nums' }}>
                      {fmtCoord(a.lat)}, {fmtCoord(a.lon)} <OpenInNewIcon sx={{ fontSize: 14 }} />
                    </Link>
                  ) : (
                    'No location'
                  ),
                },
                {
                  label: 'Recipients',
                  value: `${a.recipientsTotal ?? q.data.recipients.length} total · ${a.recipientsAccepted ?? q.data.recipients.length} accepted · ${a.recipientsDiscarded ?? q.data.discards.length} discarded`,
                  full: true,
                },
                ...(a.extra && Object.keys(a.extra).length
                  ? [{ label: 'Extra', full: true, value: <Box component="pre" sx={{ m: 0, fontSize: 12, fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{JSON.stringify(a.extra, null, 2)}</Box> }]
                  : []),
              ]}
            />
          </Box>

          <Section title="Recipients" count={q.data.recipients.length}>
            {q.data.recipients.length === 0 ? (
              <EmptyState compact title="No accepted recipients" />
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small" sx={{ minWidth: 500, '& td, & th': { px: 1 } }}>
                  <TableHead>
                    <TableRow sx={{ '& th': { color: 'text.secondary', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.05em', whiteSpace: 'nowrap' } }}>
                      <TableCell padding="checkbox" />
                      <TableCell>Phone</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Sent</TableCell>
                      <TableCell>Delivered</TableCell>
                      <TableCell>Read</TableCell>
                      <TableCell />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {q.data.recipients.map((r) => (
                      <RecipientRow key={r.notificationId} r={r} onResend={(id) => resend.mutate(id)} resending={resend.isPending} />
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}
          </Section>

          <Section title="Discarded recipients" count={q.data.discards.length}>
            {q.data.discards.length === 0 ? (
              <EmptyState compact title="Nothing discarded" />
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ '& th': { color: 'text.secondary', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.05em' } }}>
                    <TableCell>Raw phone</TableCell>
                    <TableCell>E.164</TableCell>
                    <TableCell>Reason</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {q.data.discards.map((d) => (
                    <TableRow key={d.id}>
                      <TableCell sx={{ fontFamily: 'monospace' }}>{d.rawPhone ?? '—'}</TableCell>
                      <TableCell sx={{ fontFamily: 'monospace' }}>{d.phoneE164 ?? '—'}</TableCell>
                      <TableCell>
                        <StatusChip value={d.reason} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </Section>
        </>
      )}
    </DetailDrawer>
  );
}
