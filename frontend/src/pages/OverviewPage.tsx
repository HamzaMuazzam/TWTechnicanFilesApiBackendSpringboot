import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { Box, Chip, Divider, Paper, Skeleton, Stack, TextField, ToggleButton, ToggleButtonGroup, Tooltip, Typography, useTheme } from '@mui/material';
import { BarChart, LineChart } from '@mui/x-charts';
import { overviewApi } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import { KpiTile } from '@/components/KpiTile';
import { PageHeader } from '@/components/PageHeader';
import { ErrorState } from '@/components/ErrorState';
import { EmptyState } from '@/components/EmptyState';
import { severityPalette } from '@/theme/theme';
import { dayjs, fmtNumber, fmtPct, fmtRelative, titleCase } from '@/utils/format';
import { filtersToSearch } from '@/components/datagrid/useGridQueryState';
import { DELIVERY_STATUSES, NOTIFICATION_STATUSES, type FilterClause } from '@/api/types';

type Preset = 'today' | '7d' | '30d' | 'custom';

function rangeFor(preset: Preset, custom: { from: string; to: string }): { from: string; to: string } {
  const end = dayjs().endOf('day');
  switch (preset) {
    case 'today':
      return { from: dayjs().startOf('day').toISOString(), to: end.toISOString() };
    case '7d':
      return { from: dayjs().subtract(6, 'day').startOf('day').toISOString(), to: end.toISOString() };
    case '30d':
      return { from: dayjs().subtract(29, 'day').startOf('day').toISOString(), to: end.toISOString() };
    case 'custom':
      return {
        from: dayjs(custom.from).startOf('day').toISOString(),
        to: dayjs(custom.to).endOf('day').toISOString(),
      };
  }
}

function StatusBreakdown({ title, counts, order, to }: { title: string; counts: Partial<Record<string, number>>; order: readonly string[]; to: (status: string) => string }) {
  const navigate = useNavigate();
  const total = order.reduce((acc, k) => acc + (counts[k] ?? 0), 0);
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 0.4 }}>
        {title} · {fmtNumber(total)}
      </Typography>
      <Stack direction="row" flexWrap="wrap" gap={0.75} sx={{ mt: 0.75 }}>
        {order.map((k) => (
          <Chip
            key={k}
            size="small"
            variant="outlined"
            clickable
            onClick={() => navigate(to(k))}
            label={
              <span>
                {titleCase(k)} <b style={{ fontVariantNumeric: 'tabular-nums' }}>{fmtNumber(counts[k] ?? 0)}</b>
              </span>
            }
            sx={{ opacity: counts[k] ? 1 : 0.55 }}
          />
        ))}
      </Stack>
    </Box>
  );
}

function DispatchQueueCard() {
  const q = useQuery({
    queryKey: queryKeys.dispatch,
    queryFn: overviewApi.dispatchStatus,
    refetchInterval: 15_000,
    refetchIntervalInBackground: false,
  });
  const d = q.data;
  return (
    <Paper sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="subtitle1">Dispatch</Typography>
        <Tooltip title={q.dataUpdatedAt ? `Updated ${fmtRelative(new Date(q.dataUpdatedAt).toISOString())}` : 'Loading'}>
          <Chip size="small" label={q.isFetching ? 'refreshing' : 'live · 15s'} variant="outlined" sx={{ fontSize: 11 }} />
        </Tooltip>
      </Box>
      {q.isError && <ErrorState error={q.error} compact onRetry={() => q.refetch()} />}
      {q.isPending && <Skeleton height={120} />}
      {d && (
        <Stack spacing={1.5} divider={<Divider />}>
          <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
            <Box>
              <Typography variant="h5" sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 500 }}>{fmtNumber(d.dueNow)}</Typography>
              <Typography variant="caption" color="text.secondary">Due now</Typography>
            </Box>
            <Box>
              <Typography variant="h5" sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 500 }}>{fmtNumber(d.inFlight)}</Typography>
              <Typography variant="caption" color="text.secondary">In flight</Typography>
            </Box>
            <Box sx={{ ml: 'auto', alignSelf: 'center' }}>
              <Chip
                size="small"
                color={d.firebaseEnabled ? 'success' : 'warning'}
                variant={d.firebaseEnabled ? 'filled' : 'outlined'}
                label={d.firebaseEnabled ? 'Firebase on' : 'Firebase off · dev sender'}
              />
            </Box>
          </Box>
          <StatusBreakdown
            title="Deliveries"
            counts={d.deliveriesByStatus}
            order={DELIVERY_STATUSES}
            to={(s) => `/deliveries${filtersToSearch([{ field: 'status', op: 'eq', value: s }])}`}
          />
          <StatusBreakdown
            title="Notifications"
            counts={d.notificationsByStatus}
            order={NOTIFICATION_STATUSES}
            to={(s) => `/notifications${filtersToSearch([{ field: 'status', op: 'eq', value: s }])}`}
          />
        </Stack>
      )}
    </Paper>
  );
}

export function OverviewPage() {
  const theme = useTheme();
  const navigate = useNavigate();
  const mode = theme.palette.mode === 'dark' ? 'dark' : 'light';
  const sev = severityPalette[mode];

  const [preset, setPreset] = useState<Preset>('7d');
  const [custom, setCustom] = useState({ from: dayjs().subtract(6, 'day').format('YYYY-MM-DD'), to: dayjs().format('YYYY-MM-DD') });
  const range = useMemo(() => rangeFor(preset, custom), [preset, custom]);

  const q = useQuery({
    queryKey: queryKeys.overview(range.from, range.to),
    queryFn: () => overviewApi.get(range.from, range.to),
    staleTime: 30_000,
  });
  const d = q.data;

  const perDay = d?.perDay ?? [];
  const days = perDay.map((p) => dayjs(p.date).format('MMM D'));
  const byType = [...(d?.byType ?? [])].sort((a, b) => b.count - a.count).slice(0, 12);

  const goAlerts = (filters: FilterClause[]) => navigate(`/alerts${filtersToSearch(filters)}`);
  const goNotifications = (filters: FilterClause[]) => navigate(`/notifications${filtersToSearch(filters)}`);
  const rangeFilter = (field: string): FilterClause => ({ field, op: 'between', value: [range.from, range.to] });

  return (
    <Box>
      <PageHeader
        title="Overview"
        subtitle={`${dayjs(range.from).format('MMM D, YYYY')} – ${dayjs(range.to).format('MMM D, YYYY')}`}
        actions={
          <>
            <ToggleButtonGroup
              size="small"
              exclusive
              value={preset}
              onChange={(_, v: Preset | null) => v && setPreset(v)}
              sx={{ '& .MuiToggleButton-root': { px: 1.5, textTransform: 'none' } }}
            >
              <ToggleButton value="today">Today</ToggleButton>
              <ToggleButton value="7d">7 days</ToggleButton>
              <ToggleButton value="30d">30 days</ToggleButton>
              <ToggleButton value="custom">Custom</ToggleButton>
            </ToggleButtonGroup>
            {preset === 'custom' && (
              <>
                <TextField type="date" size="small" value={custom.from} onChange={(e) => setCustom((c) => ({ ...c, from: e.target.value }))} slotProps={{ htmlInput: { max: custom.to } }} />
                <TextField type="date" size="small" value={custom.to} onChange={(e) => setCustom((c) => ({ ...c, to: e.target.value }))} slotProps={{ htmlInput: { min: custom.from } }} />
              </>
            )}
          </>
        }
      />

      {q.isError && <ErrorState error={q.error} onRetry={() => q.refetch()} />}

      {/* KPI tiles */}
      <Box sx={{ display: 'grid', gap: 1.5, gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', lg: 'repeat(5, 1fr)' } }}>
        <KpiTile label="Alerts" value={fmtNumber(d?.alertsTotal)} hint={`${fmtNumber(d?.notificationsTotal)} notifications`} loading={q.isPending} onClick={() => goAlerts([rangeFilter('sdt')])} />
        <KpiTile label="Delivered rate" value={fmtPct(d?.deliveryRatePct)} hint={`${fmtNumber(d?.delivered)} delivered`} tone="SUCCESS" loading={q.isPending} onClick={() => goNotifications([{ field: 'status', op: 'eq', value: 'DELIVERED' }, rangeFilter('createdAt')])} />
        <KpiTile label="Read rate" value={fmtPct(d?.readRatePct)} hint={`${fmtNumber(d?.read)} read`} tone="SUCCESS" loading={q.isPending} onClick={() => goNotifications([{ field: 'status', op: 'eq', value: 'READ' }, rangeFilter('createdAt')])} />
        <KpiTile label="Failed" value={fmtNumber(d?.failed)} tone={d?.failed ? 'CRITICAL' : undefined} loading={q.isPending} onClick={() => goNotifications([{ field: 'status', op: 'eq', value: 'FAILED' }, rangeFilter('createdAt')])} />
        <KpiTile label="No device" value={fmtNumber(d?.noDevice)} tone={d?.noDevice ? 'WARNING' : undefined} loading={q.isPending} onClick={() => goNotifications([{ field: 'status', op: 'eq', value: 'NO_DEVICE' }, rangeFilter('createdAt')])} />
        <KpiTile label="Discarded recipients" value={fmtNumber(d?.discarded)} tone={d?.discarded ? 'WARNING' : undefined} loading={q.isPending} onClick={() => navigate(`/discarded${filtersToSearch([rangeFilter('createdAt')])}`)} />
        <KpiTile label="Active devices" value={fmtNumber(d?.devicesActive)} loading={q.isPending} onClick={() => navigate(`/devices${filtersToSearch([{ field: 'active', op: 'eq', value: true }])}`)} />
        <KpiTile
          label="Silent devices"
          value={fmtNumber(d?.devicesSilent)}
          hint="not seen for 3+ days"
          tone={d?.devicesSilent ? 'WARNING' : undefined}
          loading={q.isPending}
          onClick={() => navigate(`/devices${filtersToSearch([{ field: 'active', op: 'eq', value: true }, { field: 'lastSeenAt', op: 'lt', value: dayjs().subtract(3, 'day').toISOString() }])}`)}
        />
        <KpiTile label="Users" value={fmtNumber(d?.usersTotal)} loading={q.isPending} onClick={() => navigate('/users')} />
        <KpiTile label="New users" value={fmtNumber(d?.usersNew)} hint="in range" loading={q.isPending} onClick={() => navigate(`/users${filtersToSearch([rangeFilter('createdAt')])}`)} />
      </Box>

      {/* Charts */}
      <Box sx={{ display: 'grid', gap: 1.5, mt: 1.5, gridTemplateColumns: { xs: '1fr', lg: '2fr 1fr' } }}>
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle1" sx={{ mb: 1 }}>
            Per day
          </Typography>
          {q.isPending ? (
            <Skeleton variant="rounded" height={280} />
          ) : perDay.length === 0 ? (
            <EmptyState compact title="No data in this range" />
          ) : (
            <LineChart
              height={300}
              xAxis={[{ scaleType: 'point', data: days }]}
              series={[
                { data: perDay.map((p) => p.alerts), label: 'Alerts', color: theme.palette.text.secondary, showMark: false, curve: 'monotoneX' },
                { data: perDay.map((p) => p.delivered), label: 'Delivered', color: sev.SUCCESS.main, showMark: false, curve: 'monotoneX' },
                { data: perDay.map((p) => p.read), label: 'Read', color: sev.INFO.main, showMark: false, curve: 'monotoneX' },
                { data: perDay.map((p) => p.failed), label: 'Failed', color: sev.CRITICAL.main, showMark: false, curve: 'monotoneX' },
              ]}
              grid={{ horizontal: true }}
              margin={{ left: 8, right: 16, top: 8, bottom: 8 }}
              slotProps={{ legend: { position: { vertical: 'bottom', horizontal: 'center' } } }}
            />
          )}
        </Paper>
        <DispatchQueueCard />
      </Box>

      <Paper sx={{ p: 2, mt: 1.5 }}>
        <Typography variant="subtitle1" sx={{ mb: 1 }}>
          Alerts by type
        </Typography>
        {q.isPending ? (
          <Skeleton variant="rounded" height={240} />
        ) : byType.length === 0 ? (
          <EmptyState compact title="No alerts in this range" />
        ) : (
          <BarChart
            height={Math.max(220, byType.length * 30 + 60)}
            layout="horizontal"
            yAxis={[{ scaleType: 'band', data: byType.map((t) => t.label || t.code), width: 150 }]}
            xAxis={[{ label: 'Alerts' }]}
            series={[{ data: byType.map((t) => t.count), label: 'Alerts', color: theme.palette.primary.main }]}
            grid={{ vertical: true }}
            margin={{ left: 8, right: 24, top: 8, bottom: 8 }}
            hideLegend
            onItemClick={(_, item) => {
              const t = byType[item.dataIndex];
              if (t) goAlerts([{ field: 'alertTypeCode', op: 'eq', value: t.code }, rangeFilter('sdt')]);
            }}
          />
        )}
      </Paper>
    </Box>
  );
}
