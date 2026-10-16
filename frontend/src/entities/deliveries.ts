import { DELIVERY_STATUSES } from '@/api/types';
import type { EntityConfig } from './types';

export const deliveriesEntity: EntityConfig = {
  name: 'deliveries',
  title: 'Deliveries',
  singular: 'delivery',
  path: '/deliveries',
  idField: 'id',
  defaultSort: { field: 'id', dir: 'desc' },
  searchPlaceholder: 'Search FCM message id, error…',
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 90, render: 'mono' },
    { field: 'notificationId', filterField: 'notification.id', label: 'Notification', type: 'number', width: 110, render: 'mono' },
    { field: 'userPhone', filterField: 'notification.user.phoneE164', label: 'Phone', type: 'text', width: 150, render: 'mono' },
    { field: 'uid', filterField: 'notification.alert.uid', label: 'UID', type: 'text', width: 110, render: 'mono' },
    { field: 'deviceManufacturer', filterField: 'device.manufacturer', label: 'Manufacturer', type: 'text', width: 130 },
    { field: 'deviceModel', filterField: 'device.model', label: 'Model', type: 'text', width: 150 },
    { field: 'status', label: 'Status', type: 'enum', options: DELIVERY_STATUSES, width: 130, render: 'chip' },
    { field: 'attempts', label: 'Attempts', type: 'number', width: 90, render: 'number' },
    { field: 'queuedAt', label: 'Queued', type: 'datetime', width: 170, render: 'time' },
    { field: 'sentAt', label: 'Sent', type: 'datetime', width: 170, render: 'time' },
    { field: 'deliveredAt', label: 'Delivered', type: 'datetime', width: 170, render: 'time' },
    { field: 'nextAttemptAt', label: 'Next attempt', type: 'datetime', width: 170, render: 'time' },
    { field: 'fcmMessageId', label: 'FCM message id', type: 'text', width: 200, render: 'mono', hidden: true },
    { field: 'lastError', label: 'Last error', type: 'text', flex: 1, minWidth: 200 },
  ],
};
