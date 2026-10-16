import { DISCARD_REASONS } from '@/api/types';
import type { EntityConfig } from './types';

export const discardsEntity: EntityConfig = {
  name: 'discards',
  title: 'Discarded recipients',
  singular: 'discarded recipient',
  path: '/discarded',
  idField: 'id',
  defaultSort: { field: 'createdAt', dir: 'desc' },
  searchPlaceholder: 'Search phone…',
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 90, render: 'mono' },
    { field: 'alertId', filterField: 'alert.id', label: 'Alert', type: 'number', width: 90, render: 'mono' },
    { field: 'uid', filterField: 'alert.uid', label: 'UID', type: 'text', width: 110, render: 'mono' },
    { field: 'alertTypeCode', filterField: 'alert.alertTypeCode', label: 'Type', type: 'enum', enumSource: 'alertTypes', width: 150, render: 'chip' },
    { field: 'rawPhone', label: 'Raw phone', type: 'text', width: 170, render: 'mono' },
    { field: 'phoneE164', label: 'Phone (E.164)', type: 'text', width: 170, render: 'mono' },
    { field: 'reason', label: 'Reason', type: 'enum', options: DISCARD_REASONS, width: 150, render: 'chip' },
    { field: 'createdAt', label: 'Created', type: 'datetime', width: 170, render: 'time' },
  ],
};
