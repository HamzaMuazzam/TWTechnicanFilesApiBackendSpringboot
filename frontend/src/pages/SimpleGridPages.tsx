import { useNavigate } from 'react-router';
import { Tooltip, IconButton } from '@mui/material';
import type { GridColDef, GridValidRowModel } from '@mui/x-data-grid';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { DataGridPage } from '@/components/datagrid/DataGridPage';
import { deliveriesEntity, devicesEntity, discardsEntity, notificationsEntity } from '@/entities';
import type { DeliveryRow, DeviceRow, DiscardRow, NotificationRow } from '@/api/types';
import { filtersToSearch } from '@/components/datagrid/useGridQueryState';

/** Small "open related" action column reused across the pipeline grids. */
function linkColumn<R extends GridValidRowModel>(label: string, to: (row: R) => string): GridColDef<R> {
  return {
    field: '__open',
    headerName: '',
    width: 48,
    sortable: false,
    filterable: false,
    hideable: false,
    disableColumnMenu: true,
    align: 'center',
    renderCell: (p) => (
      <OpenLink label={label} to={to(p.row)} />
    ),
  };
}

function OpenLink({ label, to }: { label: string; to: string }) {
  const navigate = useNavigate();
  return (
    <Tooltip title={label}>
      <IconButton
        size="small"
        aria-label={label}
        onClick={(e) => {
          e.stopPropagation();
          navigate(to);
        }}
      >
        <OpenInNewIcon sx={{ fontSize: 16 }} />
      </IconButton>
    </Tooltip>
  );
}

export function NotificationsPage() {
  const navigate = useNavigate();
  return (
    <DataGridPage<NotificationRow>
      entity={notificationsEntity}
      onRowClick={(row) => navigate(`/alerts?alert=${row.alertId}`)}
      extraColumns={[linkColumn<NotificationRow>('Open deliveries', (r) => `/deliveries${filtersToSearch([{ field: 'notificationId', op: 'eq', value: r.id }])}`)]}
    />
  );
}

export function DeliveriesPage() {
  const navigate = useNavigate();
  return (
    <DataGridPage<DeliveryRow>
      entity={deliveriesEntity}
      onRowClick={(row) => navigate(`/notifications${filtersToSearch([{ field: 'id', op: 'eq', value: row.notificationId }])}`)}
    />
  );
}

export function DevicesPage() {
  return (
    <DataGridPage<DeviceRow>
      entity={devicesEntity}
      extraColumns={[linkColumn<DeviceRow>('Open user', (r) => `/users?user=${r.userId}`)]}
    />
  );
}

export function DiscardedPage() {
  const navigate = useNavigate();
  return <DataGridPage<DiscardRow> entity={discardsEntity} onRowClick={(row) => navigate(`/alerts?alert=${row.alertId}`)} />;
}
