import { SEVERITIES } from '@/api/types';
import type { EntityConfig } from './types';

export const alertTypesEntity: EntityConfig = {
  name: 'alert-types',
  title: 'Alert types',
  singular: 'alert type',
  path: '/alert-types',
  idField: 'code',
  defaultSort: { field: 'sortOrder', dir: 'asc' },
  searchPlaceholder: 'Search code, label…',
  fields: [
    { field: 'code', label: 'Code', type: 'text', width: 160, render: 'mono' },
    { field: 'label', label: 'Label', type: 'text', flex: 1, minWidth: 160 },
    { field: 'severity', label: 'Severity', type: 'enum', options: SEVERITIES, width: 120, render: 'chip' },
    { field: 'icon', label: 'Icon', type: 'text', width: 130 },
    { field: 'color', label: 'Color', type: 'text', width: 120, render: 'color' },
    { field: 'channelId', label: 'Channel id', type: 'text', width: 150, render: 'mono' },
    { field: 'active', label: 'Active', type: 'boolean', width: 90, render: 'bool' },
    { field: 'sortOrder', label: 'Order', type: 'number', width: 80, render: 'number' },
  ],
};
