import { NOTIFICATION_STATUSES } from '@/api/types';
import type { EntityConfig } from './types';

export const notificationsEntity: EntityConfig = {
  name: 'notifications',
  title: 'Notifications',
  singular: 'notification',
  path: '/notifications',
  idField: 'id',
  defaultSort: { field: 'createdAt', dir: 'desc' },
  searchPlaceholder: 'Search phone, uid…',
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 90, render: 'mono' },
    { field: 'alertId', filterField: 'alert.id', label: 'Alert', type: 'number', width: 90, render: 'mono' },
    { field: 'userPhone', filterField: 'user.phoneE164', label: 'Phone', type: 'text', width: 150, render: 'mono' },
    { field: 'uid', filterField: 'alert.uid', label: 'UID', type: 'text', width: 110, render: 'mono' },
    { field: 'alertTypeCode', filterField: 'alert.alertTypeCode', label: 'Type', type: 'enum', enumSource: 'alertTypes', width: 150, render: 'chip' },
    { field: 'alertMessage', filterField: 'alert.message', label: 'Message', type: 'text', flex: 1, minWidth: 220 },
    { field: 'status', label: 'Status', type: 'enum', options: NOTIFICATION_STATUSES, width: 120, render: 'chip' },
    { field: 'createdAt', label: 'Created', type: 'datetime', width: 170, render: 'time' },
    { field: 'sentAt', label: 'Sent', type: 'datetime', width: 170, render: 'time' },
    { field: 'deliveredAt', label: 'Delivered', type: 'datetime', width: 170, render: 'time' },
    { field: 'readAt', label: 'Read', type: 'datetime', width: 170, render: 'time' },
  ],
};
