import { SEVERITIES } from '@/api/types';
import type { EntityConfig } from './types';

export const alertsEntity: EntityConfig = {
  name: 'alerts',
  title: 'Alerts',
  singular: 'alert',
  path: '/alerts',
  idField: 'id',
  defaultSort: { field: 'sdt', dir: 'desc' },
  searchPlaceholder: 'Search uid, message…',
  hasDetail: true,
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 90, render: 'mono' },
    { field: 'sdt', label: 'Received by server', type: 'datetime', width: 170, render: 'time', hint: 'SDT — when our server received the alert' },
    { field: 'dbdt', label: 'Device time (S3)', type: 'datetime', width: 170, render: 'time', hint: 'DBDT — timestamp sent by S3' },
    { field: 'uid', label: 'UID', type: 'text', width: 120, render: 'mono' },
    { field: 'alertTypeCode', label: 'Type', type: 'enum', enumSource: 'alertTypes', width: 150, render: 'chip' },
    { field: 'severity', filterField: 'alertType.severity', label: 'Severity', type: 'enum', options: SEVERITIES, width: 110, render: 'chip' },
    { field: 'message', label: 'Message', type: 'text', flex: 1, minWidth: 240 },
    { field: 'recipientsTotal', label: 'Recipients', type: 'number', width: 100, render: 'number' },
    { field: 'recipientsAccepted', label: 'Accepted', type: 'number', width: 100, render: 'number' },
    { field: 'recipientsDiscarded', label: 'Discarded', type: 'number', width: 100, render: 'number' },
    { field: 'sourceName', filterField: 'source.name', label: 'Source', type: 'text', width: 110, hidden: true },
    { field: 'sourceAlertId', label: 'Source alert id', type: 'text', width: 140, render: 'mono', hidden: true },
    { field: 'lat', label: 'Lat', type: 'number', width: 100, render: 'coord', hidden: true },
    { field: 'lon', label: 'Lon', type: 'number', width: 100, render: 'coord', hidden: true },
  ],
};
